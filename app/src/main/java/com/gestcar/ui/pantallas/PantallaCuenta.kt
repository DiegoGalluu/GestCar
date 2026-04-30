package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.CuentaViewModel

@Composable
fun PantallaCuenta(
    correoUsuario: String,
    usuarioId: String,
    alVolver: () -> Unit,
    alCuentaEliminada: () -> Unit,
    viewModel: CuentaViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var mostrarPrimerAviso by remember { mutableStateOf(false) }
    var mostrarConfirmacionFinal by remember { mutableStateOf(false) }

    LaunchedEffect(estado.cuentaEliminada) {
        if (estado.cuentaEliminada) {
            alCuentaEliminada()
        }
    }

    LaunchedEffect(estado.mensajeError) {
        estado.mensajeError?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Cuenta",
                alVolver = alVolver
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ListItem(
                        headlineContent = { Text("Correo electrónico") },
                        supportingContent = {
                            Text(correoUsuario.ifBlank { "No disponible" })
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    HorizontalDivider()

                    Text(
                        text = if (estado.estaEliminando) {
                            "Eliminando cuenta..."
                        } else {
                            "Eliminar cuenta"
                        },
                        color = Color(0xFFB3261E),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !estado.estaEliminando) {
                                mostrarPrimerAviso = true
                            }
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    )
                }
            }
        }
    }

    if (mostrarPrimerAviso) {
        AlertDialog(
            onDismissRequest = { mostrarPrimerAviso = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFB3261E)
                )
            },
            title = { Text("Eliminar cuenta y datos") },
            text = {
                Text("Eliminar tu cuenta supone eliminar todos los datos asociados a tus vehículos.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarPrimerAviso = false
                        mostrarConfirmacionFinal = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarPrimerAviso = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarConfirmacionFinal) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionFinal = false },
            title = { Text("¿Estás seguro?") },
            text = {
                Text("Esta acción no se puede deshacer. Se eliminará tu cuenta y todos sus datos asociados.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarConfirmacionFinal = false
                        viewModel.eliminarCuenta(usuarioId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
                ) {
                    Text("Estoy seguro")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mostrarConfirmacionFinal = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
