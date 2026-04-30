package com.gestcar.ui.componentes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gestcar.ui.viewmodel.PeriodoRepostajes
import java.util.Calendar

@Composable
fun DialogoFiltroPeriodo(
    titulo: String,
    periodoSeleccionado: PeriodoRepostajes,
    alSeleccionarPeriodo: (PeriodoRepostajes) -> Unit,
    alCancelar: () -> Unit
) {
    val opciones = listOf(
        PeriodoRepostajes.HOY to "Hoy",
        PeriodoRepostajes.SEMANA to "Semana",
        PeriodoRepostajes.MES to "Mes",
        PeriodoRepostajes.ANIO to "A\u00F1o",
        PeriodoRepostajes.TODO to "Todo",
        PeriodoRepostajes.PERSONALIZADO to "Personalizado"
    )

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                opciones.forEach { (periodo, etiqueta) ->
                    FilterChip(
                        selected = periodoSeleccionado == periodo,
                        onClick = { alSeleccionarPeriodo(periodo) },
                        label = { Text(etiqueta) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun DialogoRangoPeriodo(
    fechaInicioInicial: Long,
    fechaFinInicial: Long,
    alConfirmar: (Long, Long) -> Unit,
    alCancelar: () -> Unit
) {
    var fechaInicio by remember { mutableStateOf(fechaInicioInicial) }
    var fechaFin by remember { mutableStateOf(fechaFinInicial) }

    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Rango personalizado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoFecha(
                    etiqueta = "Desde",
                    fecha = fechaInicio,
                    alSeleccionarFecha = { fechaInicio = it }
                )
                CampoFecha(
                    etiqueta = "Hasta",
                    fecha = fechaFin,
                    alSeleccionarFecha = { fechaFin = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { alConfirmar(fechaInicio, fechaFin) }) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cancelar")
            }
        }
    )
}

fun fechaDentroDePeriodo(
    fecha: Long,
    periodo: PeriodoRepostajes,
    fechaInicioPersonalizada: Long?,
    fechaFinPersonalizada: Long?
): Boolean {
    val rango = obtenerRangoPeriodo(periodo, fechaInicioPersonalizada, fechaFinPersonalizada)
        ?: return true

    return fecha in rango.first..rango.second
}

fun hayFiltroPeriodoActivo(periodo: PeriodoRepostajes): Boolean {
    return periodo != PeriodoRepostajes.TODO
}

private fun obtenerRangoPeriodo(
    periodo: PeriodoRepostajes,
    fechaInicioPersonalizada: Long?,
    fechaFinPersonalizada: Long?
): Pair<Long, Long>? {
    val calendario = Calendar.getInstance()

    return when (periodo) {
        PeriodoRepostajes.TODO -> null
        PeriodoRepostajes.HOY -> inicioYFin(calendario)
        PeriodoRepostajes.SEMANA -> {
            calendario.firstDayOfWeek = Calendar.MONDAY
            calendario.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val inicio = inicioDelDia(calendario).timeInMillis
            Calendar.getInstance().let { hoy -> inicio to finDelDia(hoy).timeInMillis }
        }
        PeriodoRepostajes.MES -> {
            calendario.set(Calendar.DAY_OF_MONTH, 1)
            val inicio = inicioDelDia(calendario).timeInMillis
            Calendar.getInstance().let { hoy -> inicio to finDelDia(hoy).timeInMillis }
        }
        PeriodoRepostajes.ANIO -> {
            calendario.set(Calendar.DAY_OF_YEAR, 1)
            val inicio = inicioDelDia(calendario).timeInMillis
            Calendar.getInstance().let { hoy -> inicio to finDelDia(hoy).timeInMillis }
        }
        PeriodoRepostajes.PERSONALIZADO -> {
            val inicio = fechaInicioPersonalizada ?: return null
            val fin = fechaFinPersonalizada ?: return null
            val inicioNormalizado = inicioDelDia(Calendar.getInstance().apply { timeInMillis = minOf(inicio, fin) }).timeInMillis
            val finNormalizado = finDelDia(Calendar.getInstance().apply { timeInMillis = maxOf(inicio, fin) }).timeInMillis
            inicioNormalizado to finNormalizado
        }
    }
}

private fun inicioYFin(calendario: Calendar): Pair<Long, Long> {
    return inicioDelDia(calendario).timeInMillis to finDelDia(calendario).timeInMillis
}

private fun inicioDelDia(calendario: Calendar): Calendar {
    return calendario.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun finDelDia(calendario: Calendar): Calendar {
    return calendario.apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }
}
