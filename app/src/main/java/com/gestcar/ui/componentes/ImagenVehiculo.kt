package com.gestcar.ui.componentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Moped
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gestcar.datos.entidades.Vehiculo

@Composable
fun ImagenVehiculo(
    vehiculo: Vehiculo,
    modifier: Modifier = Modifier,
    iconoPadding: Int = 12
) {
    val icono = when (vehiculo.tipo) {
        "MOTO" -> Icons.Default.Moped
        "FURGONETA" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }

    if (!vehiculo.imagenUri.isNullOrBlank()) {
        AsyncImage(
            model = vehiculo.imagenUri,
            contentDescription = "foto de ${vehiculo.marca} ${vehiculo.modelo}",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icono,
                contentDescription = "icono de ${vehiculo.tipo.lowercase()}",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(iconoPadding.dp)
            )
        }
    }
}
