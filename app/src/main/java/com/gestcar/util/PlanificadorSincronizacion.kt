package com.gestcar.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PlanificadorSincronizacion {

    private const val TRABAJO_PERIODICO = "sincronizacion_periodica_gestcar"
    private const val TRABAJO_PUNTUAL = "sincronizacion_puntual_gestcar"
    private const val INTERVALO_HORAS = 6L

    private val restriccionesConInternet = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun programarSincronizacionPeriodica(contexto: Context) {
        val trabajo = PeriodicWorkRequestBuilder<SincronizacionWorker>(
            INTERVALO_HORAS,
            TimeUnit.HOURS
        )
            .setConstraints(restriccionesConInternet)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniquePeriodicWork(
                TRABAJO_PERIODICO,
                ExistingPeriodicWorkPolicy.UPDATE,
                trabajo
            )
    }

    fun encolarSincronizacionPuntual(contexto: Context) {
        val trabajo = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(restriccionesConInternet)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniqueWork(
                TRABAJO_PUNTUAL,
                ExistingWorkPolicy.REPLACE,
                trabajo
            )
    }
}
