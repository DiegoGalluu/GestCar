package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.PrimaryKey

// entidad principal que representa un vehiculo en la base de datos local
// cada vehiculo tiene un id unico que se genera como uuid para que sea
// compatible tanto con room como con supabase sin conflictos
@Entity(tableName = "vehiculos")
data class Vehiculo(
    // id unico del vehiculo, se genera como uuid al crear uno nuevo
    @PrimaryKey
    val id: String = "",

    // id del usuario en supabase, para saber a quien pertenece el vehiculo
    val usuarioId: String = "",

    // marca del fabricante, por ejemplo toyota o seat
    val marca: String = "",

    // modelo del vehiculo, por ejemplo corolla o ibiza
    val modelo: String = "",

    // anio de fabricacion
    val anio: Int = 2024,

    // tipo de vehiculo, puede ser coche, moto o furgoneta
    val tipo: String = "COCHE",

    // matricula del vehiculo, debe ser unica
    val matricula: String = "",

    // kilometraje actual del vehiculo
    val kilometraje: Double = 0.0,

    // tipo de combustible, puede ser gasolina, diesel, electrico, hibrido o glp
    val tipoCombustible: String? = null,

    // uri de la imagen del vehiculo si el usuario ha puesto una
    val imagenUri: String? = null,

    // fecha en la que se registro el vehiculo en la app, en milisegundos
    val fechaAlta: Long = System.currentTimeMillis(),

    // notas opcionales que el usuario quiera poner
    val notas: String? = null,

    // fecha de la ultima modificacion, se usa para la sincronizacion con supabase
    val actualizadoEn: Long = System.currentTimeMillis()
)
