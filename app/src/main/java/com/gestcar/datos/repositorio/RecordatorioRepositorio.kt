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

    fun obtenerRecordatorios(vehiculoId: String): Flow<List<Recordatorio>> {
        return recordatorioDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): Recordatorio? {
        return recordatorioDao.obtenerPorId(id)
    }

    suspend fun guardar(recordatorio: Recordatorio): Result<Unit> {
        return try {
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
            recordatorioDao.obtenerPorVehiculoLista(vehiculoId).forEach { recordatorio ->
                runCatching { sincronizarRecordatorio(recordatorio) }
            }

            val recordatoriosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<RecordatorioDto>()
            }.getOrElse { emptyList() }

            recordatoriosRemotos.forEach { dto ->
                recordatorioDao.insertar(dto.aEntidad())
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

        ClienteSupabase.cliente.postgrest[tablaVehiculosRemota]
            .upsert(
                value = vehiculo.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }
}
