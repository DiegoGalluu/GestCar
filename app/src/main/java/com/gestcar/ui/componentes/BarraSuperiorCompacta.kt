package com.gestcar.ui.componentes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gestcar.util.ObservadorConectividad
import kotlinx.coroutines.delay

// usamos una barra propia para compactar la cabecera sin perder el espacio seguro del sistema
// asi ganamos altura util en pantalla y mantenemos el look de la app en todos los sitios
@Composable
fun BarraSuperiorCompacta(
    titulo: String,
    modifier: Modifier = Modifier,
    alVolver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    val contexto = LocalContext.current
    val hayConexion by produceState<Boolean?>(initialValue = null, contexto) {
        ObservadorConectividad(contexto).observarConexion().collect { conectado ->
            value = conectado
        }
    }
    var huboDesconexion by remember { mutableStateOf(false) }
    var mostrarConexionRestablecida by remember { mutableStateOf(false) }

    LaunchedEffect(hayConexion) {
        when (hayConexion) {
            false -> {
                huboDesconexion = true
                mostrarConexionRestablecida = false
            }

            true -> {
                if (huboDesconexion) {
                    mostrarConexionRestablecida = true
                    delay(3000)
                    mostrarConexionRestablecida = false
                    huboDesconexion = false
                }
            }

            null -> Unit
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(
            modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (alVolver != null) {
                    IconButton(
                        onClick = alVolver,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Volver",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    text = titulo,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    content = acciones
                )
            }
        }

        BannerConexion(
            visible = hayConexion == false || mostrarConexionRestablecida,
            texto = if (hayConexion == false) "No hay conexión" else "Vuelves a tener conexión",
            color = if (hayConexion == false) Color(0xFF3A3A3A) else Color(0xFF2E8B57)
        )
    }
}

@Composable
private fun BannerConexion(
    visible: Boolean,
    texto: String,
    color: Color
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = color
        ) {
            Text(
                text = texto,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
