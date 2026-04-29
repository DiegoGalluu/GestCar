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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.EstadoVisualGasto
import com.gestcar.ui.componentes.SelectorVehiculoActivo
import com.gestcar.ui.componentes.TarjetaGasto
import com.gestcar.ui.componentes.calcularEstadoVisualGasto
import com.gestcar.ui.viewmodel.GastoPeriodicoViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaGastos(
    usuarioId: String,
    vehiculoInicialId: String? = null,
    alCrearGasto: (String) -> Unit,
    alVerDetalleGasto: (String, String) -> Unit,
    gastoViewModel: GastoPeriodicoViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoGastos by gastoViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo
    var mostrarPagados by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(usuarioId, vehiculoInicialId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId, vehiculoInicialId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { gastoViewModel.cargarGastos(it.id) }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(titulo = "Gastos")
        },
        floatingActionButton = {
            if (vehiculoActivo != null) {
                FloatingActionButton(
                    onClick = { alCrearGasto(vehiculoActivo.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir gasto")
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
                estadoVehiculo.estaCargando || estadoGastos.estaCargando -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                vehiculoActivo == null -> {
                    EstadoVacioGastos(
                        titulo = "No hay vehículos",
                        mensaje = "Añade un vehículo antes de registrar gastos"
                    )
                }

                estadoGastos.gastos.isEmpty() -> {
                    EstadoVacioGastos(
                        titulo = "No hay gastos",
                        mensaje = "Pulsa + para añadir el primero"
                    )
                }

                else -> {
                    val gastosPendientes = estadoGastos.gastos
                        .filter { !it.pagado }
                        .sortedWith(
                            compareBy<GastoPeriodico>(
                                { prioridadEstadoGasto(it) },
                                { it.fechaVencimiento ?: Long.MAX_VALUE },
                                { -it.fecha }
                            )
                        )

                    val gastosPagados = estadoGastos.gastos
                        .filter { it.pagado }
                        .sortedByDescending { it.fechaPago ?: it.fecha }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (gastosPendientes.isNotEmpty()) {
                            item {
                                TituloSeccionGastos("Pendientes")
                            }

                            items(gastosPendientes) { gasto ->
                                TarjetaGasto(
                                    gasto = gasto,
                                    alPulsar = { alVerDetalleGasto(gasto.vehiculoId, gasto.id) }
                                )
                            }
                        }

                        if (gastosPagados.isNotEmpty()) {
                            item {
                                if (gastosPendientes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                TituloSeccionGastosDesplegable(
                                    titulo = "Pagados",
                                    cantidad = gastosPagados.size,
                                    expandido = mostrarPagados,
                                    alCambiarExpandido = { mostrarPagados = !mostrarPagados }
                                )
                            }

                            if (mostrarPagados) {
                                items(gastosPagados) { gasto ->
                                    TarjetaGasto(
                                        gasto = gasto,
                                        alPulsar = { alVerDetalleGasto(gasto.vehiculoId, gasto.id) }
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
private fun TituloSeccionGastos(titulo: String) {
    Text(
        text = titulo,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun TituloSeccionGastosDesplegable(
    titulo: String,
    cantidad: Int,
    expandido: Boolean,
    alCambiarExpandido: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$titulo ($cantidad)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = alCambiarExpandido,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (expandido) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = if (expandido) "Ocultar pagados" else "Mostrar pagados"
            )
        }
    }
}

@Composable
private fun EstadoVacioGastos(
    titulo: String,
    mensaje: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Payments,
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

private fun prioridadEstadoGasto(gasto: GastoPeriodico): Int {
    return when (calcularEstadoVisualGasto(gasto)) {
        EstadoVisualGasto.VENCIDO -> 0
        EstadoVisualGasto.PROXIMO -> 1
        EstadoVisualGasto.AL_DIA -> 2
        EstadoVisualGasto.SIN_VENCIMIENTO -> 3
        EstadoVisualGasto.PAGADO -> 4
    }
}
