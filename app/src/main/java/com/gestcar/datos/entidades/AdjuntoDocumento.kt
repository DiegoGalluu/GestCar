package com.gestcar.datos.entidades

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// archivo asociado a una tarjeta de documentacion
// guardamos copia local y ruta remota por separado para no depender siempre de internet
@Entity(
    tableName = "adjuntos_documento",
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
data class AdjuntoDocumento(
    @PrimaryKey
    val id: String = "",
    val documentoId: String = "",
    val nombreArchivo: String = "",
    val mimeType: String = "",
    val uriLocal: String = "",
    val rutaStorage: String? = null,
    val tamanoBytes: Long = 0L,
    val fechaAlta: Long = System.currentTimeMillis(),
    val orden: Int = 0,
    val actualizadoEn: Long = System.currentTimeMillis()
)
