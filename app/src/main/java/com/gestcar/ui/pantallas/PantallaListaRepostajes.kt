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
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.gestcar.ui.componentes.SelectorVehiculoActivo
import com.gestcar.ui.componentes.TarjetaRepostaje
import com.gestcar.ui.viewmodel.RepostajeViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaRepostajes(
    usuarioId: String,
    alCrearRepostaje: (String) -> Unit,
    alEditarRepostaje: (String, String) -> Unit,
    repostajeViewModel: RepostajeViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoRepostajes by repostajeViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo

    LaunchedEffect(usuarioId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { repostajeViewModel.cargarRepostajes(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Repostajes") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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
            SelectorVehiculoActivo(
                vehiculos = estadoVehiculo.vehiculos,
                vehiculoActivo = vehiculoActivo,
                alSeleccionar = { vehiculoActivoViewModel.seleccionarVehiculo(it) },
                modifier = Modifier.padding(16.dp)
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Consumo medio",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = if (estadoRepostajes.consumoMedio > 0) {
                                            "${String.format("%.2f", estadoRepostajes.consumoMedio)} L/100 km"
                                        } else {
                                            "Datos insuficientes"
                                        },
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                }
                            }
                        }

                        items(estadoRepostajes.repostajes) { repostaje ->
                            TarjetaRepostaje(
                                repostaje = repostaje,
                                alPulsar = { alEditarRepostaje(repostaje.vehiculoId, repostaje.id) }
                            )
                        }
                    }
                }
            }
        }
    }
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
