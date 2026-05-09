package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gestcar.datos.entidades.DocumentoVehiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentoVehiculoDao {

    @Query("SELECT * FROM documentos_vehiculo WHERE vehiculoId = :vehiculoId ORDER BY titulo COLLATE NOCASE ASC")
    fun obtenerPorVehiculo(vehiculoId: String): Flow<List<DocumentoVehiculo>>

    @Query("SELECT * FROM documentos_vehiculo WHERE id = :id")
    suspend fun obtenerPorId(id: String): DocumentoVehiculo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(documento: DocumentoVehiculo)

    @Delete
    suspend fun eliminar(documento: DocumentoVehiculo)
}
