package com.gestcar.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.repositorio.DocumentacionRepositorio
import com.gestcar.datos.repositorio.GastoPeriodicoRepositorio
import com.gestcar.datos.repositorio.MantenimientoRepositorio
import com.gestcar.datos.repositorio.RecordatorioRepositorio
import com.gestcar.datos.repositorio.RepostajeRepositorio
import com.gestcar.datos.repositorio.VehiculoRepositorio
import io.github.jan.supabase.auth.auth

class SincronizacionWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        return try {
            // workmanager puede ejecutar este worker con la app cerrada
            // por eso cargamos la sesion guardada antes de tocar supabase
            ClienteSupabase.cliente.auth.awaitInitialization()
            ClienteSupabase.cliente.auth.loadFromStorage()

            val usuarioId = ClienteSupabase.cliente.auth
                .currentSessionOrNull()
                ?.user
                ?.id
                .orEmpty()

            if (usuarioId.isBlank()) {
                // si no hay sesion no hay nada que sincronizar
                // devolvemos success para no entrar en un bucle de reintentos inutil
                return Result.success()
            }

            val baseDatos = GestCarBaseDatos.obtenerInstancia(applicationContext)
            val vehiculoRepositorio = VehiculoRepositorio(baseDatos.vehiculoDao())
            val repostajeRepositorio = RepostajeRepositorio(
                repostajeDao = baseDatos.repostajeDao(),
                vehiculoDao = baseDatos.vehiculoDao()
            )
            val mantenimientoRepositorio = MantenimientoRepositorio(
                mantenimientoDao = baseDatos.mantenimientoDao(),
                vehiculoDao = baseDatos.vehiculoDao()
            )
            val gastoRepositorio = GastoPeriodicoRepositorio(
                gastoPeriodicoDao = baseDatos.gastoPeriodicoDao(),
                vehiculoDao = baseDatos.vehiculoDao()
            )
            val recordatorioRepositorio = RecordatorioRepositorio(
                recordatorioDao = baseDatos.recordatorioDao(),
                vehiculoDao = baseDatos.vehiculoDao()
            )
            val documentacionRepositorio = DocumentacionRepositorio(
                documentoDao = baseDatos.documentoVehiculoDao(),
                campoDao = baseDatos.campoDocumentoDao(),
                vehiculoDao = baseDatos.vehiculoDao()
            )

            val resultadoVehiculos = vehiculoRepositorio.sincronizar(usuarioId)
            val resultadoRepostajes = repostajeRepositorio.sincronizarPendientesDelUsuario(usuarioId)
            val resultadoMantenimientos = mantenimientoRepositorio.sincronizarPendientesDelUsuario(usuarioId)
            val resultadoGastos = gastoRepositorio.sincronizarPendientesDelUsuario(usuarioId)
            val resultadoRecordatorios = recordatorioRepositorio.sincronizarPendientesDelUsuario(usuarioId)
            val resultadoDocumentacion = documentacionRepositorio.sincronizarPendientesDelUsuario(usuarioId)

            if (
                resultadoVehiculos.isFailure ||
                resultadoRepostajes.isFailure ||
                resultadoMantenimientos.isFailure ||
                resultadoGastos.isFailure ||
                resultadoRecordatorios.isFailure ||
                resultadoDocumentacion.isFailure
            ) {
                // retry permite que android lo intente de nuevo con backoff
                // esto es ideal para errores temporales de red o supabase pausado
                Result.retry()
            } else {
                Result.success()
            }
        } catch (e: Exception) {
            // cualquier error inesperado se trata como temporal
            // mejor reintentar mas tarde que perder la oportunidad de sincronizar
            Result.retry()
        }
    }
}
