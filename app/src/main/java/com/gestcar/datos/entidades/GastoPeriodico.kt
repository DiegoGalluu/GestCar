package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// gasto recurrente o puntual relacionado con un vehiculo
@Entity(
    tableName = "gastos_periodicos",
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
data class GastoPeriodico(
    @PrimaryKey
    val id: String = "",
    val vehiculoId: String = "",
    val concepto: String = "",
    val importe: Double = 0.0,
    val fecha: Long = System.currentTimeMillis(),
    val periodicidad: String? = null,
    val fechaVencimiento: Long? = null,
    val notas: String? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
