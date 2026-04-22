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

    fun obtenerMantenimientos(vehiculoId: String): Flow<List<Mantenimiento>> {
        return mantenimientoDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): Mantenimiento? {
        return mantenimientoDao.obtenerPorId(id)
    }

    suspend fun guardar(mantenimiento: Mantenimiento): Result<Unit> {
        return try {
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
            mantenimientoDao.obtenerPorVehiculoLista(vehiculoId).forEach { mantenimiento ->
                runCatching { sincronizarMantenimiento(mantenimiento) }
            }

            val mantenimientosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<MantenimientoDto>()
            }.getOrElse { emptyList() }

            mantenimientosRemotos.forEach { dto ->
                mantenimientoDao.insertar(dto.aEntidad())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizarPendientesDelUsuario(usuarioId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()
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

        vehiculoDao.actualizar(
            vehiculo.copy(
                kilometraje = kilometros,
                actualizadoEn = System.currentTimeMillis()
            )
        )
    }
}
