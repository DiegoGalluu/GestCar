package com.gestcar.datos.remoto

import kotlinx.serialization.Serializable

@Serializable
data class PrecioCombustibleGasolinera(
    val nombre: String,
    val precio: Double
)

@Serializable
data class Gasolinera(
    val id: String,
    val rotulo: String,
    val direccion: String,
    val municipio: String,
    val provincia: String,
    val horario: String,
    val latitud: Double,
    val longitud: Double,
    val precios: List<PrecioCombustibleGasolinera>
)
