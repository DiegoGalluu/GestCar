package com.gestcar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.ui.navegacion.GrafoNavegacion
import com.gestcar.ui.pantallas.PantallaSplash
import com.gestcar.ui.tema.GestCarTema
import com.gestcar.ui.viewmodel.AutenticacionViewModel
import com.gestcar.util.ObservadorConectividad
import com.gestcar.util.PlanificadorSincronizacion
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
                val contexto = LocalContext.current
                var esperandoCargaVehiculos by remember { mutableStateOf(false) }

                LaunchedEffect(estadoAuth.estaAutenticado, estadoAuth.usuarioId) {
                    if (estadoAuth.estaAutenticado && estadoAuth.usuarioId.isNotBlank()) {
                        ObservadorConectividad(contexto).observarConexion().collect { hayConexion ->
                            if (hayConexion) {
                                PlanificadorSincronizacion.encolarSincronizacionPuntual(contexto)
                            }
                        }
                    }
                }

                LaunchedEffect(
                    estadoAuth.estaComprobandoSesion,
                    estadoAuth.estaAutenticado,
                    estadoAuth.usuarioId
                ) {
                    esperandoCargaVehiculos = estadoAuth.estaAutenticado &&
                        estadoAuth.usuarioId.isNotBlank() &&
                        !estadoAuth.estaComprobandoSesion
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!estadoAuth.estaComprobandoSesion) {
                        GrafoNavegacion(
                            authViewModel = authViewModel,
                            estaAutenticado = estadoAuth.estaAutenticado,
                            usuarioId = estadoAuth.usuarioId,
                            alCerrarSesion = { authViewModel.cerrarSesion() },
                            alListaVehiculosCargada = {
                                esperandoCargaVehiculos = false
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = estadoAuth.estaComprobandoSesion || esperandoCargaVehiculos,
                        enter = fadeIn(animationSpec = tween(160)),
                        exit = fadeOut(animationSpec = tween(220))
                    ) {
                        PantallaSplash()
                    }
                }
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
