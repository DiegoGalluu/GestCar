package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.RecordatorioDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.RecordatorioDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

class RecordatorioRepositorio(
    private val recordatorioDao: RecordatorioDao,
    private val vehiculoDao: VehiculoDao
) {
    private val tablaRemota = "recordatorios"
    private val tablaVehiculosRemota = "vehiculos"

    // los recordatorios son independientes de gastos o mantenimientos
    // sirven para avisos libres del usuario por fecha kilometraje o ambas cosas
    fun obtenerRecordatorios(vehiculoId: String): Flow<List<Recordatorio>> {
        return recordatorioDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): Recordatorio? {
        return recordatorioDao.obtenerPorId(id)
    }

    suspend fun guardar(recordatorio: Recordatorio): Result<Unit> {
        return try {
            // recortar el concepto evita que dos recordatorios parezcan distintos
            // solo por espacios introducidos con el teclado
            val recordatorioActualizado = recordatorio.copy(
                concepto = recordatorio.concepto.trim(),
                actualizadoEn = System.currentTimeMillis()
            )

            recordatorioDao.insertar(recordatorioActualizado)

            // guardamos primero en room, si la red falla se reintentara luego
            runCatching { sincronizarRecordatorio(recordatorioActualizado) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(recordatorio: Recordatorio): Result<Unit> {
        return try {
            recordatorioDao.eliminar(recordatorio)

            runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .delete { filter { eq("id", recordatorio.id) } }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(vehiculoId: String): Result<Unit> {
        return try {
            // el usuario puede crear recordatorios sin red
            // al volver la conexion se suben antes de descargar lo remoto
            val errores = mutableListOf<Throwable>()
            val recordatoriosLocales = recordatorioDao.obtenerPorVehiculoLista(vehiculoId)
            val recordatoriosRemotosIniciales = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<RecordatorioDto>()
            }.getOrElse { emptyList() }
            val recordatoriosRemotosPorId = recordatoriosRemotosIniciales.associateBy { it.id }

            recordatoriosLocales.forEach { recordatorio ->
                val recordatorioRemoto = recordatoriosRemotosPorId[recordatorio.id]
                val localEsMasNuevo = recordatorioRemoto == null ||
                    recordatorio.actualizadoEn > recordatorioRemoto.actualizadoEn

                if (!localEsMasNuevo) {
                    return@forEach
                }

                runCatching { sincronizarRecordatorio(recordatorio) }
                    .onFailure { errores.add(it) }
            }

            val recordatoriosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<RecordatorioDto>()
            }.getOrElse { recordatoriosRemotosIniciales }

            recordatoriosRemotos.forEach { dto ->
                val recordatorio = dto.aEntidad()
                val recordatorioLocal = recordatorioDao.obtenerPorId(recordatorio.id)
                val remotoEsMasNuevo = recordatorioLocal == null ||
                    recordatorio.actualizadoEn > recordatorioLocal.actualizadoEn

                if (!remotoEsMasNuevo) {
                    return@forEach
                }

                recordatorioDao.insertar(recordatorio)
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
            vehiculoDao.obtenerVehiculosPorUsuarioLista(usuarioId).forEach { vehiculo ->
                recordatorioDao.obtenerPorVehiculoLista(vehiculo.id).forEach { recordatorio ->
                    runCatching { sincronizarRecordatorio(recordatorio) }
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

    private suspend fun sincronizarRecordatorio(recordatorio: Recordatorio) {
        sincronizarVehiculoPadreSiHaceFalta(recordatorio.vehiculoId)

        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = recordatorio.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }

    private suspend fun sincronizarVehiculoPadreSiHaceFalta(vehiculoId: String) {
        val vehiculo = vehiculoDao.obtenerPorId(vehiculoId) ?: return

        // la rls de supabase valida la propiedad a traves del vehiculo
        // por eso debe existir el vehiculo remoto antes de insertar recordatorios
        ClienteSupabase.cliente.postgrest[tablaVehiculosRemota]
            .upsert(
                value = vehiculo.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }
}
