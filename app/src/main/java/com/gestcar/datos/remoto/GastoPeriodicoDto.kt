package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.GastoPeriodico
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GastoPeriodicoDto(
    val id: String = "",

    @SerialName("vehiculo_id")
    val vehiculoId: String = "",

    val concepto: String = "",
    val importe: Double = 0.0,
    val fecha: Long = 0,
    val periodicidad: String? = null,

    @SerialName("fecha_vencimiento")
    val fechaVencimiento: Long? = null,

    val notas: String? = null,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

fun GastoPeriodico.aDto(): GastoPeriodicoDto = GastoPeriodicoDto(
    id = id,
    vehiculoId = vehiculoId,
    concepto = concepto,
    importe = importe,
    fecha = fecha,
    periodicidad = periodicidad,
    fechaVencimiento = fechaVencimiento,
    notas = notas,
    actualizadoEn = actualizadoEn
)

fun GastoPeriodicoDto.aEntidad(): GastoPeriodico = GastoPeriodico(
    id = id,
    vehiculoId = vehiculoId,
    concepto = concepto,
    importe = importe,
    fecha = fecha,
    periodicidad = periodicidad,
    fechaVencimiento = fechaVencimiento,
    notas = notas,
    actualizadoEn = actualizadoEn
)
