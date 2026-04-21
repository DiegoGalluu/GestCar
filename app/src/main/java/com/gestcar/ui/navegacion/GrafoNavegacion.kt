package com.gestcar.ui.navegacion

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gestcar.ui.pantallas.PantallaDetalleVehiculo
import com.gestcar.ui.pantallas.PantallaFormularioVehiculo
import com.gestcar.ui.pantallas.PantallaFormularioRepostaje
import com.gestcar.ui.pantallas.PantallaInicioSesion
import com.gestcar.ui.pantallas.PantallaListaVehiculos
import com.gestcar.ui.pantallas.PantallaListaRepostajes
import com.gestcar.ui.pantallas.PantallaPlaceholder
import com.gestcar.ui.pantallas.PantallaRegistro
import com.gestcar.ui.pantallas.PantallaRestablecerContrasena
import com.gestcar.ui.viewmodel.AutenticacionViewModel

// clase que define cada elemento de la barra de navegacion inferior
data class ElementoNavegacion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

// lista de elementos que aparecen en la barra inferior
val elementosNavegacion = listOf(
    ElementoNavegacion(Rutas.LISTA_VEHICULOS, "Vehiculos", Icons.Default.DirectionsCar),
    ElementoNavegacion(Rutas.GASTOS, "Gastos", Icons.Default.Payments),
    ElementoNavegacion(Rutas.REPOSTAJES, "Repostajes", Icons.Default.LocalGasStation),
    ElementoNavegacion(Rutas.MANTENIMIENTO, "Mantenim.", Icons.Default.Build),
    ElementoNavegacion(Rutas.MAS_OPCIONES, "Mas", Icons.Default.MoreHoriz)
)

// rutas en las que se muestra la barra de navegacion inferior
// no se muestra en login, registro ni en formularios
private val rutasConBarraInferior = listOf(
    Rutas.LISTA_VEHICULOS,
    Rutas.GASTOS,
    Rutas.REPOSTAJES,
    Rutas.MANTENIMIENTO,
    Rutas.MAS_OPCIONES
)

