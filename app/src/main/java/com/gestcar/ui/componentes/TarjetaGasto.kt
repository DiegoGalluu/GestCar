package com.gestcar.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
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
import com.gestcar.datos.entidades.GastoPeriodico
import java.util.concurrent.TimeUnit

enum class EstadoVisualGasto {
    VENCIDO,
    PROXIMO,
    AL_DIA,
    SIN_VENCIMIENTO,
    PAGADO
}

@Composable
fun TarjetaGasto(
    gasto: GastoPeriodico,
    indiceColor: Int = 0,
    alPulsar: () -> Unit
) {
    val estadoVisual = calcularEstadoVisualGasto(gasto)
    val colorEstado = colorEstadoGasto(estadoVisual)

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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(96.dp)
                    .background(
                        color = colorEstado,
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
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
                        EstadoVisualGasto.VENCIDO -> Icons.Default.Warning
                        EstadoVisualGasto.PROXIMO -> Icons.Default.Schedule
                        EstadoVisualGasto.PAGADO -> Icons.Default.CheckCircle
                        else -> Icons.Default.Payments
                    },
                    contentDescription = null,
                    tint = colorEstado
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
                    Text(
                        text = textoEstadoGasto(gasto, estadoVisual),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorEstado
                    )
                }

                Text(
                    text = "${String.format("%.2f", gasto.importe)} €",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
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

fun calcularEstadoVisualGasto(
    gasto: GastoPeriodico,
    ahora: Long = System.currentTimeMillis()
): EstadoVisualGasto {
    if (gasto.pagado) {
        return EstadoVisualGasto.PAGADO
    }

    val vencimiento = gasto.fechaVencimiento ?: return EstadoVisualGasto.SIN_VENCIMIENTO
    val inicioHoy = ahora - (ahora % TimeUnit.DAYS.toMillis(1))
    val diasRestantes = TimeUnit.MILLISECONDS.toDays(vencimiento - inicioHoy)

    return when {
        diasRestantes < 0 -> EstadoVisualGasto.VENCIDO
        diasRestantes <= 30 -> EstadoVisualGasto.PROXIMO
        else -> EstadoVisualGasto.AL_DIA
    }
}

@Composable
fun colorEstadoGasto(estadoVisual: EstadoVisualGasto): Color {
    return when (estadoVisual) {
        EstadoVisualGasto.VENCIDO -> MaterialTheme.colorScheme.error
        EstadoVisualGasto.PROXIMO -> Color(0xFFF9A825)
        EstadoVisualGasto.AL_DIA -> Color(0xFF2E8B57)
        EstadoVisualGasto.SIN_VENCIMIENTO -> MaterialTheme.colorScheme.onSurfaceVariant
        EstadoVisualGasto.PAGADO -> MaterialTheme.colorScheme.primary
    }
}

fun textoEstadoGasto(
    gasto: GastoPeriodico,
    estadoVisual: EstadoVisualGasto,
    ahora: Long = System.currentTimeMillis()
): String {
    if (estadoVisual == EstadoVisualGasto.PAGADO) {
        return gasto.fechaPago?.let { "Pagado el ${formatearFechaCorta(it)}" } ?: "Pagado"
    }

    val vencimiento = gasto.fechaVencimiento ?: return "Sin vencimiento definido"
    val inicioHoy = ahora - (ahora % TimeUnit.DAYS.toMillis(1))
    val diasRestantes = TimeUnit.MILLISECONDS.toDays(vencimiento - inicioHoy)

    return when (estadoVisual) {
        EstadoVisualGasto.VENCIDO -> "Vencido desde ${formatearFechaCorta(vencimiento)}"
        EstadoVisualGasto.PROXIMO -> when {
            diasRestantes <= 0 -> "Vence hoy"
            diasRestantes == 1L -> "Vence mañana"
            else -> "Vence en $diasRestantes días"
        }
        EstadoVisualGasto.AL_DIA -> "Al día hasta ${formatearFechaCorta(vencimiento)}"
        EstadoVisualGasto.SIN_VENCIMIENTO -> "Sin vencimiento definido"
        EstadoVisualGasto.PAGADO -> "Pagado"
    }
}
