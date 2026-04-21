package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// aviso pendiente para un vehiculo, puede depender de fecha o kilometraje
@Entity(
    tableName = "recordatorios",
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
data class Recordatorio(
    @PrimaryKey
    val id: String = "",
    val vehiculoId: String = "",
    val concepto: String = "",
    val fechaLimite: Long? = null,
    val kilometrajeLimite: Double? = null,
    val completado: Boolean = false,
    val fechaCompletado: Long? = null,
    val notas: String? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
