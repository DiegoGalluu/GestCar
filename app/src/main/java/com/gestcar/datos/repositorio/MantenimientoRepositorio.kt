package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.MantenimientoDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.MantenimientoDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

class MantenimientoRepositorio(
    private val mantenimientoDao: MantenimientoDao,
    private val vehiculoDao: VehiculoDao
) {
    private val tablaRemota = "mantenimientos"
    private val tablaVehiculosRemota = "vehiculos"

    // mantenimientos tambien funciona como checklist
    // una operacion puede estar pendiente o realizada sin salir de la misma tabla
    fun obtenerMantenimientos(vehiculoId: String): Flow<List<Mantenimiento>> {
        return mantenimientoDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): Mantenimiento? {
        return mantenimientoDao.obtenerPorId(id)
    }

    suspend fun obtenerMantenimientosLocales(vehiculoId: String): List<Mantenimiento> {
        return mantenimientoDao.obtenerPorVehiculoLista(vehiculoId)
    }

    suspend fun guardar(mantenimiento: Mantenimiento): Result<Unit> {
        return try {
            // normalizamos texto y categoria antes de guardar
            // asi evitamos duplicados visuales por espacios o mayusculas mezcladas
            val mantenimientoActualizado = mantenimiento.copy(
                tipo = mantenimiento.tipo.trim(),
                categoria = mantenimiento.categoria.uppercase(),
                actualizadoEn = System.currentTimeMillis()
            )

            mantenimientoDao.insertar(mantenimientoActualizado)
            actualizarKilometrajeVehiculoSiHaceFalta(mantenimientoActualizado)

            // primero manda room, si supabase falla lo reintentaremos luego
            runCatching { sincronizarMantenimiento(mantenimientoActualizado) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(mantenimiento: Mantenimiento): Result<Unit> {
        return guardar(mantenimiento)
    }

    suspend fun eliminar(mantenimiento: Mantenimiento): Result<Unit> {
        return try {
            mantenimientoDao.eliminar(mantenimiento)

            runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .delete { filter { eq("id", mantenimiento.id) } }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(vehiculoId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()
            val mantenimientosLocales = mantenimientoDao.obtenerPorVehiculoLista(vehiculoId)
            val mantenimientosRemotosIniciales = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<MantenimientoDto>()
            }.getOrElse { emptyList() }
            val mantenimientosRemotosPorId = mantenimientosRemotosIniciales.associateBy { it.id }

            mantenimientosLocales.forEach { mantenimiento ->
                val mantenimientoRemoto = mantenimientosRemotosPorId[mantenimiento.id]
                val localEsMasNuevo = mantenimientoRemoto == null ||
                    mantenimiento.actualizadoEn > mantenimientoRemoto.actualizadoEn

                if (!localEsMasNuevo) {
                    return@forEach
                }

                runCatching { sincronizarMantenimiento(mantenimiento) }
                    .onFailure { errores.add(it) }
            }

            val mantenimientosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<MantenimientoDto>()
            }.getOrElse { mantenimientosRemotosIniciales }

            mantenimientosRemotos.forEach { dto ->
                val mantenimiento = dto.aEntidad()
                val mantenimientoLocal = mantenimientoDao.obtenerPorId(mantenimiento.id)
                val remotoEsMasNuevo = mantenimientoLocal == null ||
                    mantenimiento.actualizadoEn > mantenimientoLocal.actualizadoEn

                if (!remotoEsMasNuevo) {
                    return@forEach
                }

                mantenimientoDao.insertar(mantenimiento)
            }

            if (errores.isEmpty()) {
                Result.success(Unit)
            } else {
                Result.failure(errores.first())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizarPendientesDelUsuario(usuarioId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()
            // mantenimientos depende de vehiculos
            // por eso sincronizamos recorriendo los vehiculos del usuario
            vehiculoDao.obtenerVehiculosPorUsuarioLista(usuarioId).forEach { vehiculo ->
                mantenimientoDao.obtenerPorVehiculoLista(vehiculo.id).forEach { mantenimiento ->
                    runCatching { sincronizarMantenimiento(mantenimiento) }
                        .onFailure { errores.add(it) }
                }
            }

            if (errores.isNotEmpty()) {
                return Result.failure(errores.first())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sincronizarMantenimiento(mantenimiento: Mantenimiento) {
        sincronizarVehiculoPadreSiHaceFalta(mantenimiento.vehiculoId)

        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = mantenimiento.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }

    private suspend fun sincronizarVehiculoPadreSiHaceFalta(vehiculoId: String) {
        val vehiculo = vehiculoDao.obtenerPorId(vehiculoId) ?: return

        // si el mantenimiento se creo sin internet puede que el vehiculo tampoco este remoto
        // hacemos upsert del padre antes del hijo para respetar foreign keys y rls
        ClienteSupabase.cliente.postgrest[tablaVehiculosRemota]
            .upsert(
                value = vehiculo.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }

    private suspend fun actualizarKilometrajeVehiculoSiHaceFalta(mantenimiento: Mantenimiento) {
        val kilometros = mantenimiento.kilometros ?: return
        val vehiculo = vehiculoDao.obtenerPorId(mantenimiento.vehiculoId) ?: return
        if (kilometros <= vehiculo.kilometraje) {
            return
        }

        // algunas operaciones se hacen a mas kilometros que la ficha actual
        // actualizarlo aqui evita pedir ese dato por duplicado al usuario
        vehiculoDao.actualizar(
            vehiculo.copy(
                kilometraje = kilometros,
                actualizadoEn = System.currentTimeMillis()
            )
        )
    }
}
