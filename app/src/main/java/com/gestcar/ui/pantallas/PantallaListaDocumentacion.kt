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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.SelectorVehiculoActivo
import com.gestcar.ui.componentes.TarjetaDocumento
import com.gestcar.ui.viewmodel.DocumentacionViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel

@Composable
fun PantallaListaDocumentacion(
    usuarioId: String,
    alCrearDocumento: (String) -> Unit,
    alVerDetalleDocumento: (String, String) -> Unit,
    alVolver: () -> Unit,
    documentacionViewModel: DocumentacionViewModel = viewModel(),
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel()
) {
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoDocumentacion by documentacionViewModel.estadoLista.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo

    LaunchedEffect(usuarioId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId)
    }

    LaunchedEffect(vehiculoActivo?.id) {
        vehiculoActivo?.let { documentacionViewModel.cargarDocumentos(it.id) }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Documentación",
                alVolver = alVolver
            )
        },
        floatingActionButton = {
            if (vehiculoActivo != null) {
                FloatingActionButton(
                    onClick = { alCrearDocumento(vehiculoActivo.id) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Añadir documentación")
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
                estadoVehiculo.estaCargando || estadoDocumentacion.estaCargando -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                vehiculoActivo == null -> {
                    EstadoVacioDocumentacion(
                        titulo = "No hay vehículos",
                        mensaje = "Añade un vehículo antes de guardar documentación"
                    )
                }

                estadoDocumentacion.documentos.isEmpty() -> {
                    EstadoVacioDocumentacion(
                        titulo = "No hay documentación",
                        mensaje = "Pulsa + para crear tu primera tarjeta"
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(estadoDocumentacion.documentos) { documento ->
                            TarjetaDocumento(
                                documento = documento,
                                cantidadCampos = estadoDocumentacion
                                    .cantidadCamposPorDocumento[documento.id]
                                    ?: 0,
                                alPulsar = {
                                    alVerDetalleDocumento(documento.vehiculoId, documento.id)
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
private fun EstadoVacioDocumentacion(
    titulo: String,
    mensaje: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
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
