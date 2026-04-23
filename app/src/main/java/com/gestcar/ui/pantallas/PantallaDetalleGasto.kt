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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
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
import com.gestcar.ui.viewmodel.GastoPeriodicoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleGasto(
    gastoId: String,
    alEditar: (String, String) -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    viewModel: GastoPeriodicoViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val gasto = estado.gasto
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(gastoId) {
        viewModel.cargarParaEditar(gastoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del gasto") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (gasto.id.isNotBlank()) {
                        IconButton(onClick = { alEditar(gasto.vehiculoId, gasto.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar gasto")
                        }
                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar gasto")
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
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = gasto.concepto,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "${String.format("%.2f", gasto.importe)} €",
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
                    FilaDato("Fecha", formatearFecha(gasto.fecha))
                    FilaDato("Periodicidad", textoPeriodicidadDetalle(gasto.periodicidad))
                    gasto.fechaVencimiento?.let { FilaDato("Vencimiento", formatearFecha(it)) }
                    BloqueComentariosGasto(gasto.notas?.takeIf { it.isNotBlank() } ?: "Sin comentarios")
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar gasto") },
            text = { Text("¿Seguro que quieres eliminar este gasto?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarGasto(gasto)
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
private fun BloqueComentariosGasto(comentarios: String) {
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

private fun textoPeriodicidadDetalle(periodicidad: String?): String {
    return periodicidad
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Único"
}
