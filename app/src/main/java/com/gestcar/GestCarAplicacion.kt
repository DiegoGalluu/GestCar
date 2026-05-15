package com.gestcar

import android.app.Application
import com.gestcar.util.PlanificadorGasolineras
import com.gestcar.util.PlanificadorNotificaciones
import com.gestcar.util.PlanificadorSincronizacion

// clase de aplicacion principal, se usa para inicializar cosas globales
// por ahora no hace nada especial pero la necesitamos para que room
// y supabase tengan acceso al contexto de la app
class GestCarAplicacion : Application() {

    override fun onCreate() {
        super.onCreate()
        PlanificadorSincronizacion.programarSincronizacionPeriodica(this)
        PlanificadorGasolineras.programarActualizacionPeriodica(this)
        PlanificadorNotificaciones.programarRevisionVencimientos(this)
    }
}
