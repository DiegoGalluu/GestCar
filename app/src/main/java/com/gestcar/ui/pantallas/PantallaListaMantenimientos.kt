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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.DialogoFiltroPeriodo
import com.gestcar.ui.componentes.DialogoRangoPeriodo
import com.gestcar.ui.componentes.FilaSelectorVehiculoConFiltro
import com.gestcar.ui.componentes.TarjetaMantenimiento
import com.gestcar.ui.componentes.fechaDentroDePeriodo
import com.gestcar.ui.componentes.hayFiltroPeriodoActivo
import com.gestcar.ui.viewmodel.CATEGORIA_MANTENIMIENTO
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION
import com.gestcar.ui.viewmodel.FILTRO_TODOS_MANTENIMIENTOS
import com.gestcar.ui.viewmodel.MantenimientoViewModel
import com.gestcar.ui.viewmodel.PeriodoRepostajes
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel
import java.util.Locale

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
    var mostrarFiltroPeriodo by remember { mutableStateOf(false) }
    var mostrarRangoPersonalizado by remember { mutableStateOf(false) }
    var periodoSeleccionado by remember { mutableStateOf(PeriodoRepostajes.TODO) }
    var fechaInicioPersonalizada by remember { mutableStateOf<Long?>(null) }
    var fechaFinPersonalizada by remember { mutableStateOf<Long?>(null) }
    val colorCategoriaActual = colorFiltroMantenimiento(estadoMantenimientos.filtroCategoria)

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
            FilaSelectorVehiculoConFiltro(
                vehiculos = estadoVehiculo.vehiculos,
                vehiculoActivo = vehiculoActivo,
                alSeleccionarVehiculo = { vehiculoActivoViewModel.seleccionarVehiculo(it) },
                alPulsarFiltro = { mostrarFiltroPeriodo = true },
                filtroActivo = hayFiltroPeriodoActivo(periodoSeleccionado)
            )

            FiltrosMantenimiento(
                filtroActual = estadoMantenimientos.filtroCategoria,
                alCambiarFiltro = { mantenimientoViewModel.cambiarFiltroCategoria(it) },
                colorActual = colorCategoriaActual,
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
                    val mantenimientosDelPeriodo = estadoMantenimientos.mantenimientosFiltrados
                        .filter {
                            fechaDentroDePeriodo(
                                fecha = it.fecha,
                                periodo = periodoSeleccionado,
                                fechaInicioPersonalizada = fechaInicioPersonalizada,
                                fechaFinPersonalizada = fechaFinPersonalizada
                            )
                        }
                    val pendientes = mantenimientosDelPeriodo.filter { !it.realizado }
                    val realizadas = mantenimientosDelPeriodo.filter { it.realizado }
                    val costeTotalPeriodo = mantenimientosDelPeriodo.sumOf { it.coste }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            ResumenMantenimientos(
                                filtroActual = estadoMantenimientos.filtroCategoria,
                                costeTotal = costeTotalPeriodo,
                                numeroOperaciones = mantenimientosDelPeriodo.size,
                                colorCategoria = colorCategoriaActual
                            )
                        }

                        if (mantenimientosDelPeriodo.isEmpty()) {
                            item {
                                Text(
                                    text = "No hay operaciones en este periodo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (pendientes.isNotEmpty()) {
                            item { EncabezadoSeccionMantenimientos("Pendientes") }
                        }

                        itemsIndexed(pendientes) { indice, mantenimiento ->
                            TarjetaMantenimiento(
                                mantenimiento = mantenimiento,
                                indiceColor = indice,
                                colorCategoria = colorMantenimiento(mantenimiento.categoria),
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
                                itemsIndexed(realizadas) { indice, mantenimiento ->
                                    TarjetaMantenimiento(
                                        mantenimiento = mantenimiento,
                                        indiceColor = indice,
                                        colorCategoria = colorMantenimiento(mantenimiento.categoria),
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

    if (mostrarFiltroPeriodo) {
        DialogoFiltroPeriodo(
            titulo = "Filtrar mantenimiento",
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
    colorActual: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filtroActual == FILTRO_TODOS_MANTENIMIENTOS,
            onClick = { alCambiarFiltro(FILTRO_TODOS_MANTENIMIENTOS) },
            label = { Text("Todos") },
            colors = coloresChipMantenimiento(colorActual)
        )
        FilterChip(
            selected = filtroActual == CATEGORIA_MANTENIMIENTO,
            onClick = { alCambiarFiltro(CATEGORIA_MANTENIMIENTO) },
            label = { Text("Mantenimiento") },
            colors = coloresChipMantenimiento(colorMantenimiento(CATEGORIA_MANTENIMIENTO))
        )
        FilterChip(
            selected = filtroActual == CATEGORIA_REPARACION,
            onClick = { alCambiarFiltro(CATEGORIA_REPARACION) },
            label = { Text("Reparación") },
            colors = coloresChipMantenimiento(colorMantenimiento(CATEGORIA_REPARACION))
        )
    }
}

@Composable
private fun ResumenMantenimientos(
    filtroActual: String,
    costeTotal: Double,
    numeroOperaciones: Int,
    colorCategoria: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TarjetaDatoMantenimiento(
            titulo = tituloResumenCosteMantenimiento(filtroActual),
            valor = "${formatearImporteMantenimiento(costeTotal)} €",
            colorCategoria = colorCategoria,
            modifier = Modifier.weight(1f)
        )
        TarjetaDatoMantenimiento(
            titulo = "Operaciones",
            valor = numeroOperaciones.toString(),
            colorCategoria = colorCategoria,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaDatoMantenimiento(
    titulo: String,
    valor: String,
    colorCategoria: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = colorCategoria.copy(alpha = 0.16f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleLarge,
                color = colorCategoria
            )
        }
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

@Composable
private fun colorFiltroMantenimiento(filtroActual: String): Color {
    return when (filtroActual) {
        CATEGORIA_MANTENIMIENTO -> colorMantenimiento(CATEGORIA_MANTENIMIENTO)
        CATEGORIA_REPARACION -> colorMantenimiento(CATEGORIA_REPARACION)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun colorMantenimiento(categoria: String): Color {
    return when (categoria) {
        CATEGORIA_MANTENIMIENTO -> Color(0xFF7B61FF)
        CATEGORIA_REPARACION -> Color(0xFF008C95)
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun coloresChipMantenimiento(color: Color) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = color.copy(alpha = 0.18f),
    selectedLabelColor = color,
    selectedLeadingIconColor = color,
    containerColor = MaterialTheme.colorScheme.surface,
    labelColor = MaterialTheme.colorScheme.onSurface
)

private fun tituloResumenCosteMantenimiento(filtroActual: String): String {
    return when (filtroActual) {
        CATEGORIA_MANTENIMIENTO -> "Total mantenimiento"
        CATEGORIA_REPARACION -> "Total reparaciones"
        else -> "Total operaciones"
    }
}

private fun formatearImporteMantenimiento(valor: Double): String {
    return String.format(Locale("es", "ES"), "%.2f", valor)
}
