package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.SelectorVehiculoActivo
import com.gestcar.ui.componentes.TarjetaMantenimiento
import com.gestcar.ui.viewmodel.CATEGORIA_MANTENIMIENTO
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION
import com.gestcar.ui.viewmodel.FILTRO_TODOS_MANTENIMIENTOS
import com.gestcar.ui.viewmodel.MantenimientoViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaMantenimientos(
    usuarioId: String,
    vehiculoInicialId: String? = null,
    alCrearMantenimiento: (String) -> Unit,
    alVerDetalleMantenimiento: (String, String) -> Unit,
    mantenimientoViewModel: MantenimientoViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoMantenimientos by mantenimientoViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo
    var mostrarRealizadas by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId, vehiculoInicialId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId, vehiculoInicialId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { mantenimientoViewModel.cargarMantenimientos(it.id) }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(titulo = "Mantenimiento")
        },
        floatingActionButton = {
            if (vehiculoActivo != null) {
                FloatingActionButton(
                    onClick = { alCrearMantenimiento(vehiculoActivo.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir mantenimiento")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SelectorVehiculoActivo(
                vehiculos = estadoVehiculo.vehiculos,
                vehiculoActivo = vehiculoActivo,
                alSeleccionar = { vehiculoActivoViewModel.seleccionarVehiculo(it) },
                modifier = Modifier.padding(16.dp)
            )

            FiltrosMantenimiento(
                filtroActual = estadoMantenimientos.filtroCategoria,
                alCambiarFiltro = { mantenimientoViewModel.cambiarFiltroCategoria(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            when {
                estadoVehiculo.estaCargando || estadoMantenimientos.estaCargando -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                vehiculoActivo == null -> {
                    EstadoVacioMantenimientos(
                        titulo = "No hay vehículos",
                        mensaje = "Añade un vehículo antes de registrar operaciones"
                    )
                }

                estadoMantenimientos.mantenimientosFiltrados.isEmpty() -> {
                    EstadoVacioMantenimientos(
                        titulo = "No hay operaciones",
                        mensaje = "Pulsa + para añadir la primera"
                    )
                }

                else -> {
                    val pendientes = estadoMantenimientos.mantenimientosPendientes
                    val realizadas = estadoMantenimientos.mantenimientosRealizados

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (pendientes.isNotEmpty()) {
                            item { EncabezadoSeccionMantenimientos("Pendientes") }
                        }

                        items(pendientes) { mantenimiento ->
                            TarjetaMantenimiento(
                                mantenimiento = mantenimiento,
                                alPulsar = { alVerDetalleMantenimiento(mantenimiento.vehiculoId, mantenimiento.id) }
                            )
                        }

                        if (realizadas.isNotEmpty()) {
                            item {
                                EncabezadoSeccionMantenimientos(
                                    titulo = "Realizadas (${realizadas.size})",
                                    estaDesplegada = mostrarRealizadas,
                                    alPulsar = { mostrarRealizadas = !mostrarRealizadas }
                                )
                            }

                            if (mostrarRealizadas) {
                                items(realizadas) { mantenimiento ->
                                    TarjetaMantenimiento(
                                        mantenimiento = mantenimiento,
                                        alPulsar = { alVerDetalleMantenimiento(mantenimiento.vehiculoId, mantenimiento.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EncabezadoSeccionMantenimientos(
    titulo: String,
    estaDesplegada: Boolean? = null,
    alPulsar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (alPulsar != null) Modifier.clickable { alPulsar() } else Modifier)
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium
        )
        estaDesplegada?.let {
            Text(
                text = if (it) "v" else ">",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FiltrosMantenimiento(
    filtroActual: String,
    alCambiarFiltro: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filtroActual == FILTRO_TODOS_MANTENIMIENTOS,
            onClick = { alCambiarFiltro(FILTRO_TODOS_MANTENIMIENTOS) },
            label = { Text("Todos") }
        )
        FilterChip(
            selected = filtroActual == CATEGORIA_MANTENIMIENTO,
            onClick = { alCambiarFiltro(CATEGORIA_MANTENIMIENTO) },
            label = { Text("Mantenimiento") }
        )
        FilterChip(
            selected = filtroActual == CATEGORIA_REPARACION,
            onClick = { alCambiarFiltro(CATEGORIA_REPARACION) },
            label = { Text("Reparación") }
        )
    }
}

@Composable
private fun EstadoVacioMantenimientos(
    titulo: String,
    mensaje: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.size(16.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
