package com.gestcar.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.entidades.Repostaje
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.viewmodel.CATEGORIA_MANTENIMIENTO
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class DatosExportacionVehiculo(
    val vehiculo: Vehiculo,
    val repostajes: List<Repostaje>,
    val mantenimientos: List<Mantenimiento>,
    val gastos: List<GastoPeriodico>
)

enum class FormatoExportacionVehiculo {
    CSV,
    PDF,
    EXCEL,
    RESUMEN
}

object ExportadorDatosVehiculo {

    private val localeEs = Locale("es", "ES")
    private val formatoFecha = SimpleDateFormat("dd/MM/yyyy", localeEs)

    fun crearIntentExportacion(contexto: Context, datos: DatosExportacionVehiculo, formato: FormatoExportacionVehiculo): Intent {
        return when (formato) {
            FormatoExportacionVehiculo.CSV -> crearIntentArchivo(
                contexto = contexto,
                archivo = crearCsv(contexto, datos),
                mime = "text/csv"
            )
            FormatoExportacionVehiculo.PDF -> crearIntentArchivo(
                contexto = contexto,
                archivo = crearPdf(contexto, datos),
                mime = "application/pdf"
            )
            FormatoExportacionVehiculo.EXCEL -> crearIntentArchivo(
                contexto = contexto,
                archivo = crearExcelCompatible(contexto, datos),
                mime = "application/vnd.ms-excel"
            )
            FormatoExportacionVehiculo.RESUMEN -> Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, crearResumenCompartible(datos))
            }
        }
    }

    fun crearResumenCompartible(datos: DatosExportacionVehiculo): String {
        val vehiculo = datos.vehiculo
        val lineas = mutableListOf<String>()
        val ultimasOperaciones = ultimasOperaciones(datos).take(3)

        lineas += "${vehiculo.marca} ${vehiculo.modelo}".trim()
        lineas += "Matrícula: ${vehiculo.matricula}"
        lineas += ""
        lineas += "Kilometraje actual: ${formatearKilometros(vehiculo.kilometraje)}"
        lineas += "Fecha de fabricación: ${formatearFechaFabricacion(vehiculo)}"
        lineas += "Combustible: ${vehiculo.tipoCombustible?.ifBlank { null } ?: "Sin indicar"}"
        lineas += ""
        lineas += "Consumo medio actual: ${formatearConsumo(datos.repostajes)}"
        lineas += "Coste medio de combustible: ${formatearCosteMedioCombustible(datos.repostajes)}"
        lineas += "Gasto total registrado: ${formatearImporte(calcularGastoTotalRegistrado(datos))}"

        if (ultimasOperaciones.isNotEmpty()) {
            lineas += ""
            lineas += "Últimas operaciones:"
            lineas += ultimasOperaciones.map { "- $it" }
        }

        return lineas.joinToString("\n")
    }

    private fun crearCsv(contexto: Context, datos: DatosExportacionVehiculo): File {
        val filas = operacionesExportables(datos)
        val contenido = buildString {
            appendLine(listOf("Fecha", "Tipo", "Concepto", "Importe", "Kilómetros", "Estado", "Detalles").aCsv())
            filas.forEach { fila ->
                appendLine(
                    listOf(
                        formatearFecha(fila.fecha),
                        fila.tipo,
                        fila.concepto,
                        formatearImporte(fila.importe),
                        fila.kilometros?.let { formatearNumero(it) }.orEmpty(),
                        fila.estado,
                        fila.detalles
                    ).aCsv()
                )
            }
        }

        return crearArchivoExportacion(contexto, datos.vehiculo, "csv").also { it.writeText(contenido) }
    }

    private fun crearExcelCompatible(contexto: Context, datos: DatosExportacionVehiculo): File {
        val filas = operacionesExportables(datos)
        val contenido = buildString {
            appendLine("<html><head><meta charset=\"UTF-8\"></head><body>")
            appendLine("<h2>${datos.vehiculo.marca.escapeHtml()} ${datos.vehiculo.modelo.escapeHtml()}</h2>")
            appendLine("<p>Matrícula: ${datos.vehiculo.matricula.escapeHtml()}</p>")
            appendLine("<table border=\"1\">")
            appendLine("<tr><th>Fecha</th><th>Tipo</th><th>Concepto</th><th>Importe</th><th>Kilómetros</th><th>Estado</th><th>Detalles</th></tr>")
            filas.forEach { fila ->
                appendLine(
                    "<tr>" +
                        "<td>${formatearFecha(fila.fecha)}</td>" +
                        "<td>${fila.tipo.escapeHtml()}</td>" +
                        "<td>${fila.concepto.escapeHtml()}</td>" +
                        "<td>${formatearImporte(fila.importe)}</td>" +
                        "<td>${fila.kilometros?.let { formatearNumero(it) }.orEmpty()}</td>" +
                        "<td>${fila.estado.escapeHtml()}</td>" +
                        "<td>${fila.detalles.escapeHtml()}</td>" +
                        "</tr>"
                )
            }
            appendLine("</table></body></html>")
        }

        return crearArchivoExportacion(contexto, datos.vehiculo, "xls").also { it.writeText(contenido) }
    }

    private fun crearPdf(contexto: Context, datos: DatosExportacionVehiculo): File {
        val archivo = crearArchivoExportacion(contexto, datos.vehiculo, "pdf")
        val documento = PdfDocument()
        val pinturaTitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val pinturaTexto = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val pinturaSeccion = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var numeroPagina = 1
        var pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create())
        var y = 42f

        fun nuevaPagina() {
            documento.finishPage(pagina)
            numeroPagina += 1
            pagina = documento.startPage(PdfDocument.PageInfo.Builder(595, 842, numeroPagina).create())
            y = 42f
        }

        fun escribirLinea(texto: String, pintura: Paint = pinturaTexto, salto: Float = 18f) {
            if (y > 800f) nuevaPagina()
            pagina.canvas.drawText(texto.take(95), 40f, y, pintura)
            y += salto
        }

        escribirLinea("${datos.vehiculo.marca} ${datos.vehiculo.modelo}", pinturaTitulo, 24f)
        escribirLinea("Matrícula: ${datos.vehiculo.matricula}")
        escribirLinea("Kilometraje actual: ${formatearKilometros(datos.vehiculo.kilometraje)}")
        escribirLinea("Fecha de fabricación: ${formatearFechaFabricacion(datos.vehiculo)}")
        escribirLinea("Combustible: ${datos.vehiculo.tipoCombustible?.ifBlank { null } ?: "Sin indicar"}")
        escribirLinea("Gasto total registrado: ${formatearImporte(calcularGastoTotalRegistrado(datos))} €")
        y += 10f

        escribirLinea("Operaciones registradas", pinturaSeccion, 22f)
        operacionesExportables(datos).forEach { fila ->
            escribirLinea("${formatearFecha(fila.fecha)} · ${fila.tipo} · ${fila.concepto} · ${formatearImporte(fila.importe)} €")
        }

        documento.finishPage(pagina)
        archivo.outputStream().use { documento.writeTo(it) }
        documento.close()
        return archivo
    }

    private fun crearIntentArchivo(contexto: Context, archivo: File, mime: String): Intent {
        val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun crearArchivoExportacion(contexto: Context, vehiculo: Vehiculo, extension: String): File {
        val carpeta = File(contexto.cacheDir, "exportaciones").apply { mkdirs() }
        val nombreBase = "GestCar_${vehiculo.marca}_${vehiculo.modelo}_${vehiculo.matricula}"
            .replace(Regex("[^A-Za-z0-9_-]+"), "_")
            .trim('_')
        return File(carpeta, "$nombreBase.$extension")
    }

    private fun operacionesExportables(datos: DatosExportacionVehiculo): List<FilaExportacion> {
        val repostajes = datos.repostajes.map {
            FilaExportacion(
                fecha = it.fecha,
                tipo = "Repostaje",
                concepto = it.gasolinera?.takeIf { gasolinera -> gasolinera.isNotBlank() } ?: "Combustible",
                importe = it.importeTotal,
                kilometros = it.kilometros,
                estado = if (it.llenoCompleto) "Depósito lleno" else "Parcial",
                detalles = "${formatearNumero(it.litros)} L a ${formatearImporte(it.precioPorLitro)} €/L"
            )
        }
        val mantenimientos = datos.mantenimientos.map {
            FilaExportacion(
                fecha = it.fechaRealizado ?: it.fecha,
                tipo = if (it.categoria == CATEGORIA_REPARACION) "Reparación" else "Mantenimiento",
                concepto = it.tipo,
                importe = it.coste,
                kilometros = it.kilometros,
                estado = if (it.realizado) "Realizada" else "Pendiente",
                detalles = listOfNotNull(it.taller, it.descripcion).joinToString(" · ")
            )
        }
        val gastos = datos.gastos.map {
            FilaExportacion(
                fecha = it.fechaPago ?: it.fecha,
                tipo = "Gasto",
                concepto = it.concepto,
                importe = it.importe,
                kilometros = null,
                estado = if (it.pagado) "Pagado" else "Pendiente",
                detalles = listOfNotNull(it.periodicidad, it.fechaVencimiento?.let { fecha -> "Vence ${formatearFecha(fecha)}" }).joinToString(" · ")
            )
        }

        return (repostajes + mantenimientos + gastos).sortedBy { it.fecha }
    }

    private fun ultimasOperaciones(datos: DatosExportacionVehiculo): List<String> {
        return operacionesExportables(datos)
            .sortedByDescending { it.fecha }
            .map { "${it.tipo}: ${it.concepto}, ${formatearImporte(it.importe)} € el ${formatearFecha(it.fecha)}" }
    }

    private fun calcularGastoTotalRegistrado(datos: DatosExportacionVehiculo): Double {
        val combustible = datos.repostajes.sumOf { it.importeTotal }
        val mantenimientosRealizados = datos.mantenimientos.filter { it.realizado }.sumOf { it.coste }
        val gastosPagados = datos.gastos.filter { it.pagado }.sumOf { it.importe }
        return combustible + mantenimientosRealizados + gastosPagados
    }

    private fun formatearConsumo(repostajes: List<Repostaje>): String {
        val consumo = calcularConsumoMedio(repostajes)
        return if (consumo > 0) "${formatearNumero(consumo)} L/100 km" else "Datos insuficientes"
    }

    private fun formatearCosteMedioCombustible(repostajes: List<Repostaje>): String {
        val coste = calcularCosteMedioCada100Km(repostajes)
        return if (coste > 0) "${formatearImporte(coste)} €/100 km" else "Datos insuficientes"
    }

    private fun calcularConsumoMedio(repostajes: List<Repostaje>): Double {
        val consumos = obtenerTramosValidos(repostajes).map { (actual, kilometrosRecorridos) ->
            (actual.litros / kilometrosRecorridos) * 100
        }
        return consumos.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun calcularCosteMedioCada100Km(repostajes: List<Repostaje>): Double {
        val costes = obtenerTramosValidos(repostajes).map { (actual, kilometrosRecorridos) ->
            (actual.importeTotal / kilometrosRecorridos) * 100
        }
        return costes.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun obtenerTramosValidos(repostajes: List<Repostaje>): List<TramoConsumo> {
        return repostajes
            .filter { it.llenoCompleto }
            .sortedBy { it.kilometros }
            .zipWithNext()
            .mapNotNull { (anterior, actual) ->
                val kilometrosRecorridos = actual.kilometros - anterior.kilometros
                if (kilometrosRecorridos > 0 && actual.litros > 0) {
                    TramoConsumo(actual, kilometrosRecorridos)
                } else {
                    null
                }
            }
    }

    private fun formatearFechaFabricacion(vehiculo: Vehiculo): String {
        val mes = vehiculo.mesFabricacion?.takeIf { it in 1..12 }?.let { meses[it - 1] }
        val dia = vehiculo.diaFabricacion?.takeIf { it in 1..31 }
        return when {
            dia != null && mes != null -> "$dia de $mes de ${vehiculo.anioFabricacion}"
            mes != null -> "${mes.replaceFirstChar { it.titlecase(localeEs) }} de ${vehiculo.anioFabricacion}"
            else -> vehiculo.anioFabricacion.toString()
        }
    }

    private fun formatearFecha(fecha: Long): String = formatoFecha.format(fecha)

    private fun formatearKilometros(valor: Double): String = "${formatearNumero(valor)} km"

    private fun formatearNumero(valor: Double): String = String.format(localeEs, "%,.0f", valor)

    private fun formatearImporte(valor: Double): String = String.format(localeEs, "%.2f", valor)

    private fun List<String>.aCsv(): String = joinToString(";") { valor ->
        "\"${valor.replace("\"", "\"\"")}\""
    }

    private fun String.escapeHtml(): String = replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private val meses = listOf(
        "enero",
        "febrero",
        "marzo",
        "abril",
        "mayo",
        "junio",
        "julio",
        "agosto",
        "septiembre",
        "octubre",
        "noviembre",
        "diciembre"
    )
}

private data class FilaExportacion(
    val fecha: Long,
    val tipo: String,
    val concepto: String,
    val importe: Double,
    val kilometros: Double?,
    val estado: String,
    val detalles: String
)

private data class TramoConsumo(
    val actual: Repostaje,
    val kilometrosRecorridos: Double
)
