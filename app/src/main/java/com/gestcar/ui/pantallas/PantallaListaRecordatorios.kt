package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.EstadoVisualRecordatorio
import com.gestcar.ui.componentes.SelectorVehiculoActivo
import com.gestcar.ui.componentes.TarjetaRecordatorio
import com.gestcar.ui.viewmodel.RecordatorioViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaRecordatorios(
    usuarioId: String,
    alCrearRecordatorio: (String) -> Unit,
    alVerDetalleRecordatorio: (String, String) -> Unit,
    alVolver: () -> Unit,
    recordatorioViewModel: RecordatorioViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoRecordatorios by recordatorioViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo

    LaunchedEffect(usuarioId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { recordatorioViewModel.cargarRecordatorios(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recordatorios") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (vehiculoActivo != null) {
                FloatingActionButton(
                    onClick = { alCrearRecordatorio(vehiculoActivo.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir recordatorio")
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

            when {
                estadoVehiculo.estaCargando || estadoRecordatorios.estaCargando -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                vehiculoActivo == null -> {
                    EstadoVacioRecordatorios(
                        titulo = "No hay vehículos",
                        mensaje = "Añade un vehículo antes de crear recordatorios"
                    )
                }

                estadoRecordatorios.recordatorios.isEmpty() -> {
                    EstadoVacioRecordatorios(
                        titulo = "No hay recordatorios",
                        mensaje = "Pulsa + para añadir el primero"
                    )
                }

                else -> {
                    val recordatoriosOrdenados = estadoRecordatorios.recordatorios
                        .sortedWith(compareBy({ prioridadEstado(it, vehiculoActivo) }, { it.fechaLimite ?: Long.MAX_VALUE }))

                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recordatoriosOrdenados) { recordatorio ->
                            TarjetaRecordatorio(
                                recordatorio = recordatorio,
                                estadoVisual = calcularEstadoVisual(recordatorio, vehiculoActivo),
                                alPulsar = {
                                    alVerDetalleRecordatorio(recordatorio.vehiculoId, recordatorio.id)
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
private fun EstadoVacioRecordatorios(
    titulo: String,
    mensaje: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
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

fun calcularEstadoVisual(
    recordatorio: Recordatorio,
    vehiculo: Vehiculo?
): EstadoVisualRecordatorio {
    if (recordatorio.completado) {
        return EstadoVisualRecordatorio.COMPLETADO
    }

    val ahora = System.currentTimeMillis()
    val treintaDias = 30L * 24L * 60L * 60L * 1000L
    val fechaVencida = recordatorio.fechaLimite?.let { it < ahora } == true
    val fechaProxima = recordatorio.fechaLimite?.let { it <= ahora + treintaDias } == true
    val kilometrajeProximo = recordatorio.kilometrajeLimite?.let { limite ->
        vehiculo?.let { limite <= it.kilometraje + 1000 }
    } == true
    val kilometrajeVencido = recordatorio.kilometrajeLimite?.let { limite ->
        vehiculo?.let { limite <= it.kilometraje }
    } == true

    return when {
        fechaVencida || kilometrajeVencido -> EstadoVisualRecordatorio.VENCIDO
        fechaProxima || kilometrajeProximo -> EstadoVisualRecordatorio.PROXIMO
        else -> EstadoVisualRecordatorio.PENDIENTE
    }
}

private fun prioridadEstado(recordatorio: Recordatorio, vehiculo: Vehiculo?): Int {
    return when (calcularEstadoVisual(recordatorio, vehiculo)) {
        EstadoVisualRecordatorio.VENCIDO -> 0
        EstadoVisualRecordatorio.PROXIMO -> 1
        EstadoVisualRecordatorio.PENDIENTE -> 2
        EstadoVisualRecordatorio.COMPLETADO -> 3
    }
}
