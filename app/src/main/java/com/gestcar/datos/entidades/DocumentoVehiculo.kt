package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// tarjeta libre de documentacion asociada a un vehiculo
// no fijamos el tipo porque cada usuario guarda papeles distintos
@Entity(
    tableName = "documentos_vehiculo",
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
data class DocumentoVehiculo(
    @PrimaryKey
    val id: String = "",
    val vehiculoId: String = "",
    val titulo: String = "",
    val notas: String? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
