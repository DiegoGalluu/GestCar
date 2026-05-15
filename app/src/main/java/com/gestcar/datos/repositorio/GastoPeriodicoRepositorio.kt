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

    // gastos se usa para seguros itv impuestos peajes y pagos sueltos
    // el estado pagado permite separar pendientes de historico sin duplicar tablas
    fun obtenerGastos(vehiculoId: String): Flow<List<GastoPeriodico>> {
        return gastoPeriodicoDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): GastoPeriodico? {
        return gastoPeriodicoDao.obtenerPorId(id)
    }

    suspend fun obtenerGastosLocales(vehiculoId: String): List<GastoPeriodico> {
        return gastoPeriodicoDao.obtenerPorVehiculoLista(vehiculoId)
    }

    suspend fun guardar(gasto: GastoPeriodico): Result<Unit> {
        return try {
            // periodicidad se guarda en mayusculas para que los filtros no dependan del texto de ui
            // concepto se recorta para evitar espacios invisibles al comparar o mostrar
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
            val errores = mutableListOf<Throwable>()
            val gastosLocales = gastoPeriodicoDao.obtenerPorVehiculoLista(vehiculoId)
            val gastosRemotosIniciales = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<GastoPeriodicoDto>()
            }.getOrElse { emptyList() }
            val gastosRemotosPorId = gastosRemotosIniciales.associateBy { it.id }

            gastosLocales.forEach { gasto ->
                val gastoRemoto = gastosRemotosPorId[gasto.id]
                val localEsMasNuevo = gastoRemoto == null ||
                    gasto.actualizadoEn > gastoRemoto.actualizadoEn

                if (!localEsMasNuevo) {
                    return@forEach
                }

                runCatching { sincronizarGasto(gasto) }
                    .onFailure { errores.add(it) }
            }

            val gastosRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<GastoPeriodicoDto>()
            }.getOrElse { gastosRemotosIniciales }

            gastosRemotos.forEach { dto ->
                val gasto = dto.aEntidad()
                val gastoLocal = gastoPeriodicoDao.obtenerPorId(gasto.id)
                val remotoEsMasNuevo = gastoLocal == null ||
                    gasto.actualizadoEn > gastoLocal.actualizadoEn

                if (!remotoEsMasNuevo) {
                    return@forEach
                }

                gastoPeriodicoDao.insertar(gasto)
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
            // si una sync falla devolvemos fallo para que workmanager pueda reintentar
            // pero seguimos probando con el resto para no bloquear todo por un solo registro
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

        // los gastos necesitan que el vehiculo exista en supabase
        // hacer upsert del padre aqui hace mas robusto el modo offline
        ClienteSupabase.cliente.postgrest[tablaVehiculosRemota]
            .upsert(
                value = vehiculo.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }
}
