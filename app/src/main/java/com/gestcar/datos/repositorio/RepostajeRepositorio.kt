package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.RepostajeDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Repostaje
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.RepostajeDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

class RepostajeRepositorio(
    private val repostajeDao: RepostajeDao,
    private val vehiculoDao: VehiculoDao
) {
    private val tablaRemota = "repostajes"

    fun obtenerRepostajes(vehiculoId: String): Flow<List<Repostaje>> {
        return repostajeDao.obtenerPorVehiculo(vehiculoId)
    }

    suspend fun obtenerPorId(id: String): Repostaje? {
        return repostajeDao.obtenerPorId(id)
    }

    suspend fun obtenerUltimoKilometraje(vehiculoId: String): Double? {
        return repostajeDao.obtenerUltimoKilometraje(vehiculoId)
    }

    suspend fun guardar(repostaje: Repostaje): Result<Unit> {
        return try {
            val repostajeActualizado = repostaje.copy(
                importeTotal = repostaje.litros * repostaje.precioPorLitro,
                actualizadoEn = System.currentTimeMillis()
            )
            repostajeDao.insertar(repostajeActualizado)
            actualizarKilometrajeVehiculoSiHaceFalta(repostajeActualizado)

            // si supabase falla no bloqueamos el guardado local
            // el repostaje queda en room y se subira en una sincronizacion posterior
            runCatching { sincronizarRepostaje(repostajeActualizado) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizar(repostaje: Repostaje): Result<Unit> {
        return guardar(repostaje)
    }

    suspend fun eliminar(repostaje: Repostaje): Result<Unit> {
        return try {
            repostajeDao.eliminar(repostaje)

            // borramos en remoto si se puede, pero la accion local manda
            runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .delete { filter { eq("id", repostaje.id) } }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(vehiculoId: String): Result<Unit> {
        return try {
            repostajeDao.obtenerPorVehiculoLista(vehiculoId).forEach { repostaje ->
                runCatching { sincronizarRepostaje(repostaje) }
            }

            val repostajesRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<RepostajeDto>()
            }.getOrElse { emptyList() }

            repostajesRemotos.forEach { dto ->
                repostajeDao.insertar(dto.aEntidad())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calcularConsumoMedio(vehiculoId: String): Double {
        val repostajes = repostajeDao.obtenerPorVehiculoLista(vehiculoId)
            .filter { it.llenoCompleto }
            .sortedBy { it.kilometros }

        if (repostajes.size < 2) {
            return 0.0
        }

        val consumos = repostajes.zipWithNext().mapNotNull { (anterior, actual) ->
            val kilometrosRecorridos = actual.kilometros - anterior.kilometros
            if (kilometrosRecorridos <= 0 || actual.litros <= 0) {
                null
            } else {
                (actual.litros / kilometrosRecorridos) * 100
            }
        }

        return consumos.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private suspend fun sincronizarRepostaje(repostaje: Repostaje) {
        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = repostaje.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }

    private suspend fun actualizarKilometrajeVehiculoSiHaceFalta(repostaje: Repostaje) {
        val vehiculo = vehiculoDao.obtenerPorId(repostaje.vehiculoId) ?: return
        if (repostaje.kilometros <= vehiculo.kilometraje) {
            return
        }

        val vehiculoActualizado = vehiculo.copy(
            kilometraje = repostaje.kilometros,
            actualizadoEn = System.currentTimeMillis()
        )
        vehiculoDao.actualizar(vehiculoActualizado)
    }
}
