package com.gestcar.ui.pantallas

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.TarjetaVehiculo
import com.gestcar.ui.viewmodel.VehiculoViewModel

// pantalla principal de la app, muestra la lista de vehiculos del usuario
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaVehiculos(
    usuarioId: String,
    alPulsarVehiculo: (String) -> Unit,
    alAnadirVehiculo: () -> Unit,
    viewModel: VehiculoViewModel = viewModel()
) {
    val estado by viewModel.estadoLista.collectAsState()
    var modoOrganizacion by remember { mutableStateOf(false) }
    var vehiculosOrganizacion by remember { mutableStateOf(emptyList<Vehiculo>()) }

    LaunchedEffect(usuarioId) {
        viewModel.cargarVehiculos(usuarioId)
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

                        items(habituales, key = { it.id }) { vehiculo ->
                            TarjetaVehiculoOrganizable(
                                vehiculo = vehiculo,
                                vehiculos = vehiculosVisibles,
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

                        items(otros, key = { it.id }) { vehiculo ->
                            TarjetaVehiculoOrganizable(
                                vehiculo = vehiculo,
                                vehiculos = vehiculosVisibles,
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

@Composable
private fun ZonaVaciaOtrosVehiculos() {
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
    modoOrganizacion: Boolean,
    alPulsar: () -> Unit,
    alPulsacionLarga: () -> Unit,
    alMover: (Int) -> Unit
) {
    val indice = vehiculos.indexOfFirst { it.id == vehiculo.id }
    val cantidadHabituales = vehiculos.count { it.habitual }
    val puedeBajarAOtros = modoOrganizacion &&
        vehiculo.habitual &&
        cantidadHabituales > 1 &&
        indice == cantidadHabituales - 1
    TarjetaVehiculo(
        vehiculo = vehiculo,
        alPulsar = alPulsar,
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

    return lista.mapIndexed { nuevoIndice, vehiculo ->
        vehiculo.copy(
            habitual = nuevoIndice < cantidadHabituales,
            ordenLista = nuevoIndice
        )
    }
}

private fun List<Vehiculo>.ordenarParaMostrar(): List<Vehiculo> {
    return sortedWith(compareByDescending<Vehiculo> { it.habitual }.thenBy { it.ordenLista })
        .mapIndexed { indice, vehiculo -> vehiculo.copy(ordenLista = indice) }
}
