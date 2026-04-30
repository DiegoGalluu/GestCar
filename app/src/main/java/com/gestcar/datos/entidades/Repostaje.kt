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
    // vinculamos cada repostaje con su vehiculo
    // la relacion local replica la foreign key que tambien existe en supabase
    val vehiculoId: String = "",
    val fecha: Long = System.currentTimeMillis(),
    // kilometros es el odometro en el momento de repostar
    // no son kilometros recorridos desde el ultimo repostaje
    val kilometros: Double = 0.0,
    val litros: Double = 0.0,
    val precioPorLitro: Double = 0.0,
    // se guarda calculado para que listados y estadisticas no recalculen siempre
    val importeTotal: Double = 0.0,
    // solo los llenos completos sirven para calcular consumo medio fiable
    val llenoCompleto: Boolean = true,
    val gasolinera: String? = null,
    val notas: String? = null,
    val actualizadoEn: Long = System.currentTimeMillis()
)
