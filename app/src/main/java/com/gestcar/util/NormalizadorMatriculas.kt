package com.gestcar.util

enum class TipoMatriculaDetectada {
    Actual,
    AntiguaNumerica,
    AntiguaAlfanumerica,
    Historica,
    Ciclomotor,
    Remolque,
    Especial,
    Temporal,
    NoReconocida
}

data class ResultadoMatricula(
    val textoOriginal: String,
    val matriculaParaGuardar: String,
    val tipo: TipoMatriculaDetectada,
    val reconocida: Boolean
)

private const val LETRAS_MATRICULA_ACTUAL = "BCDFGHJKLMNPRSTVWXYZ"

private val distintivosProvinciales = setOf(
    "A", "AB", "AL", "AV",
    "B", "BA", "BI", "BU",
    "C", "CA", "CC", "CE", "CO", "CR", "CS", "CU",
    "GC", "GE", "GI", "GU",
    "H", "HU",
    "J",
    "L", "LE", "LO", "LU",
    "M", "MA", "ML", "MU",
    "NA",
    "O", "OR", "OU",
    "P", "PM", "PO",
    "S", "SA", "SE", "SG", "SO", "SS",
    "T", "TE", "TF", "TO",
    "V", "VA", "VI",
    "Z", "ZA"
)

private val matriculaActual = Regex("^([0-9]{4})([$LETRAS_MATRICULA_ACTUAL]{3})$")
private val matriculaHistorica = Regex("^H([0-9]{4})([$LETRAS_MATRICULA_ACTUAL]{3})$")
private val matriculaCiclomotor = Regex("^C([0-9]{4})([$LETRAS_MATRICULA_ACTUAL]{3})$")
private val matriculaRemolque = Regex("^R([0-9]{4})([$LETRAS_MATRICULA_ACTUAL]{3})$")
private val matriculaEspecial = Regex("^E([0-9]{4})([$LETRAS_MATRICULA_ACTUAL]{3})$")
private val matriculaTemporal = Regex("^([PSVT])([0-9]{4})([$LETRAS_MATRICULA_ACTUAL]{3})$")
private val matriculaAntiguaAlfanumerica = Regex("^([A-Z]{1,2})([0-9]{1,4})([A-Z]{1,2})$")
private val matriculaAntiguaNumerica = Regex("^([A-Z]{1,2})([0-9]{1,6})$")

fun normalizarMatricula(texto: String): ResultadoMatricula {
    val original = texto.trim()
    val compacta = original
        .uppercase()
        .replace(Regex("[^A-Z0-9]"), "")

    if (compacta.isBlank()) {
        return ResultadoMatricula(
            textoOriginal = texto,
            matriculaParaGuardar = original,
            tipo = TipoMatriculaDetectada.NoReconocida,
            reconocida = false
        )
    }

    // miramos primero formatos civiles especificos, asi evitamos que se confundan con provinciales antiguas
    matriculaHistorica.matchEntire(compacta)?.let {
        return ResultadoMatricula(texto, "H ${it.groupValues[1]} ${it.groupValues[2]}", TipoMatriculaDetectada.Historica, true)
    }

    matriculaCiclomotor.matchEntire(compacta)?.let {
        return ResultadoMatricula(texto, "C ${it.groupValues[1]} ${it.groupValues[2]}", TipoMatriculaDetectada.Ciclomotor, true)
    }

    matriculaRemolque.matchEntire(compacta)?.let {
        return ResultadoMatricula(texto, "R ${it.groupValues[1]} ${it.groupValues[2]}", TipoMatriculaDetectada.Remolque, true)
    }

    matriculaEspecial.matchEntire(compacta)?.let {
        return ResultadoMatricula(texto, "E ${it.groupValues[1]} ${it.groupValues[2]}", TipoMatriculaDetectada.Especial, true)
    }

    matriculaTemporal.matchEntire(compacta)?.let {
        return ResultadoMatricula(texto, "${it.groupValues[1]} ${it.groupValues[2]} ${it.groupValues[3]}", TipoMatriculaDetectada.Temporal, true)
    }

    matriculaActual.matchEntire(compacta)?.let {
        return ResultadoMatricula(texto, "${it.groupValues[1]} ${it.groupValues[2]}", TipoMatriculaDetectada.Actual, true)
    }

    matriculaAntiguaAlfanumerica.matchEntire(compacta)?.let {
        val provincia = it.groupValues[1]
        if (provincia in distintivosProvinciales) {
            return ResultadoMatricula(
                textoOriginal = texto,
                matriculaParaGuardar = "$provincia ${it.groupValues[2]} ${it.groupValues[3]}",
                tipo = TipoMatriculaDetectada.AntiguaAlfanumerica,
                reconocida = true
            )
        }
    }

    matriculaAntiguaNumerica.matchEntire(compacta)?.let {
        val provincia = it.groupValues[1]
        if (provincia in distintivosProvinciales) {
            return ResultadoMatricula(
                textoOriginal = texto,
                matriculaParaGuardar = "$provincia ${it.groupValues[2]}",
                tipo = TipoMatriculaDetectada.AntiguaNumerica,
                reconocida = true
            )
        }
    }

    // si no lo entendemos no bloqueamos, solo dejamos que la pantalla pida confirmacion
    return ResultadoMatricula(
        textoOriginal = texto,
        matriculaParaGuardar = original.uppercase(),
        tipo = TipoMatriculaDetectada.NoReconocida,
        reconocida = false
    )
}
