package com.gestcar.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.gestcar.datos.entidades.AdjuntoDocumento
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

private const val MAX_LADO_DOCUMENTO_IMAGEN = 1800
private const val CALIDAD_DOCUMENTO_IMAGEN = 82

object GestorArchivosDocumento {

    fun crearUriTemporalCamara(contexto: Context, documentoId: String): Uri {
        val directorio = File(contexto.cacheDir, "camera").apply { mkdirs() }
        val archivo = File.createTempFile("documento_${documentoId}_", ".jpg", directorio)
        return FileProvider.getUriForFile(
            contexto,
            "${contexto.packageName}.fileprovider",
            archivo
        )
    }

    fun guardarAdjunto(
        contexto: Context,
        documentoId: String,
        origenUri: Uri,
        orden: Int
    ): AdjuntoDocumento {
        val id = UUID.randomUUID().toString()
        val mimeType = contexto.contentResolver.getType(origenUri) ?: "application/octet-stream"
        val nombreOriginal = obtenerNombreArchivo(contexto, origenUri, mimeType)
        val esImagen = mimeType.startsWith("image/")
        val mimeFinal = if (esImagen) "image/jpeg" else mimeType
        val extension = if (esImagen) ".jpg" else extensionParaMime(nombreOriginal, mimeType)
        val directorio = File(contexto.filesDir, "documentos_adjuntos/$documentoId").apply {
            mkdirs()
        }
        val archivoDestino = File(directorio, "$id$extension")

        if (esImagen) {
            guardarImagenComprimida(contexto, origenUri, archivoDestino)
        } else {
            contexto.contentResolver.openInputStream(origenUri)?.use { entrada ->
                FileOutputStream(archivoDestino).use { salida ->
                    entrada.copyTo(salida)
                }
            } ?: error("No se ha podido leer el archivo seleccionado")
        }

        return AdjuntoDocumento(
            id = id,
            documentoId = documentoId,
            nombreArchivo = nombreOriginal,
            mimeType = mimeFinal,
            uriLocal = Uri.fromFile(archivoDestino).toString(),
            tamanoBytes = archivoDestino.length(),
            fechaAlta = System.currentTimeMillis(),
            orden = orden
        )
    }

    fun eliminarArchivo(adjunto: AdjuntoDocumento) {
        runCatching {
            val ruta = Uri.parse(adjunto.uriLocal).path ?: return
            File(ruta).delete()
        }
    }

    private fun obtenerNombreArchivo(
        contexto: Context,
        uri: Uri,
        mimeType: String
    ): String {
        val nombreDesdeProveedor = contexto.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (indiceNombre >= 0 && cursor.moveToFirst()) {
                    cursor.getString(indiceNombre)
                } else {
                    null
                }
            }

        return nombreDesdeProveedor
            ?: "documento_${System.currentTimeMillis()}${extensionParaMime("", mimeType)}"
    }

    private fun extensionParaMime(nombre: String, mimeType: String): String {
        val extensionExistente = nombre.substringAfterLast('.', missingDelimiterValue = "")
        if (extensionExistente.isNotBlank()) {
            return ".$extensionExistente"
        }

        return when (mimeType) {
            "application/pdf" -> ".pdf"
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            else -> ".jpg"
        }
    }

    private fun guardarImagenComprimida(
        contexto: Context,
        origenUri: Uri,
        archivoDestino: File
    ) {
        val bitmap = decodificarBitmapReducido(contexto, origenUri)
            ?: error("No se ha podido leer la imagen seleccionada")
        val bitmapEscalado = escalarSiHaceFalta(bitmap)

        FileOutputStream(archivoDestino).use { salida ->
            bitmapEscalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD_DOCUMENTO_IMAGEN, salida)
        }

        if (bitmapEscalado != bitmap) {
            bitmapEscalado.recycle()
        }
        bitmap.recycle()
    }

    private fun decodificarBitmapReducido(contexto: Context, uri: Uri): Bitmap? {
        val opcionesLimites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contexto.contentResolver.openInputStream(uri)?.use { flujo ->
            BitmapFactory.decodeStream(flujo, null, opcionesLimites)
        }

        val sampleSize = calcularSampleSize(
            ancho = opcionesLimites.outWidth,
            alto = opcionesLimites.outHeight
        )

        val opcionesDecodificacion = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }

        return contexto.contentResolver.openInputStream(uri)?.use { flujo ->
            BitmapFactory.decodeStream(flujo, null, opcionesDecodificacion)
        }
    }

    private fun calcularSampleSize(ancho: Int, alto: Int): Int {
        var sampleSize = 1
        var anchoActual = ancho
        var altoActual = alto

        while (
            anchoActual > MAX_LADO_DOCUMENTO_IMAGEN * 2 ||
            altoActual > MAX_LADO_DOCUMENTO_IMAGEN * 2
        ) {
            sampleSize *= 2
            anchoActual /= 2
            altoActual /= 2
        }

        return max(1, sampleSize)
    }

    private fun escalarSiHaceFalta(bitmap: Bitmap): Bitmap {
        val ladoMayor = max(bitmap.width, bitmap.height)
        if (ladoMayor <= MAX_LADO_DOCUMENTO_IMAGEN) {
            return bitmap
        }

        val ratio = MAX_LADO_DOCUMENTO_IMAGEN.toFloat() / ladoMayor.toFloat()
        val anchoNuevo = (bitmap.width * ratio).toInt()
        val altoNuevo = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, anchoNuevo, altoNuevo, true)
    }
}
