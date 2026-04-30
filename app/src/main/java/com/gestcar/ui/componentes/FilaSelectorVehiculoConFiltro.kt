package com.gestcar.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gestcar.datos.entidades.Vehiculo

@Composable
fun FilaSelectorVehiculoConFiltro(
    vehiculos: List<Vehiculo>,
    vehiculoActivo: Vehiculo?,
    alSeleccionarVehiculo: (Vehiculo) -> Unit,
    alPulsarFiltro: () -> Unit,
    modifier: Modifier = Modifier,
    filtroActivo: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectorVehiculoActivo(
            vehiculos = vehiculos,
            vehiculoActivo = vehiculoActivo,
            alSeleccionar = alSeleccionarVehiculo,
            modifier = Modifier.weight(1f)
        )

        FilledTonalIconButton(
            onClick = alPulsarFiltro,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (filtroActivo) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filtrar por periodo"
            )
        }
    }
}
