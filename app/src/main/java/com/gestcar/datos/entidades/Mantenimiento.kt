package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// operacion de mantenimiento o reparacion vinculada a un vehiculo
@Entity(
    tableName = "mantenimientos",
    foreignKeys = [
        ForeignKey(
            entity = Vehiculo::class,
            parentColumns = ["id"],
            childColumns = ["vehiculoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehiculoId")]
)
data class Mantenimiento(
    @PrimaryKey
    val id: String = "",
    val vehiculoId: String = "",
    val tipo: String = "",
    val categoria: String = "MANTENIMIENTO",
    val fecha: Long = System.currentTimeMillis(),
    val kilometros: Double? = null,
    val coste: Double = 0.0,
    val taller: String? = null,
    val descripcion: String? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
