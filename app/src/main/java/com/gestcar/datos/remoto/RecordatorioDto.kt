package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.Recordatorio
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecordatorioDto(
    val id: String = "",

    @SerialName("vehiculo_id")
    val vehiculoId: String = "",

    val concepto: String = "",

    @SerialName("fecha_limite")
    val fechaLimite: Long? = null,

    @SerialName("kilometraje_limite")
    val kilometrajeLimite: Double? = null,

    @SerialName("periodicidad_tiempo_cantidad")
    val periodicidadTiempoCantidad: Int? = null,

    @SerialName("periodicidad_tiempo_unidad")
    val periodicidadTiempoUnidad: String? = null,

    @SerialName("periodicidad_kilometros")
    val periodicidadKilometros: Double? = null,

    val completado: Boolean = false,

    @SerialName("fecha_completado")
    val fechaCompletado: Long? = null,

    val notas: String? = null,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

fun Recordatorio.aDto(): RecordatorioDto = RecordatorioDto(
    id = id,
    vehiculoId = vehiculoId,
    concepto = concepto,
    fechaLimite = fechaLimite,
    kilometrajeLimite = kilometrajeLimite,
    periodicidadTiempoCantidad = periodicidadTiempoCantidad,
    periodicidadTiempoUnidad = periodicidadTiempoUnidad,
    periodicidadKilometros = periodicidadKilometros,
    completado = completado,
    fechaCompletado = fechaCompletado,
    notas = notas,
    actualizadoEn = actualizadoEn
)

fun RecordatorioDto.aEntidad(): Recordatorio = Recordatorio(
    id = id,
    vehiculoId = vehiculoId,
    concepto = concepto,
    fechaLimite = fechaLimite,
    kilometrajeLimite = kilometrajeLimite,
    periodicidadTiempoCantidad = periodicidadTiempoCantidad,
    periodicidadTiempoUnidad = periodicidadTiempoUnidad,
    periodicidadKilometros = periodicidadKilometros,
    completado = completado,
    fechaCompletado = fechaCompletado,
    notas = notas,
    actualizadoEn = actualizadoEn
)
