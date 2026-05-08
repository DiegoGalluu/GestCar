package com.gestcar.ui.pantallas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.DialogoFiltroPeriodo
import com.gestcar.ui.componentes.DialogoRangoPeriodo
import com.gestcar.ui.componentes.FilaSelectorVehiculoConFiltro
import com.gestcar.ui.componentes.hayFiltroPeriodoActivo
import com.gestcar.ui.viewmodel.CategoriaEstadistica
import com.gestcar.ui.viewmodel.EstadisticasViewModel
import com.gestcar.ui.viewmodel.GastoMensualEstadistica
import com.gestcar.ui.viewmodel.PeriodoRepostajes
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel
import java.util.Locale

@Composable
fun PantallaEstadisticas(
    usuarioId: String,
    alVolver: () -> Unit,
    estadisticasViewModel: EstadisticasViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoEstadisticas by estadisticasViewModel.estado.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo
    var mostrarFiltroPeriodo by rememberSaveable { mutableStateOf(false) }
    var mostrarRangoPersonalizado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(usuarioId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId)
    }

    LaunchedEffect(vehiculoActivo?.id, vehiculoActivo?.kilometraje) {
        vehiculoActivo?.let {
            estadisticasViewModel.cargarEstadisticas(it.id, it.kilometraje)
        }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Estadisticas",
                alVolver = alVolver
            )
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
                filtroActivo = hayFiltroPeriodoActivo(estadoEstadisticas.periodoSeleccionado)
            )

            when {
                estadoVehiculo.estaCargando || estadoEstadisticas.estaCargando -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                vehiculoActivo == null -> {
                    EstadoVacioEstadisticas("Anade un vehiculo para ver estadisticas")
                }

                estadoEstadisticas.totalOperaciones == 0 -> {
                    EstadoVacioEstadisticas("Todavia no hay datos en este periodo")
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            CuadriculaResumenEstadisticas(
                                consumoMedio = estadoEstadisticas.consumoMedio,
                                costeMedioCada100Km = estadoEstadisticas.costeMedioCada100Km,
                                costeTotal = estadoEstadisticas.costeTotal,
                                costePorKilometro = estadoEstadisticas.costePorKilometro
                            )
                        }

                        item {
                            TarjetaGraficoCategorias(estadoEstadisticas.categorias)
                        }

                        item {
                            TarjetaEvolucionMensual(estadoEstadisticas.evolucionMensual)
                        }

                        item {
                            TarjetaOperacionesRegistradas(estadoEstadisticas.totalOperaciones)
                        }
                    }
                }
            }
        }
    }

    if (mostrarFiltroPeriodo) {
        DialogoFiltroPeriodo(
            titulo = "Filtrar estadisticas",
            periodoSeleccionado = estadoEstadisticas.periodoSeleccionado,
            alSeleccionarPeriodo = { periodo ->
                mostrarFiltroPeriodo = false
                if (periodo == PeriodoRepostajes.PERSONALIZADO) {
                    mostrarRangoPersonalizado = true
                } else {
                    estadisticasViewModel.seleccionarPeriodo(periodo)
                }
            },
            alCancelar = { mostrarFiltroPeriodo = false }
        )
    }

    if (mostrarRangoPersonalizado) {
        DialogoRangoPeriodo(
            fechaInicioInicial = estadoEstadisticas.fechaInicioPersonalizada ?: System.currentTimeMillis(),
            fechaFinInicial = estadoEstadisticas.fechaFinPersonalizada ?: System.currentTimeMillis(),
            alConfirmar = { inicio, fin ->
                mostrarRangoPersonalizado = false
                estadisticasViewModel.seleccionarRangoPersonalizado(inicio, fin)
            },
            alCancelar = { mostrarRangoPersonalizado = false }
        )
    }
}

