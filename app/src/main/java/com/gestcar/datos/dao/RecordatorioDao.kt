package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestcar.datos.entidades.Recordatorio
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordatorioDao {

    @Query("SELECT * FROM recordatorios WHERE vehiculoId = :vehiculoId ORDER BY fechaLimite ASC")
    fun obtenerPorVehiculo(vehiculoId: String): Flow<List<Recordatorio>>

    @Query("SELECT * FROM recordatorios WHERE vehiculoId = :vehiculoId ORDER BY fechaLimite ASC")
    suspend fun obtenerPorVehiculoLista(vehiculoId: String): List<Recordatorio>

    @Query("SELECT * FROM recordatorios WHERE vehiculoId = :vehiculoId AND completado = 0 ORDER BY fechaLimite ASC")
    fun obtenerPendientesPorVehiculo(vehiculoId: String): Flow<List<Recordatorio>>

    @Query("SELECT * FROM recordatorios WHERE id = :id")
    suspend fun obtenerPorId(id: String): Recordatorio?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(recordatorio: Recordatorio)

    @Update
    suspend fun actualizar(recordatorio: Recordatorio)

    @Delete
    suspend fun eliminar(recordatorio: Recordatorio)
}
