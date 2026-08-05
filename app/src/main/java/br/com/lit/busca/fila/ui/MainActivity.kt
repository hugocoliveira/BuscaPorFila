package br.com.lit.busca.fila.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.lit.busca.fila.ui.theme.BuscaPorFilaTheme

// ---------------------------------------------------------------------------
// Entry point do aplicativo BuscaPorFila.
// Responsabilidade única: inicializar o tema e exibir a tela raiz.
// Toda lógica de estado e negócio fica no MainViewModel.
// ---------------------------------------------------------------------------

/**
 * Activity principal e única do BuscaPorFila.
 * Não possui intent-filter de LAUNCHER — é iniciada via Intent explícita
 * pelo aplicativo MenuAutomatico (launcher controlado do armazém).
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ao pressionar voltar: encerra a Activity e remove a task dos recentes,
        // evitando que o app fique aberto em segundo plano após retornar ao MenuAutomatico.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishAndRemoveTask()
            }
        })

        enableEdgeToEdge()

        setContent {
            BuscaPorFilaTheme {
                MainScreen()
            }
        }
    }
}
