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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.componentes.FilaSelectorVehiculoConFiltro
import com.gestcar.ui.componentes.TarjetaRepostaje
import com.gestcar.ui.viewmodel.PeriodoRepostajes
import com.gestcar.ui.viewmodel.RepostajeViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaRepostajes(
    usuarioId: String,
    vehiculoInicialId: String? = null,
    alCrearRepostaje: (String) -> Unit,
    alVerDetalleRepostaje: (String, String) -> Unit,
    repostajeViewModel: RepostajeViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoRepostajes by repostajeViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo
    var mostrarFiltroPeriodo by remember { mutableStateOf(false) }
    var mostrarRangoPersonalizado by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId, vehiculoInicialId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId, vehiculoInicialId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { repostajeViewModel.cargarRepostajes(it.id) }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(titulo = "Repostajes")
        },
        floatingActionButton = {
            if (vehiculoActivo != null) {
                FloatingActionButton(
                    onClick = { alCrearRepostaje(vehiculoActivo.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir repostaje")
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
                filtroActivo = estadoRepostajes.periodoSeleccionado != PeriodoRepostajes.TODO
            )

            when {
                estadoVehiculo.estaCargando || estadoRepostajes.estaCargando -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                vehiculoActivo == null -> {
                    EstadoVacioRepostajes(
                        titulo = "No hay vehículos",
                        mensaje = "Añade un vehículo antes de registrar repostajes"
                    )
                }

                estadoRepostajes.repostajes.isEmpty() -> {
                    EstadoVacioRepostajes(
                        titulo = "No hay repostajes",
                        mensaje = "Pulsa + para añadir el primero"
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            ResumenConsumoRepostajes(
                                consumoMedio = estadoRepostajes.consumoMedio,
                                costeMedioCada100Km = estadoRepostajes.costeMedioCada100Km
                            )
                        }

                        if (estadoRepostajes.repostajesFiltrados.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay repostajes en este periodo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }

                        items(estadoRepostajes.repostajesFiltrados) { repostaje ->
                            TarjetaRepostaje(
                                repostaje = repostaje,
                                alPulsar = { alVerDetalleRepostaje(repostaje.vehiculoId, repostaje.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarFiltroPeriodo) {
        DialogoFiltroPeriodoRepostajes(
            periodoSeleccionado = estadoRepostajes.periodoSeleccionado,
            alSeleccionarPeriodo = { periodo ->
                mostrarFiltroPeriodo = false
                if (periodo == PeriodoRepostajes.PERSONALIZADO) {
                    mostrarRangoPersonalizado = true
                } else {
                    repostajeViewModel.seleccionarPeriodo(periodo)
                }
            },
            alCancelar = { mostrarFiltroPeriodo = false }
        )
    }

    if (mostrarRangoPersonalizado) {
        DialogoRangoRepostajes(
            fechaInicioInicial = estadoRepostajes.fechaInicioPersonalizada ?: System.currentTimeMillis(),
            fechaFinInicial = estadoRepostajes.fechaFinPersonalizada ?: System.currentTimeMillis(),
            alConfirmar = { inicio, fin ->
                mostrarRangoPersonalizado = false
                repostajeViewModel.seleccionarRangoPersonalizado(inicio, fin)
            },
            alCancelar = { mostrarRangoPersonalizado = false }
        )
    }
}

@Composable
private fun DialogoFiltroPeriodoRepostajes(
    periodoSeleccionado: PeriodoRepostajes,
    alSeleccionarPeriodo: (PeriodoRepostajes) -> Unit,
    alCancelar: () -> Unit
) {
    val opciones = listOf(
        PeriodoRepostajes.HOY to "Hoy",
        PeriodoRepostajes.SEMANA to "Semana",
        PeriodoRepostajes.MES to "Mes",
        PeriodoRepostajes.ANIO to "A\u00F1o",
        PeriodoRepostajes.TODO to "Todo",
        PeriodoRepostajes.PERSONALIZADO to "Personalizado"
    )

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Filtrar repostajes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opciones.forEach { (periodo, etiqueta) ->
                    FilterChip(
                        selected = periodoSeleccionado == periodo,
                        onClick = { alSeleccionarPeriodo(periodo) },
                        label = { Text(etiqueta) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun ResumenConsumoRepostajes(
    consumoMedio: Double,
    costeMedioCada100Km: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TarjetaDatoConsumo(
            titulo = "Consumo medio",
            valor = if (consumoMedio > 0) "${formatearDecimalRepostajes(consumoMedio)} L/100 km" else "Datos insuficientes",
            modifier = Modifier.weight(1f)
        )
        TarjetaDatoConsumo(
            titulo = "Coste medio",
            valor = if (costeMedioCada100Km > 0) "${formatearDecimalRepostajes(costeMedioCada100Km)} \u20AC/100 km" else "Datos insuficientes",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaDatoConsumo(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
private fun DialogoRangoRepostajes(
    fechaInicioInicial: Long,
    fechaFinInicial: Long,
    alConfirmar: (Long, Long) -> Unit,
    alCancelar: () -> Unit
) {
    var fechaInicio by remember { mutableStateOf(fechaInicioInicial) }
    var fechaFin by remember { mutableStateOf(fechaFinInicial) }

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Rango personalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoFecha(
                    etiqueta = "Desde",
                    fecha = fechaInicio,
                    alSeleccionarFecha = { fechaInicio = it }
                )
                CampoFecha(
                    etiqueta = "Hasta",
                    fecha = fechaFin,
                    alSeleccionarFecha = { fechaFin = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(fechaInicio, fechaFin) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EstadoVacioRepostajes(
    titulo: String,
    mensaje: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocalGasStation,
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

private fun formatearDecimalRepostajes(valor: Double): String {
    return String.format(Locale("es", "ES"), "%.2f", valor)
}
