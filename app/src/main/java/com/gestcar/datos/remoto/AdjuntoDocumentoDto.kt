package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.AdjuntoDocumento
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdjuntoDocumentoDto(
    val id: String,
    @SerialName("documento_id")
    val documentoId: String,
    @SerialName("nombre_archivo")
    val nombreArchivo: String,
    @SerialName("mime_type")
    val mimeType: String,
    @SerialName("ruta_storage")
    val rutaStorage: String,
    @SerialName("tamano_bytes")
    val tamanoBytes: Long,
    @SerialName("fecha_alta")
    val fechaAlta: Long,
    val orden: Int,
    @SerialName("actualizado_en")
    val actualizadoEn: Long
)

fun AdjuntoDocumento.aDto(): AdjuntoDocumentoDto {
    return AdjuntoDocumentoDto(
        id = id,
        documentoId = documentoId,
        nombreArchivo = nombreArchivo,
        mimeType = mimeType,
        rutaStorage = rutaStorage.orEmpty(),
        tamanoBytes = tamanoBytes,
        fechaAlta = fechaAlta,
        orden = orden,
        actualizadoEn = actualizadoEn
    )
}

fun AdjuntoDocumentoDto.aEntidad(uriLocal: String = ""): AdjuntoDocumento {
    return AdjuntoDocumento(
        id = id,
        documentoId = documentoId,
        nombreArchivo = nombreArchivo,
        mimeType = mimeType,
        uriLocal = uriLocal,
        rutaStorage = rutaStorage,
        tamanoBytes = tamanoBytes,
        fechaAlta = fechaAlta,
        orden = orden,
        actualizadoEn = actualizadoEn
    )
}
