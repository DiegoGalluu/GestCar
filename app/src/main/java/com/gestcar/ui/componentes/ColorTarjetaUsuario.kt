package com.gestcar.ui.componentes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

// alternamos las tarjetas que crea el usuario para separar mejor listas largas
// el cambio es suave para no convertir cada pantalla en una feria
@Composable
fun colorFondoTarjetaUsuario(indice: Int): Color {
    val colorBase = MaterialTheme.colorScheme.surfaceVariant
    return if (indice % 2 == 0) {
        colorBase
    } else {
        lerp(colorBase, MaterialTheme.colorScheme.onSurface, 0.035f)
    }
}
