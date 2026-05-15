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
    private val tablaVehiculosRemota = "vehiculos"

    // room emite la lista como flow
    // asi la pantalla se actualiza sola cuando se guarda o sincroniza un repostaje
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
            // el importe se calcula aqui para que la ui no sea la unica fuente de verdad
            // si en el futuro se guarda desde otro sitio seguira siendo coherente
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
            val errores = mutableListOf<Throwable>()
            val repostajesLocales = repostajeDao.obtenerPorVehiculoLista(vehiculoId)
            val repostajesRemotosIniciales = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<RepostajeDto>()
            }.getOrElse { emptyList() }
            val repostajesRemotosPorId = repostajesRemotosIniciales.associateBy { it.id }

            repostajesLocales.forEach { repostaje ->
                val repostajeRemoto = repostajesRemotosPorId[repostaje.id]
                val localEsMasNuevo = repostajeRemoto == null ||
                    repostaje.actualizadoEn > repostajeRemoto.actualizadoEn

                if (!localEsMasNuevo) {
                    return@forEach
                }

                runCatching { sincronizarRepostaje(repostaje) }
                    .onFailure { errores.add(it) }
            }

            val repostajesRemotos = runCatching {
                ClienteSupabase.cliente.postgrest[tablaRemota]
                    .select { filter { eq("vehiculo_id", vehiculoId) } }
                    .decodeList<RepostajeDto>()
            }.getOrElse { repostajesRemotosIniciales }

            repostajesRemotos.forEach { dto ->
                val repostaje = dto.aEntidad()
                val repostajeLocal = repostajeDao.obtenerPorId(repostaje.id)
                val remotoEsMasNuevo = repostajeLocal == null ||
                    repostaje.actualizadoEn > repostajeLocal.actualizadoEn

                if (!remotoEsMasNuevo) {
                    return@forEach
                }

                repostajeDao.insertar(repostaje)
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

    suspend fun calcularConsumoMedio(vehiculoId: String): Double {
        // solo usamos repostajes marcados como deposito lleno
        // si no, el calculo de litros por cien puede salir completamente falseado
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
        sincronizarVehiculoPadreSiHaceFalta(repostaje.vehiculoId)

        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = repostaje.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }

    private suspend fun sincronizarVehiculoPadreSiHaceFalta(vehiculoId: String) {
        val vehiculo = vehiculoDao.obtenerPorId(vehiculoId) ?: return

        // supabase exige que exista el vehiculo antes del repostaje
        // si no lo aseguramos aqui, la foreign key o la rls pueden rechazar el alta
        ClienteSupabase.cliente.postgrest[tablaVehiculosRemota]
            .upsert(
                value = vehiculo.aDto(),
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

        // si el repostaje trae un odometro mas alto actualizamos la ficha del vehiculo
        // asi el kilometraje global avanza sin pedirle al usuario que lo edite a mano
        val vehiculoActualizado = vehiculo.copy(
            kilometraje = repostaje.kilometros,
            actualizadoEn = System.currentTimeMillis()
        )
        vehiculoDao.actualizar(vehiculoActualizado)
    }
}
