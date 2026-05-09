package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// campo personalizado dentro de una tarjeta de documentacion
// funciona como nombre valor para no limitar lo que el usuario puede guardar
@Entity(
    tableName = "campos_documento",
    foreignKeys = [
        ForeignKey(
            entity = DocumentoVehiculo::class,
            parentColumns = ["id"],
            childColumns = ["documentoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentoId")]
)
data class CampoDocumento(
    @PrimaryKey
    val id: String = "",
    val documentoId: String = "",
    val nombre: String = "",
    val valor: String = "",
    val orden: Int = 0,
    val actualizadoEn: Long = System.currentTimeMillis()
)