// grafo de navegacion principal de la app
// aqui se definen todas las pantallas y como se navega entre ellas
@Composable
fun GrafoNavegacion(
    controladorNav: NavHostController = rememberNavController(),
    authViewModel: AutenticacionViewModel,
    estaAutenticado: Boolean,
    usuarioId: String,
    alCerrarSesion: () -> Unit
) {
    // determinamos la pantalla de inicio segun si el usuario esta logueado o no
    val pantallaInicio = if (estaAutenticado) Rutas.LISTA_VEHICULOS else Rutas.INICIO_SESION
    val estadoAuth by authViewModel.estado.collectAsState()

    // miramos la ruta actual para saber si mostramos la barra inferior
    val entradaActual by controladorNav.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val mostrarBarraInferior = rutaActual in rutasConBarraInferior

    LaunchedEffect(estadoAuth.modoRestablecerContrasena, rutaActual) {
        if (estadoAuth.modoRestablecerContrasena && rutaActual != Rutas.RESTABLECER_CONTRASENA) {
            controladorNav.navigate(Rutas.RESTABLECER_CONTRASENA)
        }
    }

    Scaffold(
        bottomBar = {
            if (mostrarBarraInferior) {
                BarraNavegacionInferior(
                    controladorNav = controladorNav,
                    rutaActual = rutaActual
                )
            }
        }
    ) { paddingInterior ->
        NavHost(
            navController = controladorNav,
            startDestination = pantallaInicio,
            modifier = Modifier.padding(paddingInterior)
        ) {
            // pantalla de inicio de sesion
            composable(Rutas.INICIO_SESION) {
                PantallaInicioSesion(
                    viewModel = authViewModel,
                    alIniciarSesion = {
                        controladorNav.navigate(Rutas.LISTA_VEHICULOS) {
                            popUpTo(Rutas.INICIO_SESION) { inclusive = true }
                        }
                    },
                    alIrARegistro = {
                        controladorNav.navigate(Rutas.REGISTRO)
                    },
                    alRecuperarContrasena = { email ->
                        authViewModel.solicitarRestablecimientoContrasena(email)
                    }
                )
            }

            // pantalla de registro de cuenta nueva
            composable(Rutas.REGISTRO) {
                PantallaRegistro(
                    viewModel = authViewModel,
                    alRegistrarse = {
                        controladorNav.navigate(Rutas.LISTA_VEHICULOS) {
                            popUpTo(Rutas.INICIO_SESION) { inclusive = true }
                        }
                    },
                    alVolverALogin = {
                        controladorNav.popBackStack()
                    }
                )
            }

            composable(Rutas.RESTABLECER_CONTRASENA) {
                PantallaRestablecerContrasena(
                    viewModel = authViewModel,
                    alCancelar = { controladorNav.popBackStack() }
                )
            }

            // pantalla principal con la lista de vehiculos
            composable(Rutas.LISTA_VEHICULOS) {
                PantallaListaVehiculos(
                    usuarioId = usuarioId,
                    alPulsarVehiculo = { vehiculoId ->
                        controladorNav.navigate(Rutas.detalleVehiculo(vehiculoId))
                    },
                    alAnadirVehiculo = {
                        controladorNav.navigate(Rutas.formularioVehiculo())
                    },
                    alCerrarSesion = alCerrarSesion
                )
            }

            // formulario para crear o editar un vehiculo
            composable(
                route = Rutas.FORMULARIO_VEHICULO,
                arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType })
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: "nuevo"
                PantallaFormularioVehiculo(
                    vehiculoId = vehiculoId,
                    usuarioId = usuarioId,
                    alGuardar = { controladorNav.popBackStack() },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            // pantalla de detalle de un vehiculo
            composable(
                route = Rutas.DETALLE_VEHICULO,
                arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType })
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                PantallaDetalleVehiculo(
                    vehiculoId = vehiculoId,
                    alEditar = {
                        controladorNav.navigate(Rutas.formularioVehiculo(vehiculoId))
                    },
                    alVolver = { controladorNav.popBackStack() },
                    alEliminar = { controladorNav.popBackStack() }
                )
            }

            composable(Rutas.REPOSTAJES) {
                PantallaListaRepostajes(
                    usuarioId = usuarioId,
                    alCrearRepostaje = { vehiculoId ->
                        controladorNav.navigate(Rutas.formularioRepostaje(vehiculoId))
                    },
                    alEditarRepostaje = { vehiculoId, repostajeId ->
                        controladorNav.navigate(Rutas.formularioRepostaje(vehiculoId, repostajeId))
                    }
                )
            }

            composable(
                route = Rutas.FORMULARIO_REPOSTAJE,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("repostajeId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val repostajeId = entrada.arguments?.getString("repostajeId") ?: "nuevo"
                PantallaFormularioRepostaje(
                    vehiculoId = vehiculoId,
                    repostajeId = repostajeId,
                    alGuardar = { controladorNav.popBackStack() },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            // pantallas placeholder para las secciones que aun no estan implementadas
            composable(Rutas.GASTOS) { PantallaPlaceholder("Gastos") }
            composable(Rutas.MANTENIMIENTO) { PantallaPlaceholder("Mantenimiento") }
            composable(Rutas.MAS_OPCIONES) { PantallaPlaceholder("Mas opciones") }
        }
    }
}

// barra de navegacion inferior con los 5 apartados principales
@Composable
fun BarraNavegacionInferior(
    controladorNav: NavHostController,
    rutaActual: String?
) {
    NavigationBar {
        elementosNavegacion.forEach { elemento ->
            NavigationBarItem(
                icon = { Icon(elemento.icono, contentDescription = elemento.titulo) },
                label = { Text(elemento.titulo) },
                selected = rutaActual == elemento.ruta,
                onClick = {
                    // navegamos a la ruta del elemento, evitando duplicados en la pila
                    controladorNav.navigate(elemento.ruta) {
                        popUpTo(Rutas.LISTA_VEHICULOS) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