@Composable
private fun CuadriculaResumenEstadisticas(
    consumoMedio: Double,
    costeMedioCada100Km: Double,
    costeTotal: Double,
    costePorKilometro: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaDatoEstadistica(
                titulo = "Consumo medio",
                valor = if (consumoMedio > 0) "${formatearDecimal(consumoMedio)} L/100 km" else "Sin datos",
                modifier = Modifier.weight(1f)
            )
            TarjetaDatoEstadistica(
                titulo = "Coste combustible",
                valor = if (costeMedioCada100Km > 0) "${formatearDecimal(costeMedioCada100Km)} €/100 km" else "Sin datos",
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TarjetaDatoEstadistica(
                titulo = "Gasto registrado",
                valor = "${formatearDecimal(costeTotal)} €",
                modifier = Modifier.weight(1f)
            )
            TarjetaDatoEstadistica(
                titulo = "Coste por km",
                valor = if (costePorKilometro > 0) "${formatearDecimal(costePorKilometro)} €/km" else "Sin datos",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TarjetaDatoEstadistica(
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.titleSmall)
            Text(text = valor, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun TarjetaGraficoCategorias(categorias: List<CategoriaEstadistica>) {
    val categoriasConImporte = categorias.filter { it.importe > 0 }
    TarjetaSeccionEstadistica(titulo = "Desglose por categoria") {
        if (categoriasConImporte.isEmpty()) {
            Text("No hay importes para comparar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            GraficoBarrasCategorias(categoriasConImporte)
        }
    }
}

@Composable
private fun TarjetaEvolucionMensual(evolucion: List<GastoMensualEstadistica>) {
    TarjetaSeccionEstadistica(titulo = "Evolucion mensual") {
        if (evolucion.isEmpty()) {
            Text("No hay evolucion suficiente", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            GraficoLineaMensual(evolucion)
        }
    }
}

@Composable
private fun TarjetaOperacionesRegistradas(totalOperaciones: Int) {
    TarjetaSeccionEstadistica(titulo = "Operaciones registradas") {
        Text(
            text = "$totalOperaciones movimientos en el periodo seleccionado",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TarjetaSeccionEstadistica(
    titulo: String,
    contenido: @Composable () -> Unit
) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = titulo, style = MaterialTheme.typography.titleMedium)
            contenido()
        }
    }
}

@Composable
private fun GraficoBarrasCategorias(categorias: List<CategoriaEstadistica>) {
    val colores = listOf(
        Color(0xFF1F4E79),
        Color(0xFF2E8B57),
        Color(0xFFF9A825),
        Color(0xFF7E57C2)
    )
    val maximo = categorias.maxOf { it.importe }.coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        categorias.forEachIndexed { indice, categoria ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(categoria.nombre, style = MaterialTheme.typography.bodyMedium)
                    Text("${formatearDecimal(categoria.importe)} €", style = MaterialTheme.typography.bodyMedium)
                }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                ) {
                    drawRoundRect(
                        color = Color(0xFFE9E1EC),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                    drawRoundRect(
                        color = colores[indice % colores.size],
                        size = Size(width = size.width * (categoria.importe / maximo).toFloat(), height = size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                    )
                }
            }
        }
    }
}

@Composable
private fun GraficoLineaMensual(evolucion: List<GastoMensualEstadistica>) {
    val colorLinea = MaterialTheme.colorScheme.primary
    val colorGuia = MaterialTheme.colorScheme.outlineVariant
    val maximo = evolucion.maxOf { it.total }.coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val margenVertical = 18f
            val anchoPaso = if (evolucion.size > 1) size.width / (evolucion.size - 1) else size.width
            val puntos = evolucion.mapIndexed { indice, dato ->
                val x = if (evolucion.size > 1) indice * anchoPaso else size.width / 2f
                val y = size.height - margenVertical - ((dato.total / maximo).toFloat() * (size.height - margenVertical * 2))
                Offset(x, y)
            }

            drawLine(
                color = colorGuia,
                start = Offset(0f, size.height - margenVertical),
                end = Offset(size.width, size.height - margenVertical),
                strokeWidth = 2f
            )

            puntos.zipWithNext().forEach { (inicio, fin) ->
                drawLine(
                    color = colorLinea,
                    start = inicio,
                    end = fin,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }

            puntos.forEach { punto ->
                drawCircle(
                    color = colorLinea,
                    radius = 6f,
                    center = punto,
                    style = Stroke(width = 4f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            evolucion.take(1).forEach { Text(it.mes, style = MaterialTheme.typography.bodySmall) }
            evolucion.takeLast(1).forEach { Text(it.mes, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
private fun EstadoVacioEstadisticas(mensaje: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(mensaje, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatearDecimal(valor: Double): String {
    return String.format(Locale("es", "ES"), "%.2f", valor)
}
