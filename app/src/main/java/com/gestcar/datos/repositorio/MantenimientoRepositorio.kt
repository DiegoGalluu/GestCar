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

            runCatching { sincronizarMantenimiento(mantenimientoActualizado) }
                .onFailure { return Result.failure(it) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(mantenimiento: Mantenimiento): Result<Unit> {
        return guardar(mantenimiento)
    }

    suspend fun cambiarEstado(
        mantenimiento: Mantenimiento,
        realizado: Boolean,
        fechaRealizado: Long?
    ): Result<Mantenimiento> {
        return try {
            val mantenimientoActualizado = mantenimiento.copy(
                realizado = realizado,
                fechaRealizado = fechaRealizado,
                actualizadoEn = System.currentTimeMillis()
            )

            mantenimientoDao.insertar(mantenimientoActualizado)

            ClienteSupabase.cliente.postgrest[tablaRemota]
                .update(
                    update = {
                        set("realizado", mantenimientoActualizado.realizado)
                        if (mantenimientoActualizado.fechaRealizado == null) {
                            setToNull("fecha_realizado")
                        } else {
                            set("fecha_realizado", mantenimientoActualizado.fechaRealizado)
                        }
                        set("actualizado_en", mantenimientoActualizado.actualizadoEn)
                    },
                    request = {
                        filter { eq("id", mantenimientoActualizado.id) }
                        select(Columns.list("id"))
                    }
                )

            Result.success(mantenimientoActualizado)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
            val mantenimientosLocales = mantenimientoDao.obtenerPorVehiculoLista(vehiculoId)
            val mantenimientosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<MantenimientoDto>()
            }.getOrElse { return Result.failure(it) }
            val mantenimientosRemotosPorId = mantenimientosRemotos.associateBy { it.id }

            mantenimientosLocales.forEach { mantenimiento ->
                if (mantenimientosRemotosPorId[mantenimiento.id] == null) {
                    mantenimientoDao.eliminarPorId(mantenimiento.id)
                }
            }

            mantenimientosRemotos.forEach { dto ->
                val mantenimiento = dto.aEntidad()
                val mantenimientoLocal = mantenimientoDao.obtenerPorId(mantenimiento.id)
                val remotoEsMasNuevo = mantenimientoLocal == null ||
                    mantenimiento.actualizadoEn >= mantenimientoLocal.actualizadoEn ||
                    mantenimiento.realizado != mantenimientoLocal.realizado ||
                    mantenimiento.fechaRealizado != mantenimientoLocal.fechaRealizado

                if (!remotoEsMasNuevo) {
                    return@forEach
                }

                mantenimientoDao.insertar(mantenimiento)
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
                sincronizar(vehiculo.id).onFailure { errores.add(it) }
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
