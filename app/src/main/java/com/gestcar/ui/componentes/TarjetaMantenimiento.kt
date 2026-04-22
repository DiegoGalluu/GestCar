package com.gestcar.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION

@Composable
fun TarjetaMantenimiento(
    mantenimiento: Mantenimiento,
    alPulsar: () -> Unit
) {
    val esReparacion = mantenimiento.categoria == CATEGORIA_REPARACION

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alPulsar() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (esReparacion) Icons.Default.Warning else Icons.Default.Build,
                contentDescription = null,
                tint = if (esReparacion) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mantenimiento.tipo,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = textoDetalleMantenimiento(mantenimiento),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                mantenimiento.taller?.takeIf { it.isNotBlank() }?.let { taller ->
                    Text(
                        text = taller,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${String.format("%.2f", mantenimiento.coste)} €",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun textoDetalleMantenimiento(mantenimiento: Mantenimiento): String {
    val kilometros = mantenimiento.kilometros
        ?.let { " · ${String.format("%,.0f", it)} km" }
        .orEmpty()
    return "${formatearFechaCorta(mantenimiento.fecha)}$kilometros"
}
