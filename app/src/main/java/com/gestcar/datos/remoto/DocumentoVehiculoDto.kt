package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.DocumentoVehiculo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentoVehiculoDto(
    val id: String = "",

    @SerialName("vehiculo_id")
    val vehiculoId: String = "",

    val titulo: String = "",
    val notas: String? = null,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

fun DocumentoVehiculo.aDto(): DocumentoVehiculoDto = DocumentoVehiculoDto(
    id = id,
    vehiculoId = vehiculoId,
    titulo = titulo,
    notas = notas,
    actualizadoEn = actualizadoEn
)

fun DocumentoVehiculoDto.aEntidad(): DocumentoVehiculo = DocumentoVehiculo(
    id = id,
    vehiculoId = vehiculoId,
    titulo = titulo,
    notas = notas,
    actualizadoEn = actualizadoEn
)
