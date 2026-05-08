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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import com.gestcar.ui.viewmodel.RecordatorioViewModel
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_ANIOS
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_DIAS
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_MESES

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleRecordatorio(
    recordatorioId: String,
    alEditar: (String, String) -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    viewModel: RecordatorioViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val recordatorio = estado.recordatorio
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }

    LaunchedEffect(recordatorioId) {
        viewModel.cargarParaEditar(recordatorioId)
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Detalle del recordatorio",
                alVolver = alVolver,
                acciones = {
                    if (recordatorio.id.isNotBlank()) {
                        IconButton(onClick = { alEditar(recordatorio.vehiculoId, recordatorio.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar recordatorio")
                        }
                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar recordatorio")
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
                imageVector = if (recordatorio.completado) Icons.Default.CheckCircle else Icons.Default.Notifications,
                contentDescription = null,
                tint = if (recordatorio.completado) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = recordatorio.concepto,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (recordatorio.completado) "Completado" else "Pendiente",
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
                    recordatorio.fechaLimite?.let { FilaDato("Fecha límite", formatearFecha(it)) }
                    recordatorio.kilometrajeLimite?.let { FilaDato("Kilometraje límite", "${String.format("%,.0f", it)} km") }
                    textoPeriodicidadRecordatorio(
                        cantidadTiempo = recordatorio.periodicidadTiempoCantidad,
                        unidadTiempo = recordatorio.periodicidadTiempoUnidad,
                        kilometros = recordatorio.periodicidadKilometros
                    )?.let { FilaDato("Periodicidad", it) }
                    recordatorio.fechaCompletado?.let { FilaDato("Fecha completado", formatearFecha(it)) }
                    BloqueComentariosRecordatorio(recordatorio.notas?.takeIf { it.isNotBlank() } ?: "Sin comentarios")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (recordatorio.id.isNotBlank()) {
                Button(
                    onClick = {
                        if (recordatorio.completado) {
                            viewModel.reabrirRecordatorio(recordatorio)
                        } else {
                            viewModel.marcarComoCompletado(recordatorio)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (recordatorio.completado) "Marcar como pendiente" else "Marcar como completado")
                }
            }
        }
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar recordatorio") },
            text = { Text("¿Seguro que quieres eliminar este recordatorio?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarRecordatorio(recordatorio)
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
private fun BloqueComentariosRecordatorio(comentarios: String) {
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

private fun textoPeriodicidadRecordatorio(
    cantidadTiempo: Int?,
    unidadTiempo: String?,
    kilometros: Double?
): String? {
    val partes = mutableListOf<String>()

    if (cantidadTiempo != null && cantidadTiempo > 0 && unidadTiempo != null) {
        val unidad = when (unidadTiempo) {
            UNIDAD_TIEMPO_DIAS -> if (cantidadTiempo == 1) "dia" else "dias"
            UNIDAD_TIEMPO_MESES -> if (cantidadTiempo == 1) "mes" else "meses"
            UNIDAD_TIEMPO_ANIOS -> if (cantidadTiempo == 1) "anio" else "anios"
            else -> null
        }
        unidad?.let { partes.add("Cada $cantidadTiempo $it") }
    }

    kilometros?.takeIf { it > 0 }?.let {
        partes.add("Cada ${String.format("%,.0f", it)} km")
    }

    return partes.takeIf { it.isNotEmpty() }?.joinToString(" o ")
}
