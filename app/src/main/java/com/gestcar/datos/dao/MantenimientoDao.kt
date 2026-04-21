package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestcar.datos.entidades.Mantenimiento
import kotlinx.coroutines.flow.Flow

@Dao
interface MantenimientoDao {

    @Query("SELECT * FROM mantenimientos WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    fun obtenerPorVehiculo(vehiculoId: String): Flow<List<Mantenimiento>>

    @Query("SELECT * FROM mantenimientos WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    suspend fun obtenerPorVehiculoLista(vehiculoId: String): List<Mantenimiento>

    @Query("SELECT * FROM mantenimientos WHERE id = :id")
    suspend fun obtenerPorId(id: String): Mantenimiento?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(mantenimiento: Mantenimiento)

    @Update
    suspend fun actualizar(mantenimiento: Mantenimiento)

    @Delete
    suspend fun eliminar(mantenimiento: Mantenimiento)
}
