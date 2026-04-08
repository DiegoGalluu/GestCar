package com.gestcar.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// esquema de colores para el tema claro
private val EsquemaClaro = lightColorScheme(
    primary = AzulOscuro,
    secondary = AzulClaro,
    tertiary = VerdeAzulado,
    error = RojoError,
    background = FondoClaro,
    surface = SuperficieBlanca,
    onPrimary = TextoSobrePrimario,
    onSurface = TextoOscuro,
    onBackground = TextoOscuro
)

// esquema de colores para el tema oscuro
private val EsquemaOscuro = darkColorScheme(
    primary = AzulOscuroNoche,
    secondary = AzulClaroNoche,
    tertiary = VerdeAzuladoNoche,
    error = RojoError,
    background = FondoOscuro,
    surface = SuperficieOscura,
    onPrimary = TextoOscuro,
    onSurface = TextoClaro,
    onBackground = TextoClaro
)

// tema principal de gestcar, se aplica a toda la app
// detecta automaticamente si el sistema esta en modo oscuro
@Composable
fun GestCarTema(
    modoOscuro: Boolean = isSystemInDarkTheme(),
    contenido: @Composable () -> Unit
) {
    val esquemaColores = if (modoOscuro) EsquemaOscuro else EsquemaClaro

    MaterialTheme(
        colorScheme = esquemaColores,
        content = contenido
    )
}
