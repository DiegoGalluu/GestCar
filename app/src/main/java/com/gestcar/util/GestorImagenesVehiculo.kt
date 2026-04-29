package com.gestcar.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.math.max

private const val MAX_LADO_IMAGEN = 1600
private const val CALIDAD_JPEG = 80

object GestorImagenesVehiculo {

    fun crearUriTemporalCamara(context: Context, vehiculoId: String): Uri {
        val directorio = File(context.cacheDir, "camera").apply { mkdirs() }
        val archivo = File.createTempFile("vehiculo_${vehiculoId}_", ".jpg", directorio)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
    }

    fun guardarImagenComprimida(
        context: Context,
        origenUri: Uri,
        usuarioId: String,
        vehiculoId: String
    ): String {
        val bitmap = decodificarBitmapReducido(context, origenUri)
            ?: error("No se ha podido leer la imagen seleccionada")

        val bitmapEscalado = escalarSiHaceFalta(bitmap)
        val directorio = File(context.filesDir, "vehiculos_imagenes/$usuarioId").apply { mkdirs() }
        val archivo = File(directorio, "$vehiculoId.jpg")

        FileOutputStream(archivo).use { salida ->
            bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD_JPEG, salida)
        }

        if (bitmapEscalado != bitmap) {
            bitmapEscalado.recycle()
        }
        bitmap.recycle()

        return Uri.fromFile(archivo).toString()
    }

    fun obtenerUriLocalVehiculo(
        context: Context,
        usuarioId: String,
        vehiculoId: String
    ): String? {
        val archivo = File(context.filesDir, "vehiculos_imagenes/$usuarioId/$vehiculoId.jpg")
        return if (archivo.exists()) {
            Uri.fromFile(archivo).toString()
        } else {
            null
        }
    }

    fun guardarImagenRemotaEnCache(
        context: Context,
        imagenUrl: String,
        usuarioId: String,
        vehiculoId: String
    ): String? {
        return try {
            val directorio = File(context.filesDir, "vehiculos_imagenes/$usuarioId").apply { mkdirs() }
            val archivo = File(directorio, "$vehiculoId.jpg")
            URL(imagenUrl).openStream().use { entrada ->
                FileOutputStream(archivo).use { salida ->
                    entrada.copyTo(salida)
                }
            }
            Uri.fromFile(archivo).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun decodificarBitmapReducido(context: Context, uri: Uri): Bitmap? {
        val opcionesLimites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { flujo ->
            BitmapFactory.decodeStream(flujo, null, opcionesLimites)
        }

        val sampleSize = calcularSampleSize(
            ancho = opcionesLimites.outWidth,
            alto = opcionesLimites.outHeight
        )

        val opcionesDecodificacion = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        return context.contentResolver.openInputStream(uri)?.use { flujo ->
            BitmapFactory.decodeStream(flujo, null, opcionesDecodificacion)
        }
    }

    private fun calcularSampleSize(ancho: Int, alto: Int): Int {
        var sampleSize = 1
        var anchoActual = ancho
        var altoActual = alto

        while (anchoActual > MAX_LADO_IMAGEN * 2 || altoActual > MAX_LADO_IMAGEN * 2) {
            sampleSize *= 2
            anchoActual /= 2
            altoActual /= 2
        }

        return max(1, sampleSize)
    }

    private fun escalarSiHaceFalta(bitmap: Bitmap): Bitmap {
        val ladoMayor = max(bitmap.width, bitmap.height)
        if (ladoMayor <= MAX_LADO_IMAGEN) {
            return bitmap
        }

        val ratio = MAX_LADO_IMAGEN.toFloat() / ladoMayor.toFloat()
        val anchoNuevo = (bitmap.width * ratio).toInt()
        val altoNuevo = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, anchoNuevo, altoNuevo, true)
    }
}
