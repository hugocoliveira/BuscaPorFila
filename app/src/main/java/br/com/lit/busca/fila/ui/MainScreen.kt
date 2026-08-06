package br.com.lit.busca.fila.ui

import android.Manifest
import android.media.MediaPlayer
import android.util.Log
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.lit.busca.fila.R
import br.com.lit.busca.fila.scanner.iniciarScanner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService

private const val TAG = "MainScreen"

// ---------------------------------------------------------------------------
// Tela principal do BuscaPorFila.
// Exibe a câmera imediatamente ao abrir. Após leitura, exibe os campos do QR.
// O usuário não pode digitar texto — apenas escanear.
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Toca error.wav sempre que uiState.erro mudar para não-nulo
    LaunchedEffect(uiState.erro) {
        if (uiState.erro != null) {
            val mp = MediaPlayer.create(context, R.raw.error)
            mp.setOnCompletionListener { it.release() }
            mp.start()
        }
    }

    val permissaoCamera = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = stringResource(R.string.titulo_tela),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ConteudoPrincipal(
                uiState        = uiState,
                onAbrirScanner = {
                    if (permissaoCamera.status.isGranted) {
                        viewModel.onAbrirScanner()
                    } else {
                        permissaoCamera.launchPermissionRequest()
                    }
                }
            )

            // Overlay de câmera — sobrepõe o conteúdo quando o scanner está aberto
            AnimatedVisibility(
                visible = uiState.scannerAberto && permissaoCamera.status.isGranted,
                enter   = fadeIn(),
                exit    = fadeOut()
            ) {
                ScannerOverlay(
                    onCodigoLido   = viewModel::onCodigoEscaneado,
                    onFechar       = viewModel::onFecharScanner,
                    lifecycleOwner = lifecycleOwner,
                    context        = context
                )
            }

            // Solicita permissão automaticamente se o scanner abriu sem permissão concedida
            if (uiState.scannerAberto && !permissaoCamera.status.isGranted) {
                permissaoCamera.launchPermissionRequest()
            }
        }
    }
}

@Composable
private fun ConteudoPrincipal(
    uiState: UiState,
    onAbrirScanner: () -> Unit
) {
    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Banner de erro
        if (uiState.erro != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape  = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text     = uiState.erro,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Card com os campos lidos do QR Code
        if (!uiState.campos.isNullOrEmpty()) {
            item {
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape     = RoundedCornerShape(6.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        uiState.campos.forEachIndexed { indice, (rotulo, valor) ->
                            if (indice > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color    = MaterialTheme.colorScheme.outline
                                )
                            }
                            CampoLinha(rotulo = rotulo, valor = valor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Ícone + texto de espera — exibido antes do primeiro scan sem erro
        if (uiState.campos == null && uiState.erro == null) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector        = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.primary,
                            modifier           = Modifier
                                .width(64.dp)
                                .height(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text  = stringResource(R.string.aguardando_leitura),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Botão escanear / escanear novamente
        item {
            Button(
                onClick  = onAbrirScanner,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.QrCodeScanner,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = if (uiState.campos != null)
                                stringResource(R.string.botao_escanear_novamente)
                            else
                                stringResource(R.string.botao_escanear),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

/**
 * Uma linha do card de resultados exibindo o rótulo do campo e seu valor.
 *
 * @param rotulo nome do campo (ex: "TD", "ITD").
 * @param valor  conteúdo do campo lido do QR Code.
 */
@Composable
private fun CampoLinha(rotulo: String, valor: String) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = "$rotulo:",
            style      = MaterialTheme.typography.bodySmall,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.width(60.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text     = valor,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Overlay de câmera que cobre a tela inteira.
 * Chama [onCodigoLido] apenas uma vez por abertura (guarda em [jaLeu]).
 */
@Composable
private fun ScannerOverlay(
    onCodigoLido: (String) -> Unit,
    onFechar: () -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    context: android.content.Context
) {
    val executorRef = remember { mutableListOf<ExecutorService>() }

    DisposableEffect(Unit) {
        onDispose {
            executorRef.firstOrNull()?.shutdown()
            executorRef.clear()
            Log.d(TAG, "Executor da câmera encerrado.")
        }
    }

    val jaLeu = remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = iniciarScanner(
                    context        = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView    = previewView,
                    onCodigoLido   = { codigo ->
                        if (!jaLeu.value) {
                            jaLeu.value = true
                            onCodigoLido(codigo)
                        }
                    }
                )
                executorRef.add(executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        IconButton(
            onClick  = onFechar,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector        = Icons.Default.Close,
                contentDescription = stringResource(R.string.fechar_scanner),
                tint               = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
