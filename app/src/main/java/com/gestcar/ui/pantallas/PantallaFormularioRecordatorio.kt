package com.gestcar.ui.pantallas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.viewmodel.RecordatorioViewModel
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_ANIOS
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_DIAS
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_MESES
import com.gestcar.ui.viewmodel.UNIDAD_TIEMPO_SEMANAS

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
    val kilometrajeActual = estado.kilometrajeVehiculoActual
    var intentoGuardar by remember { mutableStateOf(false) }
    var seccionFechaAbierta by remember { mutableStateOf(recordatorio.fechaLimite != null) }
    var seccionKilometrosAbierta by remember {
        mutableStateOf(recordatorio.kilometrajeLimite != null || recordatorio.periodicidadKilometros != null)
    }
    var mostrarDialogoTiempoPeriodico by remember { mutableStateOf(false) }
    var mostrarDialogoKilometrosPeriodicos by remember { mutableStateOf(false) }
    var mostrarDialogoKilometrajeInferior by remember { mutableStateOf(false) }

    val conceptoVacio = recordatorio.concepto.isBlank()
    val avisoFechaActivo = recordatorio.fechaLimite != null
    val avisoKilometrosActivo = recordatorio.kilometrajeLimite != null || recordatorio.periodicidadKilometros != null
    val valorKilometrosFormulario = recordatorio.periodicidadKilometros ?: recordatorio.kilometrajeLimite
    val kilometrosInvalidos = avisoKilometrosActivo && (valorKilometrosFormulario == null || valorKilometrosFormulario <= 0)
    val tiempoPeriodicoInvalido = recordatorio.periodicidadTiempoCantidad != null && recordatorio.periodicidadTiempoCantidad <= 0
    val kilometrajeInferiorNoPeriodico = recordatorio.periodicidadKilometros == null &&
        recordatorio.kilometrajeLimite != null &&
        recordatorio.kilometrajeLimite < kilometrajeActual
    val hayErrores = conceptoVacio || kilometrosInvalidos || tiempoPeriodicoInvalido

    LaunchedEffect(vehiculoId, recordatorioId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(recordatorioId)
        }
    }

    LaunchedEffect(recordatorio.fechaLimite, recordatorio.kilometrajeLimite, recordatorio.periodicidadKilometros) {
        seccionFechaAbierta = recordatorio.fechaLimite != null || seccionFechaAbierta
        seccionKilometrosAbierta =
            recordatorio.kilometrajeLimite != null || recordatorio.periodicidadKilometros != null || seccionKilometrosAbierta
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
                isError = intentoGuardar && conceptoVacio,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            SeccionDesplegableRecordatorio(
                titulo = "Por fecha",
                abierta = seccionFechaAbierta,
                alCambiar = { seccionFechaAbierta = !seccionFechaAbierta }
            ) {
                if (recordatorio.fechaLimite == null) {
                    Text(
                        text = "Sin fecha límite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            viewModel.actualizarFormulario(
                                recordatorio.copy(fechaLimite = System.currentTimeMillis())
                            )
                        }
                    ) {
                        Text("+ Añadir fecha")
                    }
                } else {
                    CampoFecha(
                        etiqueta = "Fecha límite",
                        fecha = recordatorio.fechaLimite,
                        alSeleccionarFecha = {
                            viewModel.actualizarFormulario(recordatorio.copy(fechaLimite = it))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = {
                            viewModel.actualizarFormulario(
                                recordatorio.copy(
                                    fechaLimite = null,
                                    periodicidadTiempoCantidad = null,
                                    periodicidadTiempoUnidad = null
                                )
                            )
                        }
                    ) {
                        Text("Quitar fecha")
                    }

                    FilaConSwitch(
                        texto = "Periódico",
                        activado = recordatorio.periodicidadTiempoCantidad != null,
                        alCambiar = { activado ->
                            if (activado) {
                                mostrarDialogoTiempoPeriodico = true
                            } else {
                                viewModel.actualizarFormulario(
                                    recordatorio.copy(
                                        periodicidadTiempoCantidad = null,
                                        periodicidadTiempoUnidad = null
                                    )
                                )
                            }
                        }
                    )

                    TextoResumenPeriodicidadTiempo(recordatorio)
                }
            }

            SeccionDesplegableRecordatorio(
                titulo = "Por kilómetros",
                abierta = seccionKilometrosAbierta,
                alCambiar = { seccionKilometrosAbierta = !seccionKilometrosAbierta }
            ) {
                OutlinedTextField(
                    value = valorKilometrosFormulario?.takeIf { it > 0 }?.let { String.format("%.0f", it) }.orEmpty(),
                    onValueChange = { texto ->
                        val valor = texto.filtrarNumeroDecimal().toDoubleOrNull()
                        viewModel.actualizarFormulario(
                            if (recordatorio.periodicidadKilometros != null) {
                                recordatorio.copy(
                                    periodicidadKilometros = valor,
                                    kilometrajeLimite = valor?.takeIf { it > 0 }?.let { kilometrajeActual + it }
                                )
                            } else {
                                recordatorio.copy(kilometrajeLimite = valor)
                            }
                        )
                    },
                    label = {
                        Text(
                            if (recordatorio.periodicidadKilometros != null) {
                                "Cada X km"
                            } else {
                                "Kilometraje límite"
                            }
                        )
                    },
                    isError = intentoGuardar && kilometrosInvalidos,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                FilaConSwitch(
                    texto = "Periódico",
                    activado = recordatorio.periodicidadKilometros != null,
                    alCambiar = { activado ->
                        if (activado) {
                            val valor = valorKilometrosFormulario?.takeIf { it > 0 }
                            if (valor == null) {
                                viewModel.actualizarFormulario(recordatorio.copy(periodicidadKilometros = 10000.0))
                            }
                            mostrarDialogoKilometrosPeriodicos = true
                        } else {
                            viewModel.actualizarFormulario(recordatorio.copy(periodicidadKilometros = null))
                        }
                    }
                )

                TextoResumenPeriodicidadKilometros(recordatorio)
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
                    when {
                        hayErrores -> Unit
                        kilometrajeInferiorNoPeriodico -> mostrarDialogoKilometrajeInferior = true
                        else -> viewModel.guardarRecordatorio()
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
                kilometrosInvalidos = kilometrosInvalidos,
                tiempoPeriodicoInvalido = tiempoPeriodicoInvalido
            )

            estado.mensajeError?.let { error ->
                Snackbar { Text(error) }
            }
        }
    }

    if (mostrarDialogoTiempoPeriodico) {
        DialogoPeriodicidadTiempo(
            alCancelar = {
                mostrarDialogoTiempoPeriodico = false
                viewModel.actualizarFormulario(
                    recordatorio.copy(
                        periodicidadTiempoCantidad = null,
                        periodicidadTiempoUnidad = null
                    )
                )
            },
            alAceptar = { cantidad, unidad ->
                mostrarDialogoTiempoPeriodico = false
                viewModel.actualizarFormulario(
                    recordatorio.copy(
                        periodicidadTiempoCantidad = cantidad,
                        periodicidadTiempoUnidad = unidad
                    )
                )
            }
        )
    }

    if (mostrarDialogoKilometrosPeriodicos) {
        val valor = valorKilometrosFormulario?.takeIf { it > 0 } ?: 10000.0
        AlertDialog(
            onDismissRequest = { mostrarDialogoKilometrosPeriodicos = false },
            title = { Text("Recordatorio periódico") },
            text = {
                Text("¿Quieres repetir este recordatorio cada ${String.format("%,.0f", valor)} km?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoKilometrosPeriodicos = false
                        viewModel.actualizarFormulario(
                            recordatorio.copy(
                                periodicidadKilometros = valor,
                                kilometrajeLimite = kilometrajeActual + valor
                            )
                        )
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoKilometrosPeriodicos = false
                        viewModel.actualizarFormulario(recordatorio.copy(periodicidadKilometros = null))
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoKilometrajeInferior) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoKilometrajeInferior = false },
            title = { Text("Kilometraje inferior al actual") },
            text = {
                Text(
                    "El vehículo ya tiene más kilómetros que el límite introducido. " +
                        "Si guardas este recordatorio aparecerá como vencido. ¿Quieres continuar?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        mostrarDialogoKilometrajeInferior = false
                        viewModel.guardarRecordatorio()
                    }
                ) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoKilometrajeInferior = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SeccionDesplegableRecordatorio(
    titulo: String,
    abierta: Boolean,
    alCambiar: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = alCambiar),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (abierta) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = abierta) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = contenido
                )
            }
        }
    }
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
private fun DialogoPeriodicidadTiempo(
    alCancelar: () -> Unit,
    alAceptar: (Int, String) -> Unit
) {
    var opcionSeleccionada by remember { mutableStateOf(OpcionTiempo.UN_MES) }
    var cantidadPersonalizada by remember { mutableStateOf("1") }
    var unidadPersonalizada by remember { mutableStateOf(UNIDAD_TIEMPO_MESES) }
    val cantidad = cantidadPersonalizada.toIntOrNull()?.takeIf { it > 0 } ?: 1

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Repetición por fecha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Elige cada cuánto quieres que se vuelva a crear este recordatorio.")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipOpcionTiempo("Día", opcionSeleccionada == OpcionTiempo.UN_DIA) {
                        opcionSeleccionada = OpcionTiempo.UN_DIA
                    }
                    ChipOpcionTiempo("Semana", opcionSeleccionada == OpcionTiempo.UNA_SEMANA) {
                        opcionSeleccionada = OpcionTiempo.UNA_SEMANA
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipOpcionTiempo("Mes", opcionSeleccionada == OpcionTiempo.UN_MES) {
                        opcionSeleccionada = OpcionTiempo.UN_MES
                    }
                    ChipOpcionTiempo("Año", opcionSeleccionada == OpcionTiempo.UN_ANIO) {
                        opcionSeleccionada = OpcionTiempo.UN_ANIO
                    }
                }
                ChipOpcionTiempo("Personalizado", opcionSeleccionada == OpcionTiempo.PERSONALIZADO) {
                    opcionSeleccionada = OpcionTiempo.PERSONALIZADO
                }

                AnimatedVisibility(visible = opcionSeleccionada == OpcionTiempo.PERSONALIZADO) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cantidadPersonalizada,
                            onValueChange = { cantidadPersonalizada = it.filter(Char::isDigit) },
                            label = { Text("Cada") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipUnidadTiempo("Días", unidadPersonalizada == UNIDAD_TIEMPO_DIAS) {
                                unidadPersonalizada = UNIDAD_TIEMPO_DIAS
                            }
                            ChipUnidadTiempo("Semanas", unidadPersonalizada == UNIDAD_TIEMPO_SEMANAS) {
                                unidadPersonalizada = UNIDAD_TIEMPO_SEMANAS
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChipUnidadTiempo("Meses", unidadPersonalizada == UNIDAD_TIEMPO_MESES) {
                                unidadPersonalizada = UNIDAD_TIEMPO_MESES
                            }
                            ChipUnidadTiempo("Años", unidadPersonalizada == UNIDAD_TIEMPO_ANIOS) {
                                unidadPersonalizada = UNIDAD_TIEMPO_ANIOS
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val resultado = when (opcionSeleccionada) {
                        OpcionTiempo.UN_DIA -> 1 to UNIDAD_TIEMPO_DIAS
                        OpcionTiempo.UNA_SEMANA -> 1 to UNIDAD_TIEMPO_SEMANAS
                        OpcionTiempo.UN_MES -> 1 to UNIDAD_TIEMPO_MESES
                        OpcionTiempo.UN_ANIO -> 1 to UNIDAD_TIEMPO_ANIOS
                        OpcionTiempo.PERSONALIZADO -> cantidad to unidadPersonalizada
                    }
                    alAceptar(resultado.first, resultado.second)
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ChipOpcionTiempo(
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
private fun TextoResumenPeriodicidadTiempo(recordatorio: Recordatorio) {
    val cantidad = recordatorio.periodicidadTiempoCantidad ?: return
    val unidad = recordatorio.periodicidadTiempoUnidad ?: return
    Text(
        text = "Se repetirá cada $cantidad ${unidadTiempoLegible(cantidad, unidad)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun TextoResumenPeriodicidadKilometros(recordatorio: Recordatorio) {
    val kilometros = recordatorio.periodicidadKilometros?.takeIf { it > 0 } ?: return
    Text(
        text = "Se repetirá cada ${String.format("%,.0f", kilometros)} km",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun MensajesValidacionRecordatorio(
    mostrar: Boolean,
    conceptoVacio: Boolean,
    kilometrosInvalidos: Boolean,
    tiempoPeriodicoInvalido: Boolean
) {
    if (!mostrar) {
        return
    }

    when {
        conceptoVacio -> Snackbar { Text("Faltan campos obligatorios por rellenar") }
        kilometrosInvalidos -> Snackbar { Text("El kilometraje debe ser mayor que cero") }
        tiempoPeriodicoInvalido -> Snackbar { Text("La periodicidad por fecha debe ser mayor que cero") }
    }
}

private enum class OpcionTiempo {
    UN_DIA,
    UNA_SEMANA,
    UN_MES,
    UN_ANIO,
    PERSONALIZADO
}

private fun String.filtrarNumeroDecimal(): String {
    return replace(",", ".")
        .filterIndexed { indice, caracter ->
            caracter.isDigit() || (caracter == '.' && indexOf('.') == indice)
        }
}

private fun unidadTiempoLegible(cantidad: Int, unidad: String): String {
    return when (unidad) {
        UNIDAD_TIEMPO_DIAS -> if (cantidad == 1) "día" else "días"
        UNIDAD_TIEMPO_SEMANAS -> if (cantidad == 1) "semana" else "semanas"
        UNIDAD_TIEMPO_MESES -> if (cantidad == 1) "mes" else "meses"
        UNIDAD_TIEMPO_ANIOS -> if (cantidad == 1) "año" else "años"
        else -> "periodos"
    }
}
