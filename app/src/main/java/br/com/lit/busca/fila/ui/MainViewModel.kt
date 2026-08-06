package br.com.lit.busca.fila.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// ViewModel da tela de leitura de QR Code.
// Concentra todo o estado e lógica de negócio — os Composables são stateless.
// ---------------------------------------------------------------------------

/**
 * Rótulos dos campos na ordem exata em que aparecem no QR Code da Fila.
 * Formato do QR: TD;ITD;TDs;St.;TpPr;PArm;Prod;PDO;PDD;DtCo;HrCo
 */
private val ROTULOS_CAMPOS = listOf(
    "TD", "ITD", "TDs", "St.", "TpPr", "PArm", "Prod", "PDO", "PDD", "DtCo", "HrCo"
)

/**
 * Estado imutável da tela de leitura.
 *
 * @property campos        lista de pares (rótulo, valor) extraídos do QR Code.
 *                         null enquanto nenhum código foi lido com sucesso.
 * @property scannerAberto true quando o overlay de câmera está visível.
 * @property erro          mensagem de erro ou null se não há erro.
 */
data class UiState(
    val campos: List<Pair<String, String>>? = null,
    val scannerAberto: Boolean              = false,
    val erro: String?                       = null
)

/**
 * ViewModel que gerencia o estado e os eventos da tela [MainScreen].
 */
class MainViewModel : ViewModel() {

    /** Estado interno mutável. */
    private val _uiState = MutableStateFlow(UiState())

    /** Estado observável pelos Composables. */
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // -----------------------------------------------------------------------
    // Eventos de UI
    // -----------------------------------------------------------------------

    /**
     * Chamado quando a câmera detecta um QR Code.
     * Faz o parse do conteúdo e fecha o scanner.
     *
     * @param codigo valor bruto lido pela câmera (ex: "100000502;1;1;C;Y114;...").
     */
    fun onCodigoEscaneado(codigo: String) {
        val partes = codigo.split(";")

        if (partes.size != ROTULOS_CAMPOS.size) {
            // QR com número de campos inesperado — exibe erro e mantém scanner fechado
            _uiState.update {
                it.copy(
                    scannerAberto = false,
                    erro   = "QR Code inválido: esperado ${ROTULOS_CAMPOS.size} campos, recebido ${partes.size}.",
                    campos = null
                )
            }
            return
        }

        val campos = ROTULOS_CAMPOS.zip(partes)

        _uiState.update {
            it.copy(
                scannerAberto = false,
                campos        = campos,
                erro          = null
            )
        }
    }

    /** Abre o overlay da câmera para uma nova leitura. */
    fun onAbrirScanner() {
        _uiState.update { it.copy(scannerAberto = true, erro = null) }
    }

    /** Fecha o overlay da câmera sem alterar os dados exibidos. */
    fun onFecharScanner() {
        _uiState.update { it.copy(scannerAberto = false) }
    }
}
