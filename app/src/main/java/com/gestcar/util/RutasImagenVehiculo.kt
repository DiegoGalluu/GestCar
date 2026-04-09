package com.gestcar.util

import com.gestcar.datos.remoto.ClienteSupabase

fun normalizarRutaImagenVehiculo(imagenUri: String?): String? {
    if (imagenUri.isNullOrBlank()) {
        return null
    }

    val marcadorBucketPublico = "/storage/v1/object/public/${ClienteSupabase.BUCKET_FOTOS_VEHICULOS}/"
    val marcadorBucketFirmado = "/storage/v1/object/sign/${ClienteSupabase.BUCKET_FOTOS_VEHICULOS}/"

    return when {
        imagenUri.contains(marcadorBucketPublico) -> imagenUri.substringAfter(marcadorBucketPublico)
            .substringBefore("?")

        imagenUri.contains(marcadorBucketFirmado) -> imagenUri.substringAfter(marcadorBucketFirmado)
            .substringBefore("?")

        else -> imagenUri
    }
}

fun esRutaRemotaPrivadaImagen(imagenUri: String?): Boolean {
    val ruta = normalizarRutaImagenVehiculo(imagenUri) ?: return false
    return !ruta.startsWith("file://") && !ruta.startsWith("content://") && !ruta.startsWith("http")
}
