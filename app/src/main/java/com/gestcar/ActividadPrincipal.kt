package com.gestcar

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.ui.navegacion.GrafoNavegacion
import com.gestcar.ui.pantallas.PantallaSplash
import com.gestcar.ui.tema.GestCarTema
import com.gestcar.ui.viewmodel.AutenticacionViewModel
import com.gestcar.util.ObservadorConectividad
import com.gestcar.util.PlanificadorNotificaciones
import com.gestcar.util.PlanificadorSincronizacion
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks

// actividad principal de la app, es el punto de entrada
// lo unico que hace es configurar el tema y lanzar el grafo de navegacion
class ActividadPrincipal : ComponentActivity() {

    // guardamos una referencia ligera al viewmodel para poder reaccionar a deeplinks
    // android entrega el enlace en onnewintent y ahi ya no estamos dentro del bloque compose
    private var authViewModelRef: AutenticacionViewModel? = null

    private val permisoNotificacionesLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            PlanificadorNotificaciones.encolarRevisionPuntual(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        solicitarPermisoNotificacionesSiHaceFalta()

        setContent {
            GestCarTema {
                // viewmodel de autenticacion compartido por toda la app
                val authViewModel: AutenticacionViewModel = viewModel()
                authViewModelRef = authViewModel
                val estadoAuth by authViewModel.estado.collectAsState()
                val contexto = LocalContext.current

                // cuando hay una sesion activa observamos la conectividad
                // cada vez que vuelve internet lanzamos una sincronizacion puntual
                // esto evita que un dato creado sin cobertura se quede demasiado tiempo solo en local
                LaunchedEffect(estadoAuth.estaAutenticado, estadoAuth.usuarioId) {
                    if (estadoAuth.estaAutenticado && estadoAuth.usuarioId.isNotBlank()) {
                        ObservadorConectividad(contexto).observarConexion().collect { hayConexion ->
                            if (hayConexion) {
                                PlanificadorSincronizacion.encolarSincronizacionPuntual(contexto)
                            }
                        }
                    }
                }

                // el grafo se monta solo cuando la comprobacion de sesion ha terminado.
                // el splash global no debe depender de una pantalla concreta, porque Android puede
                // restaurar la app directamente en una ruta secundaria como gasolineras.
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!estadoAuth.estaComprobandoSesion) {
                        GrafoNavegacion(
                            authViewModel = authViewModel,
                            estaAutenticado = estadoAuth.estaAutenticado,
                            usuarioId = estadoAuth.usuarioId,
                            alCerrarSesion = { authViewModel.cerrarSesion() },
                        )
                    }

                    AnimatedVisibility(
                        visible = estadoAuth.estaComprobandoSesion,
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
        // supabase devuelve el token de recuperacion en el enlace
        // si es un enlace de recovery mandamos al usuario a cambiar contrasena
        ClienteSupabase.cliente.handleDeeplinks(intentSeguro) {
            if (esRecuperacion) {
                authViewModelRef?.activarModoRestablecerContrasena()
            }
            authViewModelRef?.comprobarSesion()
        }
    }

    private fun solicitarPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val concedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (concedido) {
            PlanificadorNotificaciones.encolarRevisionPuntual(this)
        } else {
            permisoNotificacionesLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
