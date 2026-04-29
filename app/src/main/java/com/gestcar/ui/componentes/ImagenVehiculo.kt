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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.util.GestorImagenesVehiculo
import com.gestcar.util.esRutaRemotaPrivadaImagen
import com.gestcar.util.normalizarRutaImagenVehiculo
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.hours

@Composable
fun ImagenVehiculo(
    vehiculo: Vehiculo,
    modifier: Modifier = Modifier,
    iconoPadding: Int = 12
) {
    val contexto = LocalContext.current
    val imagenNormalizada = normalizarRutaImagenVehiculo(vehiculo.imagenUri)
    val imagenLocal = remember(vehiculo.usuarioId, vehiculo.id, vehiculo.imagenUri) {
        GestorImagenesVehiculo.obtenerUriLocalVehiculo(
            context = contexto,
            usuarioId = vehiculo.usuarioId,
            vehiculoId = vehiculo.id
        )
    }
    val imagenInicial = imagenLocal ?: imagenNormalizada

    val imagenMostrable by produceState<String?>(initialValue = imagenInicial, imagenNormalizada, imagenLocal) {
        value = when {
            !imagenLocal.isNullOrBlank() -> imagenLocal
            imagenNormalizada.isNullOrBlank() -> null
            esRutaRemotaPrivadaImagen(imagenNormalizada) -> {
                try {
                    val urlFirmada = ClienteSupabase.cliente.storage
                        .from(ClienteSupabase.BUCKET_FOTOS_VEHICULOS)
                        .createSignedUrl(imagenNormalizada, 6.hours)

                    withContext(Dispatchers.IO) {
                        GestorImagenesVehiculo.guardarImagenRemotaEnCache(
                            context = contexto,
                            imagenUrl = urlFirmada,
                            usuarioId = vehiculo.usuarioId,
                            vehiculoId = vehiculo.id
                        )
                    } ?: urlFirmada
                } catch (_: Exception) {
                    null
                }
            }

            else -> imagenNormalizada
        }
    }

    val icono = when (vehiculo.tipo) {
        "MOTO" -> Icons.Default.Moped
        "FURGONETA" -> Icons.Default.LocalShipping
        else -> Icons.Default.DirectionsCar
    }

    if (!imagenMostrable.isNullOrBlank()) {
        AsyncImage(
            model = imagenMostrable,
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
