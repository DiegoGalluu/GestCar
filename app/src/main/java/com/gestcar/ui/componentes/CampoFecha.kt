package com.gestcar.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: Long,
    alSeleccionarFecha: (Long) -> Unit,
    modifier: Modifier = Modifier,
    esError: Boolean = false
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = formatearFechaCorta(fecha),
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = true,
            isError = esError
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { mostrarDialogo = true }
        )
    }

    if (mostrarDialogo) {
        val estadoFecha = rememberDatePickerState(initialSelectedDateMillis = fecha)
        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estadoFecha.selectedDateMillis?.let(alSeleccionarFecha)
                        mostrarDialogo = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}

fun formatearFechaCorta(timestamp: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
    return formato.format(Date(timestamp))
}
