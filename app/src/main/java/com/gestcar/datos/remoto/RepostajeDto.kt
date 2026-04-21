package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.Repostaje
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepostajeDto(
    val id: String = "",

    @SerialName("vehiculo_id")
    val vehiculoId: String = "",

    val fecha: Long = 0,
    val kilometros: Double = 0.0,
    val litros: Double = 0.0,

    @SerialName("precio_por_litro")
    val precioPorLitro: Double = 0.0,

    @SerialName("importe_total")
    val importeTotal: Double = 0.0,

    @SerialName("lleno_completo")
    val llenoCompleto: Boolean = true,

    val gasolinera: String? = null,
    val notas: String? = null,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

fun Repostaje.aDto(): RepostajeDto = RepostajeDto(
    id = id,
    vehiculoId = vehiculoId,
    fecha = fecha,
    kilometros = kilometros,
    litros = litros,
    precioPorLitro = precioPorLitro,
    importeTotal = importeTotal,
    llenoCompleto = llenoCompleto,
    gasolinera = gasolinera,
    notas = notas,
    actualizadoEn = actualizadoEn
)

fun RepostajeDto.aEntidad(): Repostaje = Repostaje(
    id = id,
    vehiculoId = vehiculoId,
    fecha = fecha,
    kilometros = kilometros,
    litros = litros,
    precioPorLitro = precioPorLitro,
    importeTotal = importeTotal,
    llenoCompleto = llenoCompleto,
    gasolinera = gasolinera,
    notas = notas,
    actualizadoEn = actualizadoEn
)
