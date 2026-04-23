package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.gestcar.ui.viewmodel.GastoPeriodicoViewModel
import com.gestcar.ui.viewmodel.PERIODICIDAD_ANUAL
import com.gestcar.ui.viewmodel.PERIODICIDAD_MENSUAL
import com.gestcar.ui.viewmodel.PERIODICIDAD_SEMESTRAL
import com.gestcar.ui.viewmodel.PERIODICIDAD_TRIMESTRAL
import com.gestcar.ui.viewmodel.PERIODICIDAD_UNICO
import com.gestcar.ui.viewmodel.conceptosGastoPredefinidos

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioGasto(
    vehiculoId: String,
    gastoId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: GastoPeriodicoViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val esNuevo = gastoId == "nuevo"
    val gasto = estado.gasto
    var intentoGuardar by remember { mutableStateOf(false) }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    val conceptoVacio = gasto.concepto.isBlank()
    val importeVacio = gasto.importe <= 0
    val faltaConcepto = intentoGuardar && conceptoVacio
    val faltaImporte = intentoGuardar && importeVacio
    val faltanCamposObligatorios = conceptoVacio || importeVacio

    LaunchedEffect(vehiculoId, gastoId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(gastoId)
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
                title = { Text(if (esNuevo) "Nuevo gasto" else "Editar gasto") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                value = gasto.concepto,
                onValueChange = { viewModel.actualizarFormulario(gasto.copy(concepto = it)) },
                label = { Text("Concepto *") },
                isError = faltaConcepto,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.clickable { mostrarSugerencias = !mostrarSugerencias },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sugerencias",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (mostrarSugerencias) "▼" else "▶",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (mostrarSugerencias) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    conceptosGastoPredefinidos.forEach { sugerencia ->
                        FilterChip(
                            selected = gasto.concepto == sugerencia,
                            onClick = { viewModel.actualizarFormulario(gasto.copy(concepto = sugerencia)) },
                            label = { Text(sugerencia) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = if (gasto.importe > 0) gasto.importe.toString() else "",
                onValueChange = {
                    viewModel.actualizarFormulario(
                        gasto.copy(importe = it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                    )
                },
                label = { Text("Importe *") },
                isError = faltaImporte,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            CampoFecha(
                etiqueta = "Fecha *",
                fecha = gasto.fecha,
                alSeleccionarFecha = {
                    viewModel.actualizarFormulario(gasto.copy(fecha = it))
                },
                modifier = Modifier.fillMaxWidth()
            )

            SelectorDesplegable(
                etiqueta = "Periodicidad",
                valorSeleccionado = textoPeriodicidad(gasto.periodicidad),
                opciones = opcionesPeriodicidad(),
                alSeleccionar = {
                    viewModel.actualizarFormulario(gasto.copy(periodicidad = valorPeriodicidad(it)))
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Añadir vencimiento",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = gasto.fechaVencimiento != null,
                    onCheckedChange = { activado ->
                        viewModel.actualizarFormulario(
                            gasto.copy(
                                fechaVencimiento = if (activado) {
                                    gasto.fechaVencimiento ?: System.currentTimeMillis()
                                } else {
                                    null
                                }
                            )
                        )
                    }
                )
            }

            gasto.fechaVencimiento?.let { vencimiento ->
                CampoFecha(
                    etiqueta = "Fecha de vencimiento",
                    fecha = vencimiento,
                    alSeleccionarFecha = {
                        viewModel.actualizarFormulario(gasto.copy(fechaVencimiento = it))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = gasto.notas ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(gasto.copy(notas = it.ifBlank { null }))
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
                        viewModel.guardarGasto()
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
                    Text("Guardar gasto")
                }
            }

            if (intentoGuardar && faltanCamposObligatorios) {
                Snackbar {
                    Text("Faltan campos obligatorios por rellenar")
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

private fun opcionesPeriodicidad(): List<String> = listOf(
    "Único",
    "Mensual",
    "Trimestral",
    "Semestral",
    "Anual"
)

private fun textoPeriodicidad(periodicidad: String?): String {
    return when (periodicidad) {
        PERIODICIDAD_MENSUAL -> "Mensual"
        PERIODICIDAD_TRIMESTRAL -> "Trimestral"
        PERIODICIDAD_SEMESTRAL -> "Semestral"
        PERIODICIDAD_ANUAL -> "Anual"
        else -> "Único"
    }
}

private fun valorPeriodicidad(texto: String): String {
    return when (texto) {
        "Mensual" -> PERIODICIDAD_MENSUAL
        "Trimestral" -> PERIODICIDAD_TRIMESTRAL
        "Semestral" -> PERIODICIDAD_SEMESTRAL
        "Anual" -> PERIODICIDAD_ANUAL
        else -> PERIODICIDAD_UNICO
    }
}
