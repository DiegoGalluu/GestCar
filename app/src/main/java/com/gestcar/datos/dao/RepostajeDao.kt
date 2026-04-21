package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gestcar.datos.entidades.Repostaje
import kotlinx.coroutines.flow.Flow

@Dao
interface RepostajeDao {

    @Query("SELECT * FROM repostajes WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    fun obtenerPorVehiculo(vehiculoId: String): Flow<List<Repostaje>>

    @Query("SELECT * FROM repostajes WHERE vehiculoId = :vehiculoId ORDER BY fecha DESC")
    suspend fun obtenerPorVehiculoLista(vehiculoId: String): List<Repostaje>

    @Query("SELECT * FROM repostajes WHERE id = :id")
    suspend fun obtenerPorId(id: String): Repostaje?

    @Query("SELECT MAX(kilometros) FROM repostajes WHERE vehiculoId = :vehiculoId")
    suspend fun obtenerUltimoKilometraje(vehiculoId: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(repostaje: Repostaje)

    @Update
    suspend fun actualizar(repostaje: Repostaje)

    @Delete
    suspend fun eliminar(repostaje: Repostaje)
}
