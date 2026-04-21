package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// repostaje asociado a un vehiculo, se guarda en local aunque no haya conexion
@Entity(
    tableName = "repostajes",
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
data class Repostaje(
    @PrimaryKey
    val id: String = "",
    val vehiculoId: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val kilometros: Double = 0.0,
    val litros: Double = 0.0,
    val precioPorLitro: Double = 0.0,
    val importeTotal: Double = 0.0,
    val llenoCompleto: Boolean = true,
    val gasolinera: String? = null,
    val notas: String? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
