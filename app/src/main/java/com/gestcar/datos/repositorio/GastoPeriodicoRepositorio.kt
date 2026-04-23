package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.GastoPeriodicoDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.GastoPeriodicoDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

class GastoPeriodicoRepositorio(
    private val gastoPeriodicoDao: GastoPeriodicoDao,
    private val vehiculoDao: VehiculoDao
) {
    private val tablaRemota = "gastos_periodicos"
    private val tablaVehiculosRemota = "vehiculos"

    fun obtenerGastos(vehiculoId: String): Flow<List<GastoPeriodico>> {
        return gastoPeriodicoDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): GastoPeriodico? {
        return gastoPeriodicoDao.obtenerPorId(id)
    }

    suspend fun guardar(gasto: GastoPeriodico): Result<Unit> {
        return try {
            val gastoActualizado = gasto.copy(
                concepto = gasto.concepto.trim(),
                periodicidad = gasto.periodicidad?.uppercase(),
                actualizadoEn = System.currentTimeMillis()
            )

            gastoPeriodicoDao.insertar(gastoActualizado)

            // room manda primero, supabase ya se pondra al dia cuando pueda
            runCatching { sincronizarGasto(gastoActualizado) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(gasto: GastoPeriodico): Result<Unit> {
        return guardar(gasto)
    }

    suspend fun eliminar(gasto: GastoPeriodico): Result<Unit> {
        return try {
            gastoPeriodicoDao.eliminar(gasto)

            runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .delete { filter { eq("id", gasto.id) } }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(vehiculoId: String): Result<Unit> {
        return try {
            gastoPeriodicoDao.obtenerPorVehiculoLista(vehiculoId).forEach { gasto ->
                runCatching { sincronizarGasto(gasto) }
            }

            val gastosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<GastoPeriodicoDto>()
            }.getOrElse { emptyList() }

            gastosRemotos.forEach { dto ->
                gastoPeriodicoDao.insertar(dto.aEntidad())
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
                gastoPeriodicoDao.obtenerPorVehiculoLista(vehiculo.id).forEach { gasto ->
                    runCatching { sincronizarGasto(gasto) }
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

    private suspend fun sincronizarGasto(gasto: GastoPeriodico) {
        sincronizarVehiculoPadreSiHaceFalta(gasto.vehiculoId)

        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = gasto.aDto(),
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
