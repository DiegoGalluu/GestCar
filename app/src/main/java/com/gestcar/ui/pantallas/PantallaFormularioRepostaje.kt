package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.viewmodel.RepostajeViewModel
import com.gestcar.util.limpiarEntradaDecimal
import com.gestcar.util.parsearDecimalFlexible
import com.gestcar.util.textoDecimalEditable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioRepostaje(
    vehiculoId: String,
    repostajeId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: RepostajeViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val esNuevo = repostajeId == "nuevo"
    val repostaje = estado.repostaje
    var intentoGuardar by remember { mutableStateOf(false) }
    var litrosTexto by remember { mutableStateOf("") }
    var precioPorLitroTexto by remember { mutableStateOf("") }
    val kilometrosVacios = repostaje.kilometros <= 0
    val litrosVacios = repostaje.litros <= 0
    val precioPorLitroVacio = repostaje.precioPorLitro <= 0
    val faltaKilometros = intentoGuardar && kilometrosVacios
    val faltaLitros = intentoGuardar && litrosVacios
    val faltaPrecioPorLitro = intentoGuardar && precioPorLitroVacio
    val faltanCamposObligatorios = kilometrosVacios || litrosVacios || precioPorLitroVacio

    LaunchedEffect(vehiculoId, repostajeId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(repostajeId)
        }
    }

    LaunchedEffect(repostaje.id) {
        litrosTexto = textoDecimalEditable(repostaje.litros)
        precioPorLitroTexto = textoDecimalEditable(repostaje.precioPorLitro)
    }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            alGuardar()
        }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = if (esNuevo) "Nuevo repostaje" else "Editar repostaje",
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
            CampoFecha(
                etiqueta = "Fecha *",
                fecha = repostaje.fecha,
                alSeleccionarFecha = {
                    viewModel.actualizarFormulario(repostaje.copy(fecha = it))
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = if (repostaje.kilometros > 0) repostaje.kilometros.toLong().toString() else "",
                onValueChange = {
                    viewModel.actualizarFormulario(
                        repostaje.copy(kilometros = it.toDoubleOrNull() ?: 0.0)
                    )
                },
                label = { Text("Kilómetros actuales *") },
                isError = faltaKilometros,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = litrosTexto,
                onValueChange = {
                    val textoLimpio = limpiarEntradaDecimal(it)
                    litrosTexto = textoLimpio
                    viewModel.actualizarFormulario(
                        repostaje.copy(litros = parsearDecimalFlexible(textoLimpio) ?: 0.0)
                    )
                },
                label = { Text("Litros *") },
                isError = faltaLitros,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = precioPorLitroTexto,
                onValueChange = {
                    val textoLimpio = limpiarEntradaDecimal(it)
                    precioPorLitroTexto = textoLimpio
                    viewModel.actualizarFormulario(
                        repostaje.copy(precioPorLitro = parsearDecimalFlexible(textoLimpio) ?: 0.0)
                    )
                },
                label = { Text("Precio por litro *") },
                isError = faltaPrecioPorLitro,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = "${String.format("%.2f", repostaje.importeTotal)} €",
                onValueChange = {},
                readOnly = true,
                label = { Text("Importe total") },
                modifier = Modifier.fillMaxWidth()
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Depósito lleno",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = repostaje.llenoCompleto,
                    onCheckedChange = {
                        viewModel.actualizarFormulario(repostaje.copy(llenoCompleto = it))
                    }
                )
            }

            OutlinedTextField(
                value = repostaje.gasolinera ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(repostaje.copy(gasolinera = it.ifBlank { null }))
                },
                label = { Text("Gasolinera") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = repostaje.notas ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(repostaje.copy(notas = it.ifBlank { null }))
                },
                label = { Text("Comentarios") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    intentoGuardar = true
                    if (!faltanCamposObligatorios) {
                        viewModel.guardarRepostaje()
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
                    Text("Guardar repostaje")
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

    if (estado.necesitaConfirmarKilometrajeMenor) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelarConfirmacionKilometrajeMenor() },
            title = { Text("Kilometraje inferior") },
            text = {
                Text("El kilometraje indicado es menor que el último registrado. ¿Quieres guardar el repostaje de todos modos?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.guardarRepostaje(permitirKilometrajeMenor = true) }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelarConfirmacionKilometrajeMenor() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
