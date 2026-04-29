package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.gestcar.ui.viewmodel.AutenticacionViewModel

@Composable
fun PantallaRegistro(
    alRegistrarse: () -> Unit,
    alVolverALogin: () -> Unit,
    viewModel: AutenticacionViewModel
) {
    val estado by viewModel.estado.collectAsState()

    var email by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var mostrarContrasena by remember { mutableStateOf(false) }

    LaunchedEffect(estado.estaAutenticado) {
        if (estado.estaAutenticado) {
            alRegistrarse()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Crear cuenta",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Regístrate para empezar a gestionar tus vehículos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None
            ),
            visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                    Icon(
                        imageVector = if (mostrarContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmarContrasena,
            onValueChange = { confirmarContrasena = it },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None
            ),
            visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                    Icon(
                        imageVector = if (mostrarContrasena) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.registrarse(email, contrasena) },
            enabled = !estado.estaCargando &&
                email.isNotBlank() &&
                contrasena.isNotBlank() &&
                contrasena == confirmarContrasena,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (estado.estaCargando) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Registrarse")
            }
        }

        if (confirmarContrasena.isNotBlank() && contrasena != confirmarContrasena) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Las contraseñas no coinciden",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = alVolverALogin) {
            Text("¿Ya tienes cuenta? Inicia sesión.")
        }

        estado.mensajeError?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.limpiarError() }) {
                        Text("Cerrar")
                    }
                }
            ) {
                Text(error)
            }
        }
    }
}
