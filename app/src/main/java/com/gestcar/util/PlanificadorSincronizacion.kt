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

    // usamos nombres fijos para que workmanager no duplique trabajos
    // si se programa otra vez actualiza el existente en vez de crear varios workers
    private const val TRABAJO_PERIODICO = "sincronizacion_periodica_gestcar"
    private const val TRABAJO_PUNTUAL = "sincronizacion_puntual_gestcar"
    private const val INTERVALO_HORAS = 6L

    // no queremos despertar el worker sin internet
    // room ya mantiene los datos locales hasta que esta restriccion se cumpla
    private val restriccionesConInternet = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun programarSincronizacionPeriodica(contexto: Context) {
        // trabajo cada 6 horas
        // cubre el caso de usuario que crea datos offline y cierra la app
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
        // trabajo inmediato para cuando vuelve la conexion o se guarda un dato importante
        // replace evita que se acumulen muchas sincronizaciones iguales seguidas
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
