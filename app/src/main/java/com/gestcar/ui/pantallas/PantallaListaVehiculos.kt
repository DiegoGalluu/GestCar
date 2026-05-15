package com.gestcar.ui.pantallas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.TarjetaVehiculo
import com.gestcar.ui.viewmodel.VehiculoViewModel

private const val PREFERENCIAS_NOTIFICACIONES = "preferencias_notificaciones"
private const val CLAVE_AVISO_NOTIFICACIONES_MOSTRADO = "aviso_notificaciones_mostrado"

// pantalla principal de la app, muestra la lista de vehiculos del usuario
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaVehiculos(
    usuarioId: String,
    alCargaInicialCompletada: () -> Unit = {},
    alSolicitarPermisoNotificaciones: () -> Unit = {},
    alPulsarVehiculo: (String) -> Unit,
    alAnadirVehiculo: () -> Unit,
    viewModel: VehiculoViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val estado by viewModel.estadoLista.collectAsState()
    var modoOrganizacion by remember { mutableStateOf(false) }
    var vehiculosOrganizacion by remember { mutableStateOf(emptyList<Vehiculo>()) }
    var haVistoCargaInicial by remember { mutableStateOf(false) }
    var avisoCargaInicialEnviado by remember { mutableStateOf(false) }
    var avisoNotificacionesEvaluado by remember { mutableStateOf(false) }
    var mostrarAvisoNotificaciones by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId) {
        // reiniciamos el aviso para que el splash inicial no desaparezca antes de tiempo
        // se usa sobre todo cuando la app recupera una sesion guardada
        haVistoCargaInicial = false
        avisoCargaInicialEnviado = false
        avisoNotificacionesEvaluado = false
        mostrarAvisoNotificaciones = false
        viewModel.cargarVehiculos(usuarioId)
    }

    LaunchedEffect(estado.estaCargando) {
        if (estado.estaCargando) {
            haVistoCargaInicial = true
        } else if (haVistoCargaInicial && !avisoCargaInicialEnviado) {
            avisoCargaInicialEnviado = true
            alCargaInicialCompletada()
        }
    }

    LaunchedEffect(avisoCargaInicialEnviado, usuarioId) {
        if (avisoCargaInicialEnviado && !avisoNotificacionesEvaluado) {
            avisoNotificacionesEvaluado = true
            when {
                debeMostrarAvisoNotificaciones(contexto) -> {
                    mostrarAvisoNotificaciones = true
                }

                lasNotificacionesYaPuedenUsarse(contexto) -> {
                    alSolicitarPermisoNotificaciones()
                }
            }
        }
    }

    if (mostrarAvisoNotificaciones) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(text = "Notificaciones de GestCar")
            },
            text = {
                Text(
                    text = "GestCar usa notificaciones para avisarte de recordatorios, vencimientos de gastos y tareas pendientes de tus vehículos."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        marcarAvisoNotificacionesMostrado(contexto)
                        mostrarAvisoNotificaciones = false
                        alSolicitarPermisoNotificaciones()
                    }
                ) {
                    Text(text = "Entendido")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = if (modoOrganizacion) "Organizar veh\u00EDculos" else "Mis veh\u00EDculos",
                acciones = {
                    if (modoOrganizacion) {
                        IconButton(
                            onClick = {
                                modoOrganizacion = false
                                vehiculosOrganizacion = emptyList()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.guardarOrganizacion(vehiculosOrganizacion)
                                modoOrganizacion = false
                                vehiculosOrganizacion = emptyList()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Guardar organizaci\u00F3n",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!modoOrganizacion) {
                FloatingActionButton(
                    onClick = alAnadirVehiculo,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "A\u00F1adir veh\u00EDculo")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                estado.estaCargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                estado.vehiculos.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No tienes veh\u00EDculos registrados",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Pulsa + para a\u00F1adir el primero",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                else -> {
                    // en modo organizacion trabajamos con una copia temporal
                    // si el usuario cancela no tocamos los datos reales de room
                    val vehiculosVisibles = if (modoOrganizacion) vehiculosOrganizacion else estado.vehiculos
                    val habituales = vehiculosVisibles.filter { it.habitual }
                    val otros = vehiculosVisibles.filter { !it.habitual }
                    val mostrarSecciones = modoOrganizacion || otros.isNotEmpty()

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (mostrarSecciones && habituales.isNotEmpty()) {
                            item {
                                TituloSeccionVehiculos(
                                    titulo = if (habituales.size == 1) {
                                        "Veh\u00EDculo habitual"
                                    } else {
                                        "Veh\u00EDculos habituales"
                                    }
                                )
                            }
                        }

                        itemsIndexed(habituales, key = { _, vehiculo -> vehiculo.id }) { indice, vehiculo ->
                            TarjetaVehiculoOrganizable(
                                vehiculo = vehiculo,
                                vehiculos = vehiculosVisibles,
                                indiceColor = indice,
                                modoOrganizacion = modoOrganizacion,
                                alPulsar = { if (!modoOrganizacion) alPulsarVehiculo(vehiculo.id) },
                                alPulsacionLarga = {
                                    vehiculosOrganizacion = estado.vehiculos
                                    modoOrganizacion = true
                                },
                                alMover = { direccion ->
                                    vehiculosOrganizacion = moverVehiculoOrganizacion(
                                        vehiculosOrganizacion,
                                        vehiculo.id,
                                        direccion
                                    )
                                }
                            )
                        }

                        if (mostrarSecciones) {
                            item {
                                TituloSeccionVehiculos(titulo = "Otros veh\u00EDculos")
                            }
                        }

                        if (modoOrganizacion && otros.isEmpty()) {
                            item {
                                ZonaVaciaOtrosVehiculos()
                            }
                        }

                        itemsIndexed(otros, key = { _, vehiculo -> vehiculo.id }) { indice, vehiculo ->
                            TarjetaVehiculoOrganizable(
                                vehiculo = vehiculo,
                                vehiculos = vehiculosVisibles,
                                indiceColor = indice,
                                modoOrganizacion = modoOrganizacion,
                                alPulsar = { if (!modoOrganizacion) alPulsarVehiculo(vehiculo.id) },
                                alPulsacionLarga = {
                                    vehiculosOrganizacion = estado.vehiculos
                                    modoOrganizacion = true
                                },
                                alMover = { direccion ->
                                    vehiculosOrganizacion = moverVehiculoOrganizacion(
                                        vehiculosOrganizacion,
                                        vehiculo.id,
                                        direccion
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun debeMostrarAvisoNotificaciones(contexto: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return false
    }

    if (lasNotificacionesYaPuedenUsarse(contexto)) {
        return false
    }

    return !contexto
        .getSharedPreferences(PREFERENCIAS_NOTIFICACIONES, Context.MODE_PRIVATE)
        .getBoolean(CLAVE_AVISO_NOTIFICACIONES_MOSTRADO, false)
}

private fun lasNotificacionesYaPuedenUsarse(contexto: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return true
    }

    return ContextCompat.checkSelfPermission(
        contexto,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun marcarAvisoNotificacionesMostrado(contexto: Context) {
    contexto
        .getSharedPreferences(PREFERENCIAS_NOTIFICACIONES, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(CLAVE_AVISO_NOTIFICACIONES_MOSTRADO, true)
        .apply()
}

@Composable
private fun ZonaVaciaOtrosVehiculos() {
    // aunque no haya secundarios mantenemos una zona visible
    // asi el usuario entiende que puede mover vehiculos a esa seccion
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Sin veh\u00EDculos secundarios",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun TituloSeccionVehiculos(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun TarjetaVehiculoOrganizable(
    vehiculo: Vehiculo,
    vehiculos: List<Vehiculo>,
    indiceColor: Int,
    modoOrganizacion: Boolean,
    alPulsar: () -> Unit,
    alPulsacionLarga: () -> Unit,
    alMover: (Int) -> Unit
) {
    val indice = vehiculos.indexOfFirst { it.id == vehiculo.id }
    val cantidadHabituales = vehiculos.count { it.habitual }
    // solo permitimos bajar el ultimo habitual a secundarios
    // asi no queda un hueco raro en mitad del grupo de habituales
    val puedeBajarAOtros = modoOrganizacion &&
        vehiculo.habitual &&
        cantidadHabituales > 1 &&
        indice == cantidadHabituales - 1
    TarjetaVehiculo(
        vehiculo = vehiculo,
        alPulsar = alPulsar,
        indiceColor = indiceColor,
        alPulsacionLarga = alPulsacionLarga,
        modoOrganizacion = modoOrganizacion,
        alMoverArriba = if (indice > 0) {
            { alMover(-1) }
        } else {
            null
        },
        alMoverAbajo = if (indice in 0 until vehiculos.lastIndex || puedeBajarAOtros) {
            { alMover(1) }
        } else {
            null
        }
    )
}

private fun moverVehiculoOrganizacion(
    vehiculos: List<Vehiculo>,
    vehiculoId: String,
    direccion: Int
): List<Vehiculo> {
    if (vehiculos.size <= 1) {
        return vehiculos
    }

    val lista = vehiculos.toMutableList()
    val indice = lista.indexOfFirst { it.id == vehiculoId }
    if (indice == -1) {
        return vehiculos
    }

    val cantidadHabituales = lista.count { it.habitual }.coerceAtLeast(1)
    val esUltimoHabitual = indice == cantidadHabituales - 1
    val esPrimerSecundario = indice == cantidadHabituales

    // bajar el ultimo habitual lo convierte en secundario
    // mantenemos al menos un habitual para que los selectores tengan vehiculo por defecto
    if (direccion > 0 && esUltimoHabitual && cantidadHabituales > 1) {
        return lista.mapIndexed { nuevoIndice, vehiculo ->
            vehiculo.copy(
                habitual = if (vehiculo.id == vehiculoId) {
                    false
                } else {
                    nuevoIndice < cantidadHabituales - 1
                },
                ordenLista = nuevoIndice
            )
        }.ordenarParaMostrar()
    }

    // subir el primer secundario lo convierte en habitual
    // esto permite crear varios habituales ordenados por prioridad
    if (direccion < 0 && esPrimerSecundario) {
        return lista.mapIndexed { nuevoIndice, vehiculo ->
            vehiculo.copy(
                habitual = if (vehiculo.id == vehiculoId) {
                    true
                } else {
                    nuevoIndice < cantidadHabituales
                },
                ordenLista = nuevoIndice
            )
        }.ordenarParaMostrar()
    }

    val destino = (indice + direccion).coerceIn(0, lista.lastIndex)
    if (indice == destino) {
        return vehiculos
    }

    val vehiculoMovido = lista.removeAt(indice)
    lista.add(destino, vehiculoMovido)

    // despues de mover recalculamos flags y orden
    // es mas seguro que intentar parchear solo dos elementos
    return lista.mapIndexed { nuevoIndice, vehiculo ->
        vehiculo.copy(
            habitual = nuevoIndice < cantidadHabituales,
            ordenLista = nuevoIndice
        )
    }
}

private fun List<Vehiculo>.ordenarParaMostrar(): List<Vehiculo> {
    // habituales siempre arriba, secundarios debajo
    // dentro de cada bloque manda el orden elegido por el usuario
    return sortedWith(compareByDescending<Vehiculo> { it.habitual }.thenBy { it.ordenLista })
        .mapIndexed { indice, vehiculo -> vehiculo.copy(ordenLista = indice) }
}
