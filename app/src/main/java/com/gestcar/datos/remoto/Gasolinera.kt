package com.gestcar.datos.remoto

data class PrecioCombustibleGasolinera(
    val nombre: String,
    val precio: Double
)

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
