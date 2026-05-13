package com.gestcar.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestcar.ui.viewmodel.PeriodoRepostajes
import java.util.Calendar

@Composable
fun DialogoFiltroPeriodo(
    titulo: String,
    periodoSeleccionado: PeriodoRepostajes,
    alSeleccionarPeriodo: (PeriodoRepostajes) -> Unit,
    alCancelar: () -> Unit
) {
    // usamos el mismo dialogo para repostajes y otras secciones filtrables
    // asi el comportamiento temporal es consistente en toda la app
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
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = null,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = titulo,
                    color = Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color.Black.copy(alpha = 0.16f))
                opciones.forEach { (periodo, etiqueta) ->
                    FilaOpcionPeriodo(
                        etiqueta = etiqueta,
                        seleccionada = periodoSeleccionado == periodo,
                        alPulsar = { alSeleccionarPeriodo(periodo) }
                    )
                    if (periodo != opciones.last().first) {
                        HorizontalDivider(
                            color = Color.Black.copy(alpha = 0.16f)
                        )
                    }
                }
                HorizontalDivider(color = Color.Black.copy(alpha = 0.16f))
                FilaCerrarFiltro(alCancelar)
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun FilaOpcionPeriodo(
    etiqueta: String,
    seleccionada: Boolean,
    alPulsar: () -> Unit
) {
    Text(
        text = etiqueta,
        color = if (seleccionada) Color(0xFF1F4E79) else Color.Black,
        fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .background(if (seleccionada) Color(0xFFEAF4FF) else Color.White)
            .clickable(onClick = alPulsar)
            .padding(horizontal = 22.dp, vertical = 16.dp)
    )
}

@Composable
private fun FilaCerrarFiltro(alCancelar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = alCancelar)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Cerrar",
            color = Color(0xFF1F4E79),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DialogoRangoPeriodo(
    fechaInicioInicial: Long,
    fechaFinInicial: Long,
    alConfirmar: (Long, Long) -> Unit,
    alCancelar: () -> Unit
) {
    // guardamos el rango dentro del dialogo hasta que el usuario pulsa aplicar
    // cancelar no cambia el filtro que ya tenia la pantalla
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
    // si el periodo es todo no hay rango y por tanto todos los registros pasan el filtro
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

    // los rangos se calculan desde el inicio del dia hasta el final del dia
    // esto evita perder registros por diferencias invisibles de hora
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
