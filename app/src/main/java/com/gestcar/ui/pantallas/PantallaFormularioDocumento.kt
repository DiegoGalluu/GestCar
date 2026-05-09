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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.DocumentacionViewModel

@Composable
fun PantallaFormularioDocumento(
    vehiculoId: String,
    documentoId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: DocumentacionViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val documento = estado.documento
    val esNuevo = documentoId == "nuevo"
    var intentoGuardar by remember { mutableStateOf(false) }
    val tituloVacio = documento.titulo.isBlank()

    LaunchedEffect(vehiculoId, documentoId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(documentoId)
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
                titulo = if (esNuevo) "Nueva documentación" else "Editar documentación",
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
                value = documento.titulo,
                onValueChange = { viewModel.actualizarDocumento(documento.copy(titulo = it)) },
                label = { Text("Título *") },
                isError = intentoGuardar && tituloVacio,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = documento.notas ?: "",
                onValueChange = {
                    viewModel.actualizarDocumento(documento.copy(notas = it.ifBlank { null }))
                },
                label = { Text("Notas") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Campos personalizados",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = { viewModel.anadirCampo() }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Añadir")
                }
            }

            estado.campos.forEachIndexed { indice, campo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Campo ${indice + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IconButton(
                                onClick = { viewModel.eliminarCampo(campo.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar campo"
                                )
                            }
                        }

                        OutlinedTextField(
                            value = campo.nombre,
                            onValueChange = { viewModel.actualizarCampo(campo.id, nombre = it) },
                            label = { Text("Nombre del campo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = campo.valor,
                            onValueChange = { viewModel.actualizarCampo(campo.id, valor = it) },
                            label = { Text("Valor o descripción") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    intentoGuardar = true
                    if (!tituloVacio) {
                        viewModel.guardarDocumento()
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
                    Text("Guardar documentación")
                }
            }

            if (intentoGuardar && tituloVacio) {
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
