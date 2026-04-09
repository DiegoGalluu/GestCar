package com.gestcar.datos.remoto

import com.gestcar.datos.entidades.Vehiculo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// dto (data transfer object) para enviar y recibir vehiculos de supabase
// los nombres con serial name coinciden con las columnas de la tabla en supabase
// que usa snake_case en vez de camelCase
@Serializable
data class VehiculoDto(
    val id: String = "",

    @SerialName("usuario_id")
    val usuarioId: String = "",

    val marca: String = "",
    val modelo: String = "",
    // compatibilidad temporal con la columna antigua `anio` en Supabase
    @SerialName("anio")
    val anioLegacy: Int? = null,

    @SerialName("anio_fabricacion")
    val anioFabricacion: Int = 2024,

    @SerialName("mes_fabricacion")
    val mesFabricacion: Int? = null,

    @SerialName("dia_fabricacion")
    val diaFabricacion: Int? = null,
    val tipo: String = "COCHE",
    val matricula: String = "",
    val kilometraje: Double = 0.0,

    @SerialName("tipo_combustible")
    val tipoCombustible: String? = null,

    @SerialName("imagen_uri")
    val imagenUri: String? = null,

    @SerialName("fecha_alta")
    val fechaAlta: Long = 0,

    val notas: String? = null,

    @SerialName("actualizado_en")
    val actualizadoEn: Long = 0
)

// extension para convertir de la entidad local al dto de supabase
fun Vehiculo.aDto(): VehiculoDto = VehiculoDto(
    id = id,
    usuarioId = usuarioId,
    marca = marca,
    modelo = modelo,
    anioLegacy = anioFabricacion,
    anioFabricacion = anioFabricacion,
    mesFabricacion = mesFabricacion,
    diaFabricacion = diaFabricacion,
    tipo = tipo,
    matricula = matricula,
    kilometraje = kilometraje,
    tipoCombustible = tipoCombustible,
    imagenUri = imagenUri,
    fechaAlta = fechaAlta,
    notas = notas,
    actualizadoEn = actualizadoEn
)

// extension para convertir del dto de supabase a la entidad local
fun VehiculoDto.aEntidad(): Vehiculo = Vehiculo(
    id = id,
    usuarioId = usuarioId,
    marca = marca,
    modelo = modelo,
    anioFabricacion = anioFabricacion.takeIf { it > 0 } ?: anioLegacy ?: 2024,
    mesFabricacion = mesFabricacion,
    diaFabricacion = diaFabricacion,
    tipo = tipo,
    matricula = matricula,
    kilometraje = kilometraje,
    tipoCombustible = tipoCombustible,
    imagenUri = imagenUri,
    fechaAlta = fechaAlta,
    notas = notas,
    actualizadoEn = actualizadoEn
)
