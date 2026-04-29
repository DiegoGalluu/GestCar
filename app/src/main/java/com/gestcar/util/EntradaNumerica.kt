package com.gestcar.util

import java.math.BigDecimal

// dejamos que el usuario escriba con coma o punto sin pelear con el cursor
// el formateo bonito se hace despues en tarjetas y detalles
fun limpiarEntradaDecimal(texto: String): String {
    val resultado = StringBuilder()
    var separadorUsado = false

    texto.forEach { caracter ->
        when {
            caracter.isDigit() -> resultado.append(caracter)
            (caracter == ',' || caracter == '.') && !separadorUsado -> {
                resultado.append(caracter)
                separadorUsado = true
            }
        }
    }

    return resultado.toString()
}

fun parsearDecimalFlexible(texto: String): Double? {
    val normalizado = texto.trim().replace(',', '.')
    if (normalizado.isBlank() || normalizado == ".") {
        return null
    }

    return normalizado.toDoubleOrNull()
}

fun textoDecimalEditable(valor: Double): String {
    if (valor <= 0.0) {
        return ""
    }

    return BigDecimal.valueOf(valor).stripTrailingZeros().toPlainString()
}
