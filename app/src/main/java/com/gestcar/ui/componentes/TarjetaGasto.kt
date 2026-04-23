package com.gestcar.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gestcar.datos.entidades.GastoPeriodico

@Composable
fun TarjetaGasto(
    gasto: GastoPeriodico,
    alPulsar: () -> Unit
) {
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
                imageVector = Icons.Default.Payments,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gasto.concepto,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = textoDetalleGasto(gasto),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                gasto.fechaVencimiento?.let { vencimiento ->
                    Text(
                        text = "Vence: ${formatearFechaCorta(vencimiento)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "${String.format("%.2f", gasto.importe)} €",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

private fun textoDetalleGasto(gasto: GastoPeriodico): String {
    val periodicidad = gasto.periodicidad
        ?.lowercase()
        ?.replaceFirstChar { it.uppercase() }
        ?: "Sin periodicidad"

    return "${formatearFechaCorta(gasto.fecha)} · $periodicidad"
}
