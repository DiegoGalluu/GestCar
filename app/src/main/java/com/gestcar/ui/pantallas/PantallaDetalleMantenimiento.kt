package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION
import com.gestcar.ui.viewmodel.MantenimientoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleMantenimiento(
    mantenimientoId: String,
    alEditar: (String, String) -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    viewModel: MantenimientoViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val mantenimiento = estado.mantenimiento
    val esReparacion = mantenimiento.categoria == CATEGORIA_REPARACION
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(mantenimientoId) {
        viewModel.cargarParaEditar(mantenimientoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de operación") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (mantenimiento.id.isNotBlank()) {
                        IconButton(onClick = { alEditar(mantenimiento.vehiculoId, mantenimiento.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar operación")
                        }
                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar operación")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (esReparacion) Icons.Default.Warning else Icons.Default.Build,
                contentDescription = null,
                tint = if (esReparacion) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = mantenimiento.tipo,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (esReparacion) "Reparación" else "Mantenimiento",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilaDato("Fecha", formatearFecha(mantenimiento.fecha))
                    mantenimiento.kilometros?.let { FilaDato("Kilómetros", "${String.format("%,.0f", it)} km") }
                    FilaDato("Coste", if (mantenimiento.coste > 0) "${String.format("%.2f", mantenimiento.coste)} €" else "Sin coste")
                    mantenimiento.taller?.takeIf { it.isNotBlank() }?.let { FilaDato("Taller", it) }
                    BloqueComentarios(mantenimiento.descripcion?.takeIf { it.isNotBlank() } ?: "Sin comentarios")
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar operación") },
            text = { Text("¿Seguro que quieres eliminar esta operación?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarMantenimiento(mantenimiento)
                        mostrarDialogoEliminar = false
                        alEliminar()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun BloqueComentarios(comentarios: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Comentarios",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = comentarios,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
