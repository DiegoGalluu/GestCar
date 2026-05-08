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
    val conceptoVacio = recordatorio.concepto.isBlank()
    val kilometrajeInvalido = recordatorio.kilometrajeLimite != null && recordatorio.kilometrajeLimite <= 0
    val periodicidadTiempoInvalida = recordatorio.periodicidadTiempoCantidad != null && recordatorio.periodicidadTiempoCantidad <= 0
    val periodicidadKmInvalida = recordatorio.periodicidadKilometros != null && recordatorio.periodicidadKilometros <= 0
    val sinLimite = recordatorio.fechaLimite == null && recordatorio.kilometrajeLimite == null
    val faltaConcepto = intentoGuardar && conceptoVacio
    val faltanCamposObligatorios = conceptoVacio ||
        sinLimite ||
        kilometrajeInvalido ||
        periodicidadTiempoInvalida ||
        periodicidadKmInvalida

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
                    etiqueta = "Fecha limite",
                    fecha = fecha,
                    alSeleccionarFecha = {
                        viewModel.actualizarFormulario(recordatorio.copy(fechaLimite = it))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SelectorLimiteKilometraje(
                activado = recordatorio.kilometrajeLimite != null,
                alCambiar = { activado ->
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

            if (recordatorio.kilometrajeLimite != null) {
                OutlinedTextField(
                    value = recordatorio.kilometrajeLimite.takeIf { it > 0 }?.toLong()?.toString().orEmpty(),
                    onValueChange = {
                        viewModel.actualizarFormulario(
                            recordatorio.copy(kilometrajeLimite = it.toDoubleOrNull())
                        )
                    },
                    label = { Text("Kilometraje limite") },
                    isError = intentoGuardar && kilometrajeInvalido,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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
                        texto = "Dias",
                        seleccionada = recordatorio.periodicidadTiempoUnidad == UNIDAD_TIEMPO_DIAS,
                        alPulsar = { viewModel.actualizarFormulario(recordatorio.copy(periodicidadTiempoUnidad = UNIDAD_TIEMPO_DIAS)) }
                    )
                    ChipUnidadTiempo(
                        texto = "Meses",
                        seleccionada = recordatorio.periodicidadTiempoUnidad == UNIDAD_TIEMPO_MESES,
                        alPulsar = { viewModel.actualizarFormulario(recordatorio.copy(periodicidadTiempoUnidad = UNIDAD_TIEMPO_MESES)) }
                    )
                    ChipUnidadTiempo(
                        texto = "Anios",
                        seleccionada = recordatorio.periodicidadTiempoUnidad == UNIDAD_TIEMPO_ANIOS,
                        alPulsar = { viewModel.actualizarFormulario(recordatorio.copy(periodicidadTiempoUnidad = UNIDAD_TIEMPO_ANIOS)) }
                    )
                }
            }

            SelectorPeriodicidadKilometraje(
                activado = recordatorio.periodicidadKilometros != null,
                alCambiar = { activado ->
                    viewModel.actualizarFormulario(
                        recordatorio.copy(
                            periodicidadKilometros = if (activado) recordatorio.periodicidadKilometros ?: 10000.0 else null
                        )
                    )
                }
            )

            if (recordatorio.periodicidadKilometros != null) {
                OutlinedTextField(
                    value = recordatorio.periodicidadKilometros.takeIf { it > 0 }?.toLong()?.toString().orEmpty(),
                    onValueChange = {
                        viewModel.actualizarFormulario(
                            recordatorio.copy(periodicidadKilometros = it.toDoubleOrNull())
                        )
                    },
                    label = { Text("Cada X km") },
                    isError = intentoGuardar && periodicidadKmInvalida,
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

            MensajesValidacionRecordatorio(
                mostrar = intentoGuardar,
                conceptoVacio = conceptoVacio,
                sinLimite = sinLimite,
                kilometrajeInvalido = kilometrajeInvalido,
                periodicidadTiempoInvalida = periodicidadTiempoInvalida,
                periodicidadKmInvalida = periodicidadKmInvalida
            )

            estado.mensajeError?.let { error ->
                Snackbar {
                    Text(error)
                }
            }
        }
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
private fun SelectorPeriodicidadKilometraje(
    activado: Boolean,
    alCambiar: (Boolean) -> Unit
) {
    FilaConSwitch("Repetir por kilometraje", activado, alCambiar)
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
    periodicidadTiempoInvalida: Boolean,
    periodicidadKmInvalida: Boolean
) {
    if (!mostrar) {
        return
    }

    when {
        conceptoVacio -> Snackbar { Text("Faltan campos obligatorios por rellenar") }
        sinLimite -> Snackbar { Text("Indica una fecha limite o un kilometraje limite") }
        kilometrajeInvalido -> Snackbar { Text("El kilometraje limite debe ser mayor que cero") }
        periodicidadTiempoInvalida -> Snackbar { Text("La periodicidad por tiempo debe ser mayor que cero") }
        periodicidadKmInvalida -> Snackbar { Text("La periodicidad por kilometraje debe ser mayor que cero") }
    }
}
