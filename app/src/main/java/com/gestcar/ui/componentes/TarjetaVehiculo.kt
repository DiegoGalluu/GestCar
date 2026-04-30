package com.gestcar.ui.componentes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.gestcar.datos.entidades.Vehiculo

// tarjeta que muestra la info resumida de un vehiculo en la lista
// muestra el icono segun el tipo, marca, modelo, matricula y kilometraje
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TarjetaVehiculo(
    vehiculo: Vehiculo,
    alPulsar: () -> Unit,
    alPulsacionLarga: (() -> Unit)? = null,
    modoOrganizacion: Boolean = false,
    alMoverArriba: (() -> Unit)? = null,
    alMoverAbajo: (() -> Unit)? = null
) {
    val umbralArrastre = with(LocalDensity.current) { 44.dp.toPx() }
    var acumuladoArrastre by remember { mutableFloatStateOf(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (alPulsacionLarga != null) {
                    Modifier.combinedClickable(
                        onClick = alPulsar,
                        onLongClick = alPulsacionLarga
                    )
                } else {
                    Modifier.clickable { alPulsar() }
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImagenVehiculo(
                vehiculo = vehiculo,
                modifier = Modifier.size(64.dp),
                iconoPadding = 10
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // marca y modelo juntos como titulo
                Text(
                    text = "${vehiculo.marca} ${vehiculo.modelo}",
                    style = MaterialTheme.typography.titleMedium
                )

                // matricula en gris
                Text(
                    text = vehiculo.matricula,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // kilometraje actual
                Text(
                    text = "${String.format("%,.0f", vehiculo.kilometraje)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (modoOrganizacion) {
                Spacer(modifier = Modifier.width(8.dp))
                VerticalDivider(
                    modifier = Modifier
                        .height(72.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(44.dp)
                        .pointerInput(vehiculo.id) {
                            detectVerticalDragGestures(
                                onDragEnd = { acumuladoArrastre = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    acumuladoArrastre += dragAmount
                                    when {
                                        acumuladoArrastre <= -umbralArrastre -> {
                                            alMoverArriba?.invoke()
                                            acumuladoArrastre = 0f
                                        }

                                        acumuladoArrastre >= umbralArrastre -> {
                                            alMoverAbajo?.invoke()
                                            acumuladoArrastre = 0f
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    IconButton(
                        onClick = { alMoverArriba?.invoke() },
                        enabled = alMoverArriba != null,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Mover arriba"
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Arrastrar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    IconButton(
                        onClick = { alMoverAbajo?.invoke() },
                        enabled = alMoverAbajo != null,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Mover abajo"
                        )
                    }
                }
            }
        }
    }
}
