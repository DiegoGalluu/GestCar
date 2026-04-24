package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.viewmodel.RecordatorioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioRecordatorio(
    vehiculoId: String,
    recordatorioId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: RecordatorioViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val esNuevo = recordatorioId == "nuevo"
    val recordatorio = estado.recordatorio
    var intentoGuardar by remember { mutableStateOf(false) }
    val conceptoVacio = recordatorio.concepto.isBlank()
    val kilometrajeInvalido = recordatorio.kilometrajeLimite != null && recordatorio.kilometrajeLimite <= 0
    val sinLimite = recordatorio.fechaLimite == null && recordatorio.kilometrajeLimite == null
    val faltaConcepto = intentoGuardar && conceptoVacio
    val faltanCamposObligatorios = conceptoVacio || sinLimite || kilometrajeInvalido

    LaunchedEffect(vehiculoId, recordatorioId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(recordatorioId)
        }
    }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            alGuardar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esNuevo) "Nuevo recordatorio" else "Editar recordatorio") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = recordatorio.concepto,
                onValueChange = { viewModel.actualizarFormulario(recordatorio.copy(concepto = it)) },
                label = { Text("Concepto *") },
                isError = faltaConcepto,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Avisar por fecha", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = recordatorio.fechaLimite != null,
                    onCheckedChange = { activado ->
                        viewModel.actualizarFormulario(
                            recordatorio.copy(
                                fechaLimite = if (activado) {
                                    recordatorio.fechaLimite ?: System.currentTimeMillis()
                                } else {
                                    null
                                }
                            )
                        )
                    }
                )
            }

            recordatorio.fechaLimite?.let { fecha ->
                CampoFecha(
                    etiqueta = "Fecha límite",
                    fecha = fecha,
                    alSeleccionarFecha = {
                        viewModel.actualizarFormulario(recordatorio.copy(fechaLimite = it))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Avisar por kilometraje", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = recordatorio.kilometrajeLimite != null,
                    onCheckedChange = { activado ->
                        viewModel.actualizarFormulario(
                            recordatorio.copy(
                                kilometrajeLimite = if (activado) {
                                    recordatorio.kilometrajeLimite ?: 0.0
                                } else {
                                    null
                                }
                            )
                        )
                    }
                )
            }

            if (recordatorio.kilometrajeLimite != null) {
                OutlinedTextField(
                    value = recordatorio.kilometrajeLimite.takeIf { it > 0 }?.toLong()?.toString().orEmpty(),
                    onValueChange = {
                        viewModel.actualizarFormulario(
                            recordatorio.copy(kilometrajeLimite = it.toDoubleOrNull())
                        )
                    },
                    label = { Text("Kilometraje límite") },
                    isError = intentoGuardar && kilometrajeInvalido,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = recordatorio.notas ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(recordatorio.copy(notas = it.ifBlank { null }))
                },
                label = { Text("Comentarios") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    intentoGuardar = true
                    if (!faltanCamposObligatorios) {
                        viewModel.guardarRecordatorio()
                    }
                },
                enabled = !estado.estaCargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.estaCargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar recordatorio")
                }
            }

            if (intentoGuardar && conceptoVacio) {
                Snackbar {
                    Text("Faltan campos obligatorios por rellenar")
                }
            }

            if (intentoGuardar && !conceptoVacio && sinLimite) {
                Snackbar {
                    Text("Indica una fecha límite o un kilometraje límite")
                }
            }

            if (intentoGuardar && kilometrajeInvalido) {
                Snackbar {
                    Text("El kilometraje límite debe ser mayor que cero")
                }
            }

            estado.mensajeError?.let { error ->
                Snackbar {
                    Text(error)
                }
            }
        }
    }
}
