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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.viewmodel.RecordatorioViewModel
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_ANIOS
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_DIAS
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_MESES

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
    var mostrarDialogoKilometrajeSuperado by remember { mutableStateOf(false) }
    val conceptoVacio = recordatorio.concepto.isBlank()
    val periodicidadKilometrajeActiva = recordatorio.periodicidadKilometros != null
    val avisoKilometrajeActivo = recordatorio.kilometrajeLimite != null || periodicidadKilometrajeActiva
    val valorCampoKilometraje = if (periodicidadKilometrajeActiva) {
        recordatorio.periodicidadKilometros
    } else {
        recordatorio.kilometrajeLimite
    }
    val kilometrajeInvalido = avisoKilometrajeActivo && (valorCampoKilometraje == null || valorCampoKilometraje <= 0)
    val kilometrajeYaSuperado = avisoKilometrajeActivo &&
        !periodicidadKilometrajeActiva &&
        valorCampoKilometraje != null &&
        valorCampoKilometraje < estado.kilometrajeVehiculoActual
    val periodicidadTiempoInvalida = recordatorio.periodicidadTiempoCantidad != null && recordatorio.periodicidadTiempoCantidad <= 0
    val sinLimite = recordatorio.fechaLimite == null && !avisoKilometrajeActivo
    val faltaConcepto = intentoGuardar && conceptoVacio
    val faltanCamposObligatorios = conceptoVacio ||
        sinLimite ||
        kilometrajeInvalido ||
        periodicidadTiempoInvalida

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
            BarraSuperiorCompacta(
                titulo = if (esNuevo) "Nuevo recordatorio" else "Editar recordatorio",
                alVolver = alVolver
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

            SelectorLimiteFecha(
                activado = recordatorio.fechaLimite != null,
                alCambiar = { activado ->
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

            SelectorLimiteKilometraje(
                activado = avisoKilometrajeActivo,
                alCambiar = { activado ->
                    viewModel.actualizarFormulario(
                        recordatorio.copy(
                            kilometrajeLimite = if (activado) {
                                recordatorio.kilometrajeLimite ?: estado.kilometrajeVehiculoActual.takeIf { it > 0 } ?: 0.0
                            } else {
                                null
                            },
                            periodicidadKilometros = if (activado) recordatorio.periodicidadKilometros else null
                        )
                    )
                }
            )

            if (avisoKilometrajeActivo) {
                OutlinedTextField(
                    value = valorCampoKilometraje.takeIf { it != null && it > 0 }?.toLong()?.toString().orEmpty(),
                    onValueChange = {
                        val valor = it.replace(",", ".").toDoubleOrNull()
                        viewModel.actualizarFormulario(
                            if (periodicidadKilometrajeActiva) {
                                recordatorio.copy(
                                    periodicidadKilometros = valor,
                                    kilometrajeLimite = valor?.takeIf { intervalo -> intervalo > 0 }?.let { intervalo ->
                                        estado.kilometrajeVehiculoActual + intervalo
                                    }
                                )
                            } else {
                                recordatorio.copy(kilometrajeLimite = valor)
                            }
                        )
                    },
                    label = {
                        Text(if (periodicidadKilometrajeActiva) "Intervalo en kilómetros" else "Kilometraje límite")
                    },
                    isError = intentoGuardar && kilometrajeInvalido,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                FilaConSwitch(
                    texto = "Repetir por kilometraje",
                    activado = periodicidadKilometrajeActiva,
                    alCambiar = { activado ->
                        val valorActual = valorCampoKilometraje?.takeIf { it > 0 } ?: 10000.0
                        viewModel.actualizarFormulario(
                            if (activado) {
                                recordatorio.copy(
                                    periodicidadKilometros = valorActual,
                                    kilometrajeLimite = estado.kilometrajeVehiculoActual + valorActual
                                )
                            } else {
                                recordatorio.copy(
                                    periodicidadKilometros = null,
                                    kilometrajeLimite = recordatorio.kilometrajeLimite
                                )
                            }
                        )
                    }
                )
            }

            Text(
                text = "Periodicidad",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            SelectorPeriodicidadTiempo(
                activado = recordatorio.periodicidadTiempoCantidad != null,
                alCambiar = { activado ->
                    viewModel.actualizarFormulario(
                        recordatorio.copy(
                            periodicidadTiempoCantidad = if (activado) recordatorio.periodicidadTiempoCantidad ?: 1 else null,
                            periodicidadTiempoUnidad = if (activado) recordatorio.periodicidadTiempoUnidad ?: UNIDAD_TIEMPO_ANIOS else null
                        )
                    )
                }
            )

            if (recordatorio.periodicidadTiempoCantidad != null) {
                OutlinedTextField(
                    value = recordatorio.periodicidadTiempoCantidad.takeIf { it > 0 }?.toString().orEmpty(),
                    onValueChange = {
                        viewModel.actualizarFormulario(
                            recordatorio.copy(periodicidadTiempoCantidad = it.toIntOrNull())
                        )
                    },
                    label = { Text("Cada") },
                    isError = intentoGuardar && periodicidadTiempoInvalida,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipUnidadTiempo(
                        texto = "Días",
                        seleccionada = recordatorio.periodicidadTiempoUnidad == UNIDAD_TIEMPO_DIAS,
                        alPulsar = { viewModel.actualizarFormulario(recordatorio.copy(periodicidadTiempoUnidad = UNIDAD_TIEMPO_DIAS)) }
                    )
                    ChipUnidadTiempo(
                        texto = "Meses",
                        seleccionada = recordatorio.periodicidadTiempoUnidad == UNIDAD_TIEMPO_MESES,
                        alPulsar = { viewModel.actualizarFormulario(recordatorio.copy(periodicidadTiempoUnidad = UNIDAD_TIEMPO_MESES)) }
                    )
                    ChipUnidadTiempo(
                        texto = "Años",
                        seleccionada = recordatorio.periodicidadTiempoUnidad == UNIDAD_TIEMPO_ANIOS,
                        alPulsar = { viewModel.actualizarFormulario(recordatorio.copy(periodicidadTiempoUnidad = UNIDAD_TIEMPO_ANIOS)) }
                    )
                }
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
                        if (kilometrajeYaSuperado) {
                            mostrarDialogoKilometrajeSuperado = true
                        } else {
                            viewModel.guardarRecordatorio()
                        }
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

            MensajesValidacionRecordatorio(
                mostrar = intentoGuardar,
                conceptoVacio = conceptoVacio,
                sinLimite = sinLimite,
                kilometrajeInvalido = kilometrajeInvalido,
                periodicidadTiempoInvalida = periodicidadTiempoInvalida
            )

            estado.mensajeError?.let { error ->
                Snackbar {
                    Text(error)
                }
            }
        }
    }

    if (mostrarDialogoKilometrajeSuperado) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoKilometrajeSuperado = false },
            title = { Text("Kilometraje ya superado") },
            text = {
                Text(
                    "El vehículo ya tiene más kilómetros que el límite indicado. " +
                        "Si quieres un aviso recurrente, activa la opción de repetir por kilometraje."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoKilometrajeSuperado = false
                        viewModel.guardarRecordatorio()
                    }
                ) {
                    Text("Guardar igualmente")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoKilometrajeSuperado = false }) {
                    Text("Revisar")
                }
            }
        )
    }
}

