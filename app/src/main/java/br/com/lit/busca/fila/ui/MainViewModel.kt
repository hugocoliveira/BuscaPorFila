package br.com.lit.busca.fila.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ---------------------------------------------------------------------------
// ViewModel da tela de leitura de QR Code do BuscaPorFila.
// ---------------------------------------------------------------------------

/**
 * Rótulos dos campos na ordem exata em que aparecem no QR Code da Fila.
 * Formato do QR: TD;ITD;TDs;St.;TpPr;PArm;Prod;PDO;PDD;DtCo;HrCo (separador ; diferencia do BuscaPorUC que usa |)
 */
private val ROTULOS_CAMPOS = listOf(
    "TD", "ITD", "TDs", "St.", "TpPr", "PArm", "Prod", "PDO", "PDD", "DtCo", "HrCo"
)

/**
 * Estado imutável da tela de leitura.
 *
 * @property campoBusca    texto digitado ou lido pelo scanner.
 * @property campos        lista de pares (rótulo, valor) extraídos do QR Code.
 *                         null enquanto nenhum código foi lido com sucesso.
 * @property scannerAberto true quando o overlay de câmera está visível.
 * @property erro          mensagem de erro ou null se não há erro.
 */
data class UiState(
    val campoBusca: String                  = "",
    val campos: List<Pair<String, String>>? = null,
    val scannerAberto: Boolean              = false,
    val erro: String?                       = null
)

/**
 * ViewModel que gerencia o estado e os eventos da tela [MainScreen].
 */
class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /** Atualiza o texto do campo de busca e limpa erros anteriores. */
    fun onCampoAlterado(texto: String) {
        _uiState.update { it.copy(campoBusca = texto, erro = null) }
    }

    /** Limpa campo, resultados e erros. */
    fun onLimpar() {
        _uiState.update { UiState() }
    }

    /** Dispara o parse com o conteúdo atual do campo. */
    fun onBuscar() {
        onCodigoEscaneado(_uiState.value.campoBusca)
    }

    /**
     * Chamado quando a câmera detecta um QR Code ou o usuário confirma o campo.
     * Faz o parse por ';' e mapeia para os rótulos definidos em [ROTULOS_CAMPOS].
     *
     * @param codigo valor bruto lido (ex: "100000502;1;1;C;Y114;...").
     */
    fun onCodigoEscaneado(codigo: String) {
        val partes = codigo.split(";")

        if (partes.size != ROTULOS_CAMPOS.size) {
            _uiState.update {
                it.copy(
                    campoBusca    = codigo,
                    scannerAberto = false,
                    erro          = "Dado não encontrado",
                    campos        = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                campoBusca    = "",
                scannerAberto = false,
                campos        = ROTULOS_CAMPOS.zip(partes),
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
