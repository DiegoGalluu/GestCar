package com.gestcar.ui.componentes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gestcar.datos.entidades.Vehiculo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorVehiculoActivo(
    vehiculos: List<Vehiculo>,
    vehiculoActivo: Vehiculo?,
    alSeleccionar: (Vehiculo) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandido by remember { mutableStateOf(false) }
    val textoVehiculo = vehiculoActivo?.let { "${it.marca} ${it.modelo}" } ?: "Sin vehículos"

    if (vehiculos.size <= 1) {
        OutlinedTextField(
            value = textoVehiculo,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text("Vehículo activo") },
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = modifier.fillMaxWidth()
        )
        return
    }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = textoVehiculo,
            onValueChange = {},
            readOnly = true,
            label = { Text("Vehículo activo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            vehiculos.forEach { vehiculo ->
                DropdownMenuItem(
                    text = { Text("${vehiculo.marca} ${vehiculo.modelo}") },
                    onClick = {
                        alSeleccionar(vehiculo)
                        expandido = false
                    }
                )
            }
        }
    }
}
