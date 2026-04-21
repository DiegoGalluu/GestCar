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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.viewmodel.RepostajeViewModel

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

    LaunchedEffect(vehiculoId, repostajeId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(repostajeId)
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
                title = { Text(if (esNuevo) "Nuevo repostaje" else "Editar repostaje") },
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
            CampoFecha(
                etiqueta = "Fecha (obligatorio)",
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
                label = { Text("Kilómetros actuales (obligatorio)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = if (repostaje.litros > 0) repostaje.litros.toString() else "",
                onValueChange = {
                    viewModel.actualizarFormulario(
                        repostaje.copy(litros = it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                    )
                },
                label = { Text("Litros (obligatorio)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = if (repostaje.precioPorLitro > 0) repostaje.precioPorLitro.toString() else "",
                onValueChange = {
                    viewModel.actualizarFormulario(
                        repostaje.copy(precioPorLitro = it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                    )
                },
                label = { Text("Precio por litro (obligatorio)") },
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
                label = { Text("Gasolinera (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = repostaje.notas ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(repostaje.copy(notas = it.ifBlank { null }))
                },
                label = { Text("Notas (opcional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.guardarRepostaje() },
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
