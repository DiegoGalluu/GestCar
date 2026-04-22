package com.gestcar.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.repositorio.RepostajeRepositorio
import com.gestcar.datos.repositorio.VehiculoRepositorio
import io.github.jan.supabase.auth.auth

class SincronizacionWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        return try {
            ClienteSupabase.cliente.auth.awaitInitialization()
            ClienteSupabase.cliente.auth.loadFromStorage()

            val usuarioId = ClienteSupabase.cliente.auth
                .currentSessionOrNull()
                ?.user
                ?.id
                .orEmpty()

            if (usuarioId.isBlank()) {
                return Result.success()
            }

            val baseDatos = GestCarBaseDatos.obtenerInstancia(applicationContext)
            val vehiculoRepositorio = VehiculoRepositorio(baseDatos.vehiculoDao())
            val repostajeRepositorio = RepostajeRepositorio(
                repostajeDao = baseDatos.repostajeDao(),
                vehiculoDao = baseDatos.vehiculoDao()
            )

            val resultadoVehiculos = vehiculoRepositorio.sincronizar(usuarioId)
            val resultadoRepostajes = repostajeRepositorio.sincronizarPendientesDelUsuario(usuarioId)

            if (resultadoVehiculos.isFailure || resultadoRepostajes.isFailure) {
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
