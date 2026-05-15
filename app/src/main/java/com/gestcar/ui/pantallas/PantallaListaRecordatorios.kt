package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.DialogoFiltroPeriodo
import com.gestcar.ui.componentes.DialogoRangoPeriodo
import com.gestcar.ui.componentes.EstadoVisualRecordatorio
import com.gestcar.ui.componentes.FilaSelectorVehiculoConFiltro
import com.gestcar.ui.componentes.TarjetaRecordatorio
import com.gestcar.ui.componentes.fechaDentroDePeriodo
import com.gestcar.ui.componentes.hayFiltroPeriodoActivo
import com.gestcar.ui.viewmodel.PeriodoRepostajes
import com.gestcar.ui.viewmodel.RecordatorioViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaRecordatorios(
    usuarioId: String,
    vehiculoInicialId: String? = null,
    alCrearRecordatorio: (String) -> Unit,
    alVerDetalleRecordatorio: (String, String) -> Unit,
    alVolver: () -> Unit,
    recordatorioViewModel: RecordatorioViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoRecordatorios by recordatorioViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo
    var mostrarCompletados by rememberSaveable { mutableStateOf(false) }
    var mostrarFiltroPeriodo by rememberSaveable { mutableStateOf(false) }
    var mostrarRangoPersonalizado by rememberSaveable { mutableStateOf(false) }
    var periodoSeleccionado by rememberSaveable { mutableStateOf(PeriodoRepostajes.TODO) }
    var fechaInicioPersonalizada by rememberSaveable { mutableStateOf<Long?>(null) }
    var fechaFinPersonalizada by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(usuarioId, vehiculoInicialId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId, vehiculoInicialId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { recordatorioViewModel.cargarRecordatorios(it.id) }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Recordatorios",
                alVolver = alVolver
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
            FilaSelectorVehiculoConFiltro(
                vehiculos = estadoVehiculo.vehiculos,
                vehiculoActivo = vehiculoActivo,
                alSeleccionarVehiculo = { vehiculoActivoViewModel.seleccionarVehiculo(it) },
                alPulsarFiltro = { mostrarFiltroPeriodo = true },
                filtroActivo = hayFiltroPeriodoActivo(periodoSeleccionado)
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
                    val recordatoriosDelPeriodo = estadoRecordatorios.recordatorios
                        .filter {
                            fechaDentroDePeriodo(
                                fecha = fechaReferenciaFiltroRecordatorio(it),
                                periodo = periodoSeleccionado,
                                fechaInicioPersonalizada = fechaInicioPersonalizada,
                                fechaFinPersonalizada = fechaFinPersonalizada
                            )
                        }

                    val recordatoriosPendientes = recordatoriosDelPeriodo
                        .filter { !it.completado }
                        .sortedWith(compareBy({ prioridadEstado(it, vehiculoActivo) }, { it.fechaLimite ?: Long.MAX_VALUE }))

                    val recordatoriosCompletados = recordatoriosDelPeriodo
                        .filter { it.completado }
                        .sortedByDescending { it.fechaCompletado ?: it.fechaLimite ?: 0L }

                    androidx.compose.foundation.lazy.LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (recordatoriosDelPeriodo.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay recordatorios en este periodo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (recordatoriosPendientes.isNotEmpty()) {
                            item { EncabezadoSeccionRecordatorios("Pendientes") }

                            itemsIndexed(recordatoriosPendientes) { indice, recordatorio ->
                                TarjetaRecordatorio(
                                    recordatorio = recordatorio,
                                    estadoVisual = calcularEstadoVisual(recordatorio, vehiculoActivo),
                                    indiceColor = indice,
                                    alPulsar = {
                                        alVerDetalleRecordatorio(recordatorio.vehiculoId, recordatorio.id)
                                    }
                                )
                            }
                        }

                        if (recordatoriosCompletados.isNotEmpty()) {
                            item {
                                if (recordatoriosPendientes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                EncabezadoSeccionRecordatorios(
                                    titulo = "Completados (${recordatoriosCompletados.size})",
                                    estaDesplegada = mostrarCompletados,
                                    alPulsar = { mostrarCompletados = !mostrarCompletados }
                                )
                            }

                            if (mostrarCompletados) {
                                itemsIndexed(recordatoriosCompletados) { indice, recordatorio ->
                                    TarjetaRecordatorio(
                                        recordatorio = recordatorio,
                                        estadoVisual = calcularEstadoVisual(recordatorio, vehiculoActivo),
                                        indiceColor = indice,
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
    }

    if (mostrarFiltroPeriodo) {
        DialogoFiltroPeriodo(
            titulo = "Filtrar recordatorios",
            periodoSeleccionado = periodoSeleccionado,
            alSeleccionarPeriodo = { periodo ->
                mostrarFiltroPeriodo = false
                if (periodo == PeriodoRepostajes.PERSONALIZADO) {
                    mostrarRangoPersonalizado = true
                } else {
                    periodoSeleccionado = periodo
                }
            },
            alCancelar = { mostrarFiltroPeriodo = false }
        )
    }

    if (mostrarRangoPersonalizado) {
        DialogoRangoPeriodo(
            fechaInicioInicial = fechaInicioPersonalizada ?: System.currentTimeMillis(),
            fechaFinInicial = fechaFinPersonalizada ?: System.currentTimeMillis(),
            alConfirmar = { inicio, fin ->
                fechaInicioPersonalizada = inicio
                fechaFinPersonalizada = fin
                periodoSeleccionado = PeriodoRepostajes.PERSONALIZADO
                mostrarRangoPersonalizado = false
            },
            alCancelar = { mostrarRangoPersonalizado = false }
        )
    }
}

@Composable
private fun EncabezadoSeccionRecordatorios(
    titulo: String,
    estaDesplegada: Boolean? = null,
    alPulsar: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleMedium
        )
        estaDesplegada?.let {
            IconButton(
                onClick = { alPulsar?.invoke() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (it) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (it) "Ocultar completados" else "Mostrar completados",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

private fun fechaReferenciaFiltroRecordatorio(recordatorio: Recordatorio): Long {
    return recordatorio.fechaLimite
        ?: recordatorio.fechaCompletado
        ?: recordatorio.actualizadoEn
}
