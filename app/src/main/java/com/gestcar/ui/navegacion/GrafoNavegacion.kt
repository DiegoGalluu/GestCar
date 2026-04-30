package com.gestcar.ui.navegacion

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.gestcar.ui.pantallas.PantallaDetalleGasto
import com.gestcar.ui.pantallas.PantallaDetalleRecordatorio
import com.gestcar.ui.pantallas.PantallaDetalleVehiculo
import com.gestcar.ui.pantallas.PantallaDetalleMantenimiento
import com.gestcar.ui.pantallas.PantallaDetalleRepostaje
import com.gestcar.ui.pantallas.PantallaCuenta
import com.gestcar.ui.pantallas.PantallaFormularioGasto
import com.gestcar.ui.pantallas.PantallaFormularioMantenimiento
import com.gestcar.ui.pantallas.PantallaFormularioRecordatorio
import com.gestcar.ui.pantallas.PantallaFormularioVehiculo
import com.gestcar.ui.pantallas.PantallaFormularioRepostaje
import com.gestcar.ui.pantallas.PantallaInicioSesion
import com.gestcar.ui.pantallas.PantallaListaGastos
import com.gestcar.ui.pantallas.PantallaListaMantenimientos
import com.gestcar.ui.pantallas.PantallaListaRecordatorios
import com.gestcar.ui.pantallas.PantallaListaVehiculos
import com.gestcar.ui.pantallas.PantallaListaRepostajes
import com.gestcar.ui.pantallas.PantallaMasOpciones
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
    ElementoNavegacion(Rutas.LISTA_VEHICULOS, "Vehículos", Icons.Default.DirectionsCar),
    ElementoNavegacion(Rutas.GASTOS, "Gastos", Icons.Default.Payments),
    ElementoNavegacion(Rutas.REPOSTAJES, "Repostajes", Icons.Default.LocalGasStation),
    ElementoNavegacion(Rutas.MANTENIMIENTO, "Mantenim.", Icons.Default.Build),
    ElementoNavegacion(Rutas.MAS_OPCIONES, "Más", Icons.Default.MoreHoriz)
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
    alCerrarSesion: () -> Unit,
    alListaVehiculosCargada: () -> Unit = {}
) {
    // determinamos la pantalla de inicio segun si el usuario esta logueado o no
    val pantallaInicio = if (estaAutenticado) Rutas.LISTA_VEHICULOS else Rutas.INICIO_SESION
    val estadoAuth by authViewModel.estado.collectAsState()

    // miramos la ruta actual para saber si mostramos la barra inferior
    val entradaActual by controladorNav.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val rutaPrincipalActual = rutaPrincipalDeBarra(rutaActual)
    val mostrarBarraInferior = rutaPrincipalActual in rutasConBarraInferior

    LaunchedEffect(estadoAuth.modoRestablecerContrasena, rutaActual) {
        // si el usuario llega desde el enlace de recuperacion de contrasena
        // forzamos la ruta correcta aunque la app estuviese abierta en otra pantalla
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
            modifier = Modifier.padding(paddingInterior),
            // transiciones cortas para que la navegacion no sea brusca
            // no usamos animaciones largas porque la app es utilitaria y debe sentirse agil
            enterTransition = { fadeIn(animationSpec = tween(180)) },
            exitTransition = { fadeOut(animationSpec = tween(120)) },
            popEnterTransition = { fadeIn(animationSpec = tween(180)) },
            popExitTransition = { fadeOut(animationSpec = tween(120)) }
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
                    // avisa a actividadprincipal cuando ya se puede retirar el splash inicial
                    // asi no se ve el login ni una pantalla vacia durante la carga de sesion
                    alCargaInicialCompletada = alListaVehiculosCargada,
                    alPulsarVehiculo = { vehiculoId ->
                        controladorNav.navigate(Rutas.detalleVehiculo(vehiculoId))
                    },
                    alAnadirVehiculo = {
                        controladorNav.navigate(Rutas.formularioVehiculo())
                    }
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
                    alEliminar = { controladorNav.popBackStack() },
                    alVerRepostajes = { controladorNav.navigate(Rutas.repostajesVehiculo(vehiculoId)) },
                    alVerMantenimientos = { controladorNav.navigate(Rutas.mantenimientoVehiculo(vehiculoId)) },
                    alVerGastos = { controladorNav.navigate(Rutas.gastosVehiculo(vehiculoId)) },
                    alVerRecordatorios = { controladorNav.navigate(Rutas.recordatoriosVehiculo(vehiculoId)) }
                )
            }

            composable(Rutas.REPOSTAJES) {
                PantallaListaRepostajes(
                    usuarioId = usuarioId,
                    alCrearRepostaje = { vehiculoId ->
                        controladorNav.navigate(Rutas.formularioRepostaje(vehiculoId))
                    },
                    alVerDetalleRepostaje = { vehiculoId, repostajeId ->
                        controladorNav.navigate(Rutas.detalleRepostaje(vehiculoId, repostajeId))
                    }
                )
            }

            composable(
                route = Rutas.REPOSTAJES_VEHICULO,
                arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType })
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId")
                PantallaListaRepostajes(
                    usuarioId = usuarioId,
                    vehiculoInicialId = vehiculoId,
                    alCrearRepostaje = { id ->
                        controladorNav.navigate(Rutas.formularioRepostaje(id))
                    },
                    alVerDetalleRepostaje = { id, repostajeId ->
                        controladorNav.navigate(Rutas.detalleRepostaje(id, repostajeId))
                    }
                )
            }

            composable(Rutas.MANTENIMIENTO) {
                PantallaListaMantenimientos(
                    usuarioId = usuarioId,
                    alCrearMantenimiento = { vehiculoId ->
                        controladorNav.navigate(Rutas.formularioMantenimiento(vehiculoId))
                    },
                    alVerDetalleMantenimiento = { vehiculoId, mantenimientoId ->
                        controladorNav.navigate(Rutas.detalleMantenimiento(vehiculoId, mantenimientoId))
                    }
                )
            }

            composable(
                route = Rutas.MANTENIMIENTO_VEHICULO,
                arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType })
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId")
                PantallaListaMantenimientos(
                    usuarioId = usuarioId,
                    vehiculoInicialId = vehiculoId,
                    alCrearMantenimiento = { id ->
                        controladorNav.navigate(Rutas.formularioMantenimiento(id))
                    },
                    alVerDetalleMantenimiento = { id, mantenimientoId ->
                        controladorNav.navigate(Rutas.detalleMantenimiento(id, mantenimientoId))
                    }
                )
            }

            composable(Rutas.GASTOS) {
                PantallaListaGastos(
                    usuarioId = usuarioId,
                    alCrearGasto = { vehiculoId ->
                        controladorNav.navigate(Rutas.formularioGasto(vehiculoId))
                    },
                    alVerDetalleGasto = { vehiculoId, gastoId ->
                        controladorNav.navigate(Rutas.detalleGasto(vehiculoId, gastoId))
                    }
                )
            }

            composable(
                route = Rutas.GASTOS_VEHICULO,
                arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType })
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId")
                PantallaListaGastos(
                    usuarioId = usuarioId,
                    vehiculoInicialId = vehiculoId,
                    alCrearGasto = { id ->
                        controladorNav.navigate(Rutas.formularioGasto(id))
                    },
                    alVerDetalleGasto = { id, gastoId ->
                        controladorNav.navigate(Rutas.detalleGasto(id, gastoId))
                    }
                )
            }

            composable(Rutas.MAS_OPCIONES) {
                PantallaMasOpciones(
                    alIrARecordatorios = {
                        controladorNav.navigate(Rutas.RECORDATORIOS)
                    },
                    alIrAEstadisticas = {
                        controladorNav.navigate(Rutas.ESTADISTICAS)
                    },
                    alIrACuenta = {
                        controladorNav.navigate(Rutas.CUENTA)
                    },
                    alCerrarSesion = alCerrarSesion
                )
            }

            composable(Rutas.CUENTA) {
                PantallaCuenta(
                    correoUsuario = estadoAuth.correoUsuario,
                    usuarioId = usuarioId,
                    alVolver = { controladorNav.popBackStack() },
                    alCuentaEliminada = alCerrarSesion
                )
            }

            composable(Rutas.RECORDATORIOS) {
                PantallaListaRecordatorios(
                    usuarioId = usuarioId,
                    alCrearRecordatorio = { vehiculoId ->
                        controladorNav.navigate(Rutas.formularioRecordatorio(vehiculoId))
                    },
                    alVerDetalleRecordatorio = { vehiculoId, recordatorioId ->
                        controladorNav.navigate(Rutas.detalleRecordatorio(vehiculoId, recordatorioId))
                    },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.RECORDATORIOS_VEHICULO,
                arguments = listOf(navArgument("vehiculoId") { type = NavType.StringType })
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId")
                PantallaListaRecordatorios(
                    usuarioId = usuarioId,
                    vehiculoInicialId = vehiculoId,
                    alCrearRecordatorio = { id ->
                        controladorNav.navigate(Rutas.formularioRecordatorio(id))
                    },
                    alVerDetalleRecordatorio = { id, recordatorioId ->
                        controladorNav.navigate(Rutas.detalleRecordatorio(id, recordatorioId))
                    },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.DETALLE_RECORDATORIO,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("recordatorioId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val recordatorioId = entrada.arguments?.getString("recordatorioId") ?: ""
                PantallaDetalleRecordatorio(
                    recordatorioId = recordatorioId,
                    alEditar = { _, id ->
                        controladorNav.navigate(Rutas.formularioRecordatorio(vehiculoId, id))
                    },
                    alVolver = { controladorNav.popBackStack() },
                    alEliminar = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.FORMULARIO_RECORDATORIO,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("recordatorioId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val recordatorioId = entrada.arguments?.getString("recordatorioId") ?: "nuevo"
                PantallaFormularioRecordatorio(
                    vehiculoId = vehiculoId,
                    recordatorioId = recordatorioId,
                    alGuardar = { controladorNav.popBackStack() },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.DETALLE_GASTO,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("gastoId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val gastoId = entrada.arguments?.getString("gastoId") ?: ""
                PantallaDetalleGasto(
                    gastoId = gastoId,
                    alEditar = { _, id ->
                        controladorNav.navigate(Rutas.formularioGasto(vehiculoId, id))
                    },
                    alVolver = { controladorNav.popBackStack() },
                    alEliminar = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.FORMULARIO_GASTO,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("gastoId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val gastoId = entrada.arguments?.getString("gastoId") ?: "nuevo"
                PantallaFormularioGasto(
                    vehiculoId = vehiculoId,
                    gastoId = gastoId,
                    alGuardar = { controladorNav.popBackStack() },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.DETALLE_REPOSTAJE,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("repostajeId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val repostajeId = entrada.arguments?.getString("repostajeId") ?: ""
                PantallaDetalleRepostaje(
                    repostajeId = repostajeId,
                    alEditar = { _, id ->
                        controladorNav.navigate(Rutas.formularioRepostaje(vehiculoId, id))
                    },
                    alVolver = { controladorNav.popBackStack() },
                    alEliminar = { controladorNav.popBackStack() }
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

            composable(
                route = Rutas.DETALLE_MANTENIMIENTO,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("mantenimientoId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val mantenimientoId = entrada.arguments?.getString("mantenimientoId") ?: ""
                PantallaDetalleMantenimiento(
                    mantenimientoId = mantenimientoId,
                    alEditar = { _, id ->
                        controladorNav.navigate(Rutas.formularioMantenimiento(vehiculoId, id))
                    },
                    alVolver = { controladorNav.popBackStack() },
                    alEliminar = { controladorNav.popBackStack() }
                )
            }

            composable(
                route = Rutas.FORMULARIO_MANTENIMIENTO,
                arguments = listOf(
                    navArgument("vehiculoId") { type = NavType.StringType },
                    navArgument("mantenimientoId") { type = NavType.StringType }
                )
            ) { entrada ->
                val vehiculoId = entrada.arguments?.getString("vehiculoId") ?: ""
                val mantenimientoId = entrada.arguments?.getString("mantenimientoId") ?: "nuevo"
                PantallaFormularioMantenimiento(
                    vehiculoId = vehiculoId,
                    mantenimientoId = mantenimientoId,
                    alGuardar = { controladorNav.popBackStack() },
                    alVolver = { controladorNav.popBackStack() }
                )
            }

            // pantallas placeholder para las secciones que aun no estan implementadas
            composable(Rutas.ESTADISTICAS) {
                PantallaPlaceholder(
                    nombreSeccion = "Estadísticas",
                    alVolver = { controladorNav.popBackStack() }
                )
            }
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
                selected = rutaActual == elemento.ruta || rutaPrincipalDeBarra(rutaActual) == elemento.ruta,
                onClick = {
                    controladorNav.navegarASeccionPrincipal(elemento.ruta)
                }
            )
        }
    }
}

private fun rutaPrincipalDeBarra(ruta: String?): String? {
    // algunas pantallas tienen rutas con parametros
    // las agrupamos bajo la seccion principal para que la bottom bar marque bien el icono
    return when (ruta) {
        Rutas.LISTA_VEHICULOS -> Rutas.LISTA_VEHICULOS
        Rutas.GASTOS, Rutas.GASTOS_VEHICULO -> Rutas.GASTOS
        Rutas.REPOSTAJES, Rutas.REPOSTAJES_VEHICULO -> Rutas.REPOSTAJES
        Rutas.MANTENIMIENTO, Rutas.MANTENIMIENTO_VEHICULO -> Rutas.MANTENIMIENTO
        Rutas.MAS_OPCIONES -> Rutas.MAS_OPCIONES
        else -> null
    }
}

private fun NavHostController.navegarASeccionPrincipal(ruta: String) {
    val rutaActual = currentBackStackEntry?.destination?.route
    if (rutaPrincipalDeBarra(rutaActual) == ruta) {
        return
    }

    // al tocar la barra inferior volvemos a la seccion principal
    // popupto evita apilar pantallas infinitas al navegar entre tabs
    navigate(ruta) {
        popUpTo(Rutas.LISTA_VEHICULOS) {
            inclusive = false
        }
        launchSingleTop = true
    }
}
