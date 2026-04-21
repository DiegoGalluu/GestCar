package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestcar.datos.entidades.GastoPeriodico
import kotlinx.coroutines.flow.Flow

@Dao
interface GastoPeriodicoDao {

    @Query("SELECT * FROM gastos_periodicos WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    fun obtenerPorVehiculo(vehiculoId: String): Flow<List<GastoPeriodico>>

    @Query("SELECT * FROM gastos_periodicos WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    suspend fun obtenerPorVehiculoLista(vehiculoId: String): List<GastoPeriodico>

    @Query("SELECT * FROM gastos_periodicos WHERE id = :id")
    suspend fun obtenerPorId(id: String): GastoPeriodico?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(gasto: GastoPeriodico)

    @Update
    suspend fun actualizar(gasto: GastoPeriodico)

    @Delete
    suspend fun eliminar(gasto: GastoPeriodico)
}
