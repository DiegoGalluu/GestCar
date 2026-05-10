package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.CampoDocumento
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CampoDocumentoDto(
    val id: String = "",

    @SerialName("documento_id")
    val documentoId: String = "",

    val nombre: String = "",
    val valor: String = "",
    val orden: Int = 0,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

fun CampoDocumento.aDto(): CampoDocumentoDto = CampoDocumentoDto(
    id = id,
    documentoId = documentoId,
    nombre = nombre,
    valor = valor,
    orden = orden,
    actualizadoEn = actualizadoEn
)

fun CampoDocumentoDto.aEntidad(): CampoDocumento = CampoDocumento(
    id = id,
    documentoId = documentoId,
    nombre = nombre,
    valor = valor,
    orden = orden,
    actualizadoEn = actualizadoEn
)
