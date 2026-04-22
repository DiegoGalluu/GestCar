package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.gestcar.datos.entidades.Vehiculo
import kotlinx.coroutines.flow.Flow

// dao de vehiculos, aqui se definen todas las consultas a la base de datos local
// room se encarga de generar la implementacion automaticamente
@Dao
interface VehiculoDao {

    // obtener todos los vehiculos de un usuario como flow
    // flow hace que la lista se actualice automaticamente cuando hay cambios
    @Query("SELECT * FROM vehiculos WHERE usuarioId = :usuarioId ORDER BY fechaAlta DESC")
    fun obtenerVehiculosPorUsuario(usuarioId: String): Flow<List<Vehiculo>>

    // obtener todos los vehiculos de un usuario una sola vez, util para sincronizaciones
    @Query("SELECT * FROM vehiculos WHERE usuarioId = :usuarioId ORDER BY fechaAlta DESC")
    suspend fun obtenerVehiculosPorUsuarioLista(usuarioId: String): List<Vehiculo>

    // obtener un vehiculo concreto por su id
    @Query("SELECT * FROM vehiculos WHERE id = :id")
    suspend fun obtenerPorId(id: String): Vehiculo?

    // usamos upsert porque replace borra la fila antes de insertarla
    // y eso dispara el borrado en cascada de repostajes y futuros gastos
    @Upsert
    suspend fun insertar(vehiculo: Vehiculo)

    // actualizar un vehiculo existente
    @Update
    suspend fun actualizar(vehiculo: Vehiculo)

    // eliminar un vehiculo
    @Delete
    suspend fun eliminar(vehiculo: Vehiculo)

    // eliminar todos los vehiculos de un usuario, se usa al cerrar sesion
    @Query("DELETE FROM vehiculos WHERE usuarioId = :usuarioId")
    suspend fun eliminarTodosPorUsuario(usuarioId: String)
}
