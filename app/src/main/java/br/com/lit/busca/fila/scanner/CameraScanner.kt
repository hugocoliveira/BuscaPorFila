package br.com.lit.busca.fila.scanner

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// ---------------------------------------------------------------------------
// Integração CameraX + ML Kit para leitura de QR Code.
// A câmera é iniciada/encerrada pelo ciclo de vida do Composable chamador.
// ---------------------------------------------------------------------------

private const val TAG = "CameraScanner"

/**
 * Configura e inicia a câmera traseira com análise de imagem em tempo real
 * usando ML Kit Barcode Scanning.
 *
 * @param context        contexto Android (preferencialmente Activity).
 * @param lifecycleOwner dono do ciclo de vida que controla a câmera.
 * @param previewView    View onde o preview da câmera será renderizado.
 * @param onCodigoLido   callback chamado com o valor do código detectado.
 * @return [ExecutorService] — o chamador deve chamar shutdown() ao encerrar.
 */
fun iniciarScanner(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onCodigoLido: (String) -> Unit
): ExecutorService {
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        // Caso de uso de preview — exibe o feed da câmera no PreviewView
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        // Caso de uso de análise — processa cada frame para detecção de QR
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor, BarcodeAnalyzer { codigo ->
                    previewView.post { onCodigoLido(codigo) }
                })
            }

        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalyzer
            )
        }.onFailure { e ->
            Log.e(TAG, "Falha ao iniciar câmera: ${e.message}", e)
        }

    }, ContextCompat.getMainExecutor(context))

    return cameraExecutor
}

/**
 * Analisador de frames que usa ML Kit para detectar QR Codes.
 *
 * @param onCodigoDetectado callback chamado com o valor bruto do primeiro código encontrado.
 */
private class BarcodeAnalyzer(
    private val onCodigoDetectado: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val imagemMedia = imageProxy.image

        if (imagemMedia == null) {
            imageProxy.close()
            return
        }

        val imagem = InputImage.fromMediaImage(imagemMedia, imageProxy.imageInfo.rotationDegrees)

        scanner.process(imagem)
            .addOnSuccessListener { codigos ->
                val primeiro = codigos.firstOrNull { !it.rawValue.isNullOrBlank() }
                primeiro?.rawValue?.let { onCodigoDetectado(it) }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Falha na análise do frame: ${e.message}")
            }
            .addOnCompleteListener {
                // Sempre fecha o frame — libera o buffer da câmera
                imageProxy.close()
            }
    }
}
