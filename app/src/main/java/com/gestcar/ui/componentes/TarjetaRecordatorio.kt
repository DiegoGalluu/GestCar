package com.gestcar.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
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
import com.gestcar.datos.entidades.Recordatorio

enum class EstadoVisualRecordatorio {
    VENCIDO,
    PROXIMO,
    PENDIENTE,
    COMPLETADO
}

@Composable
fun TarjetaRecordatorio(
    recordatorio: Recordatorio,
    estadoVisual: EstadoVisualRecordatorio,
    alPulsar: () -> Unit
) {
    val colorEstado = colorEstadoRecordatorio(estadoVisual)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { alPulsar() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(84.dp)
                    .background(colorEstado, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (estadoVisual) {
                        EstadoVisualRecordatorio.COMPLETADO -> Icons.Default.CheckCircle
                        EstadoVisualRecordatorio.VENCIDO -> Icons.Default.Warning
                        else -> Icons.Default.Notifications
                    },
                    contentDescription = null,
                    tint = colorEstado
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recordatorio.concepto,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = textoDetalleRecordatorio(recordatorio, estadoVisual),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun colorEstadoRecordatorio(estadoVisual: EstadoVisualRecordatorio): Color {
    return when (estadoVisual) {
        EstadoVisualRecordatorio.VENCIDO -> MaterialTheme.colorScheme.error
        EstadoVisualRecordatorio.PROXIMO -> Color(0xFFF9A825)
        EstadoVisualRecordatorio.PENDIENTE -> Color(0xFF2E8B57)
        EstadoVisualRecordatorio.COMPLETADO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun textoDetalleRecordatorio(
    recordatorio: Recordatorio,
    estadoVisual: EstadoVisualRecordatorio
): String {
    val partes = mutableListOf<String>()

    recordatorio.fechaLimite?.let { fecha ->
        partes.add("Fecha: ${formatearFechaCorta(fecha)}")
    }

    recordatorio.kilometrajeLimite?.let { kilometros ->
        partes.add("Km: ${String.format("%,.0f", kilometros)}")
    }

    if (partes.isEmpty()) {
        partes.add("Sin limite definido")
    }

    partes.add(
        when (estadoVisual) {
            EstadoVisualRecordatorio.VENCIDO -> "Vencido"
            EstadoVisualRecordatorio.PROXIMO -> "Próximo"
            EstadoVisualRecordatorio.PENDIENTE -> "Pendiente"
            EstadoVisualRecordatorio.COMPLETADO -> "Completado"
        }
    )

    return partes.joinToString(" · ")
}
