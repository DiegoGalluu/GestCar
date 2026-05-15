package com.gestcar.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object PlanificadorGasolineras {

    private const val TRABAJO_WIFI = "actualizacion_gasolineras_wifi_gestcar"
    private const val TRABAJO_RESPALDO = "actualizacion_gasolineras_gestcar"
    private const val INTERVALO_WIFI_HORAS = 6L
    private const val INTERVALO_RESPALDO_HORAS = 24L

    private val restriccionesConInternet = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private val restriccionesConWifi = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.UNMETERED)
        .build()

    fun programarActualizacionPeriodica(contexto: Context) {
        val trabajoWifi = PeriodicWorkRequestBuilder<GasolinerasWorker>(
            INTERVALO_WIFI_HORAS,
            TimeUnit.HOURS
        )
            .setConstraints(restriccionesConWifi)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val trabajoRespaldo = PeriodicWorkRequestBuilder<GasolinerasWorker>(
            INTERVALO_RESPALDO_HORAS,
            TimeUnit.HOURS
        )
            .setInputData(
                workDataOf(GasolinerasWorker.CLAVE_SOLO_SI_CACHE_CADUCADA to true)
            )
            .setConstraints(restriccionesConInternet)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniquePeriodicWork(
                TRABAJO_WIFI,
                ExistingPeriodicWorkPolicy.UPDATE,
                trabajoWifi
            )

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniquePeriodicWork(
                TRABAJO_RESPALDO,
                ExistingPeriodicWorkPolicy.UPDATE,
                trabajoRespaldo
            )
    }
}
