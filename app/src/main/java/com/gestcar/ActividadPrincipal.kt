package com.gestcar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.navegacion.GrafoNavegacion
import com.gestcar.ui.tema.GestCarTema
import com.gestcar.ui.viewmodel.AutenticacionViewModel

// actividad principal de la app, es el punto de entrada
// lo unico que hace es configurar el tema y lanzar el grafo de navegacion
class ActividadPrincipal : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GestCarTema {
                // viewmodel de autenticacion compartido por toda la app
                val authViewModel: AutenticacionViewModel = viewModel()
                val estadoAuth by authViewModel.estado.collectAsState()

                GrafoNavegacion(
                    authViewModel = authViewModel,
                    estaAutenticado = estadoAuth.estaAutenticado,
                    usuarioId = estadoAuth.usuarioId,
                    alCerrarSesion = { authViewModel.cerrarSesion() }
                )
            }
        }
    }
}
