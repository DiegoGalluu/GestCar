package com.gestcar.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gestcar.datos.repositorio.GasolineraRepositorio

class GasolinerasWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        val soloSiCacheCaducada = inputData.getBoolean(CLAVE_SOLO_SI_CACHE_CADUCADA, false)
        val resultado = GasolineraRepositorio(applicationContext).actualizarCacheGasolineras(
            soloSiCacheCaducada = soloSiCacheCaducada
        )
        return if (resultado.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val CLAVE_SOLO_SI_CACHE_CADUCADA = "solo_si_cache_caducada"
    }
}
