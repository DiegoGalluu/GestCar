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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION

@Composable
fun TarjetaMantenimiento(
    mantenimiento: Mantenimiento,
    indiceColor: Int = 0,
    colorCategoria: Color = MaterialTheme.colorScheme.secondary,
    alPulsar: () -> Unit
) {
    val esReparacion = mantenimiento.categoria == CATEGORIA_REPARACION

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alPulsar() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorFondoTarjetaUsuario(indiceColor)
        )
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
                tint = colorCategoria
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
                text = textoCosteMantenimiento(mantenimiento),
                style = MaterialTheme.typography.titleMedium,
                color = colorCategoria
            )
        }
    }
}

private fun textoDetalleMantenimiento(mantenimiento: Mantenimiento): String {
    val kilometros = mantenimiento.kilometros
        ?.let { " · ${String.format("%,.0f", it)} km" }
        .orEmpty()
    val estado = if (mantenimiento.realizado) "Realizada" else "Pendiente"
    return "$estado · ${formatearFechaCorta(mantenimiento.fecha)}$kilometros"
}

private fun textoCosteMantenimiento(mantenimiento: Mantenimiento): String {
    return when {
        mantenimiento.coste > 0 -> "${String.format("%.2f", mantenimiento.coste)} €"
        mantenimiento.realizado -> "Sin coste"
        else -> "Sin definir"
    }
}
