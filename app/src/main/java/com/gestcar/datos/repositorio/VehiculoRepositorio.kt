package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.VehiculoDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import com.gestcar.util.normalizarRutaImagenVehiculo
import java.io.File

// repositorio de vehiculos, es el intermediario entre la ui y los datos
// se encarga de guardar en local (room) y sincronizar con supabase
// la estrategia es guardar primero en local y luego intentar subir a supabase
// asi la app funciona siempre aunque no haya internet
class VehiculoRepositorio(
    private val vehiculoDao: VehiculoDao
) {
    // nombre de la tabla en supabase
    private val tablaRemota = "vehiculos"

    // obtener todos los vehiculos del usuario como flow reactivo
    // esto devuelve los datos locales de room
    fun obtenerVehiculos(usuarioId: String): Flow<List<Vehiculo>> {
        return vehiculoDao.obtenerVehiculosPorUsuario(usuarioId)
    }

    // obtener todos los vehiculos locales de una sola vez, util para sincronizar pendientes
    private suspend fun obtenerVehiculosLocales(usuarioId: String): List<Vehiculo> {
        return vehiculoDao.obtenerVehiculosPorUsuarioLista(usuarioId)
    }

    // obtener un vehiculo por su id desde la base de datos local
    suspend fun obtenerPorId(id: String): Vehiculo? {
        return vehiculoDao.obtenerPorId(id)
    }

    // guardar un vehiculo nuevo, primero en local y luego intenta subirlo a supabase
    suspend fun guardar(vehiculo: Vehiculo): Result<Unit> {
        return try {
            // guardamos en room primero para que la app sea responsive
            vehiculoDao.insertar(vehiculo)

            sincronizarVehiculo(vehiculo)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // actualizar un vehiculo existente en local y en remoto
    suspend fun actualizar(vehiculo: Vehiculo): Result<Unit> {
        return try {
            val vehiculoActualizado = vehiculo.copy(
                actualizadoEn = System.currentTimeMillis()
            )
            vehiculoDao.actualizar(vehiculoActualizado)

            sincronizarVehiculo(vehiculoActualizado)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // eliminar un vehiculo de local y de supabase
    suspend fun eliminar(vehiculo: Vehiculo): Result<Unit> {
        return try {
            vehiculoDao.eliminar(vehiculo)

            ClienteSupabase.cliente.postgrest[tablaRemota]
                .delete { filter { eq("id", vehiculo.id) } }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // sincronizar los vehiculos remotos con los locales
    // primero intenta subir los cambios locales pendientes y despues descarga el estado remoto
    // esto permite trabajar sin internet y sincronizar en cuanto vuelva la conexion
    suspend fun sincronizar(usuarioId: String): Result<Unit> {
        try {
            val vehiculosLocales = obtenerVehiculosLocales(usuarioId)
            vehiculosLocales.forEach { vehiculo ->
                sincronizarVehiculo(vehiculo)
            }

            val vehiculosRemotos = ClienteSupabase.cliente.postgrest[tablaRemota]
                .select { filter { eq("usuario_id", usuarioId) } }
                .decodeList<VehiculoDto>()

            // guardamos cada vehiculo remoto en local, replace si ya existe
            vehiculosRemotos.forEach { dto ->
                vehiculoDao.insertar(dto.aEntidad())
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun actualizarImagenVehiculo(vehiculo: Vehiculo, imagenUriLocal: String): Result<Vehiculo> {
        val vehiculoActualizado = vehiculo.copy(
            imagenUri = imagenUriLocal,
            actualizadoEn = System.currentTimeMillis()
        )

        return try {
            vehiculoDao.actualizar(vehiculoActualizado)
            val vehiculoSincronizado = sincronizarVehiculo(vehiculoActualizado)
            Result.success(vehiculoSincronizado)
        } catch (e: Exception) {
            Result.success(vehiculoActualizado)
        }
    }

    private suspend fun sincronizarVehiculo(vehiculo: Vehiculo): Vehiculo {
        val vehiculoPreparado = subirImagenSiHaceFalta(vehiculo)
        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = vehiculoPreparado.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
        return vehiculoPreparado
    }

    private suspend fun subirImagenSiHaceFalta(vehiculo: Vehiculo): Vehiculo {
        val imagenUri = normalizarRutaImagenVehiculo(vehiculo.imagenUri) ?: return vehiculo
        if (!imagenUri.startsWith("file://")) {
            return vehiculo.copy(imagenUri = imagenUri)
        }

        val archivoImagen = File(requireNotNull(android.net.Uri.parse(imagenUri).path))
        if (!archivoImagen.exists()) {
            return vehiculo.copy(imagenUri = null)
        }

        val rutaRemota = "${vehiculo.usuarioId}/${vehiculo.id}.jpg"
        val bucket = ClienteSupabase.cliente.storage.from(ClienteSupabase.BUCKET_FOTOS_VEHICULOS)
        bucket.upload(rutaRemota, archivoImagen.readBytes()) {
            upsert = true
        }

        val vehiculoConImagenRemota = vehiculo.copy(
            imagenUri = rutaRemota,
            actualizadoEn = System.currentTimeMillis()
        )
        vehiculoDao.actualizar(vehiculoConImagenRemota)
        return vehiculoConImagenRemota
    }
}
