package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.Mantenimiento
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MantenimientoDto(
    val id: String = "",

    @SerialName("vehiculo_id")
    val vehiculoId: String = "",

    val tipo: String = "",
    val categoria: String = "MANTENIMIENTO",
    val fecha: Long = 0,
    val kilometros: Double? = null,
    val coste: Double = 0.0,
    val taller: String? = null,
    val descripcion: String? = null,
    val realizado: Boolean = true,

    @SerialName("fecha_realizado")
    val fechaRealizado: Long? = null,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

fun Mantenimiento.aDto(): MantenimientoDto = MantenimientoDto(
    id = id,
    vehiculoId = vehiculoId,
    tipo = tipo,
    categoria = categoria,
    fecha = fecha,
    kilometros = kilometros,
    coste = coste,
    taller = taller,
    descripcion = descripcion,
    realizado = realizado,
    fechaRealizado = fechaRealizado,
    actualizadoEn = actualizadoEn
)

fun MantenimientoDto.aEntidad(): Mantenimiento = Mantenimiento(
    id = id,
    vehiculoId = vehiculoId,
    tipo = tipo,
    categoria = categoria,
    fecha = fecha,
    kilometros = kilometros,
    coste = coste,
    taller = taller,
    descripcion = descripcion,
    realizado = realizado,
    fechaRealizado = fechaRealizado,
    actualizadoEn = actualizadoEn
)
