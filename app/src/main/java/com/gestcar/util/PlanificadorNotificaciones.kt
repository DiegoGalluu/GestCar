package com.gestcar.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object PlanificadorNotificaciones {

    private const val TRABAJO_VENCIMIENTOS = "notificaciones_vencimientos_gestcar"
    private const val TRABAJO_VENCIMIENTOS_PUNTUAL = "notificaciones_vencimientos_puntual_gestcar"
    private const val INTERVALO_HORAS = 12L

    fun programarRevisionVencimientos(contexto: Context) {
        // workmanager decide la hora exacta para respetar bateria y restricciones del sistema
        // nosotros solo garantizamos que se revise de forma periodica aunque la app este cerrada
        val trabajo = PeriodicWorkRequestBuilder<VencimientosWorker>(
            INTERVALO_HORAS,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniquePeriodicWork(
                TRABAJO_VENCIMIENTOS,
                ExistingPeriodicWorkPolicy.UPDATE,
                trabajo
                )
    }

    fun encolarRevisionPuntual(contexto: Context) {
        // esta revision inmediata cubre el arranque de la app y el momento de conceder permisos
        // el trabajo periodico sigue siendo el respaldo cuando la app esta cerrada
        val trabajo = OneTimeWorkRequestBuilder<VencimientosWorker>().build()

        WorkManager.getInstance(contexto.applicationContext)
            .enqueueUniqueWork(
                TRABAJO_VENCIMIENTOS_PUNTUAL,
                ExistingWorkPolicy.REPLACE,
                trabajo
            )
    }
}