@Composable
private fun SelectorLimiteFecha(
    activado: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    FilaConSwitch("Avisar por fecha", activado, alCambiar)
}

@Composable
private fun SelectorLimiteKilometraje(
    activado: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    FilaConSwitch("Avisar por kilometraje", activado, alCambiar)
}

@Composable
private fun SelectorPeriodicidadTiempo(
    activado: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    FilaConSwitch("Repetir por tiempo", activado, alCambiar)
}

@Composable
private fun FilaConSwitch(
    texto: String,
    activado: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = activado,
            onCheckedChange = alCambiar
        )
    }
}

@Composable
private fun ChipUnidadTiempo(
    texto: String,
    seleccionada: Boolean,
    alPulsar: () -> Unit
) {
    FilterChip(
        selected = seleccionada,
        onClick = alPulsar,
        label = { Text(texto) }
    )
}

@Composable
private fun MensajesValidacionRecordatorio(
    mostrar: Boolean,
    conceptoVacio: Boolean,
    sinLimite: Boolean,
    kilometrajeInvalido: Boolean,
    periodicidadTiempoInvalida: Boolean
) {
    if (!mostrar) {
        return
    }

    when {
        conceptoVacio -> Snackbar { Text("Faltan campos obligatorios por rellenar") }
        sinLimite -> Snackbar { Text("Indica una fecha límite o un kilometraje límite") }
        kilometrajeInvalido -> Snackbar { Text("El kilometraje debe ser mayor que cero") }
        periodicidadTiempoInvalida -> Snackbar { Text("La periodicidad por tiempo debe ser mayor que cero") }
    }
}
