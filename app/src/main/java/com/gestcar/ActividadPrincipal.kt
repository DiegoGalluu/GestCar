package com.gestcar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.ui.navegacion.GrafoNavegacion
import com.gestcar.ui.tema.GestCarTema
import com.gestcar.ui.viewmodel.AutenticacionViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks

// actividad principal de la app, es el punto de entrada
// lo unico que hace es configurar el tema y lanzar el grafo de navegacion
class ActividadPrincipal : ComponentActivity() {

    private var authViewModelRef: AutenticacionViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GestCarTema {
                // viewmodel de autenticacion compartido por toda la app
                val authViewModel: AutenticacionViewModel = viewModel()
                authViewModelRef = authViewModel
                val estadoAuth by authViewModel.estado.collectAsState()

                GrafoNavegacion(
                    authViewModel = authViewModel,
                    estaAutenticado = estadoAuth.estaAutenticado,
                    usuarioId = estadoAuth.usuarioId,
                    alCerrarSesion = { authViewModel.cerrarSesion() }
                )
            }
        }

        procesarDeepLinkAuth(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarDeepLinkAuth(intent)
    }

    private fun procesarDeepLinkAuth(intent: Intent?) {
        val intentSeguro = intent ?: return
        val fragmento = intentSeguro.data?.fragment.orEmpty()
        val esRecuperacion = fragmento.contains("type=recovery")
        ClienteSupabase.cliente.handleDeeplinks(intentSeguro) {
            if (esRecuperacion) {
                authViewModelRef?.activarModoRestablecerContrasena()
            }
            authViewModelRef?.comprobarSesion()
        }
    }
}
