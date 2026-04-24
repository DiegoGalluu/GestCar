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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalGasStation
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
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.RepostajeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleRepostaje(
    repostajeId: String,
    alEditar: (String, String) -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    viewModel: RepostajeViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val repostaje = estado.repostaje
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(repostajeId) {
        viewModel.cargarParaEditar(repostajeId)
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Detalle del repostaje",
                alVolver = alVolver,
                acciones = {
                    if (repostaje.id.isNotBlank()) {
                        IconButton(onClick = { alEditar(repostaje.vehiculoId, repostaje.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar repostaje")
                        }
                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar repostaje")
                        }
                    }
                }
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
                imageVector = Icons.Default.LocalGasStation,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "${String.format("%.2f", repostaje.importeTotal)} €",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = formatearFecha(repostaje.fecha),
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
                    FilaDato("Kilómetros", "${String.format("%,.0f", repostaje.kilometros)} km")
                    FilaDato("Litros", "${String.format("%.2f", repostaje.litros)} L")
                    FilaDato("Precio por litro", "${String.format("%.3f", repostaje.precioPorLitro)} €")
                    FilaDato("Importe total", "${String.format("%.2f", repostaje.importeTotal)} €")
                    FilaDato("Depósito lleno", if (repostaje.llenoCompleto) "Sí" else "No")
                    repostaje.gasolinera?.takeIf { it.isNotBlank() }?.let { FilaDato("Gasolinera", it) }
                    BloqueComentarios(repostaje.notas?.takeIf { it.isNotBlank() } ?: "Sin comentarios")
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar repostaje") },
            text = { Text("¿Seguro que quieres eliminar este repostaje?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarRepostaje(repostaje)
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
