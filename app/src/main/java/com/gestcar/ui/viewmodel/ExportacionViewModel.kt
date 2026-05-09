package com.gestcar.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.util.DatosExportacionVehiculo
import com.gestcar.util.ExportadorDatosVehiculo
import com.gestcar.util.FormatoExportacionVehiculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoExportacion(
    val estaExportando: Boolean = false,
    val mensajeError: String? = null
)

class ExportacionViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)

    private val _estado = MutableStateFlow(EstadoExportacion())
    val estado: StateFlow<EstadoExportacion> = _estado.asStateFlow()

    fun exportarVehiculo(
        contexto: Context,
        vehiculo: Vehiculo?,
        formato: FormatoExportacionVehiculo,
        alCrearIntent: (Intent) -> Unit
    ) {
        if (vehiculo == null) {
            _estado.value = EstadoExportacion(mensajeError = "Selecciona un vehículo antes de exportar")
            return
        }

        viewModelScope.launch {
            _estado.value = EstadoExportacion(estaExportando = true)

            runCatching {
                // leemos todo desde room para que la exportacion funcione tambien sin internet
                val datos = DatosExportacionVehiculo(
                    vehiculo = vehiculo,
                    repostajes = baseDatos.repostajeDao().obtenerPorVehiculoLista(vehiculo.id),
                    mantenimientos = baseDatos.mantenimientoDao().obtenerPorVehiculoLista(vehiculo.id),
                    gastos = baseDatos.gastoPeriodicoDao().obtenerPorVehiculoLista(vehiculo.id)
                )
                ExportadorDatosVehiculo.crearIntentExportacion(contexto, datos, formato)
            }.onSuccess { intent ->
                _estado.value = EstadoExportacion()
                alCrearIntent(Intent.createChooser(intent, "Compartir datos de GestCar"))
            }.onFailure {
                _estado.value = EstadoExportacion(
                    estaExportando = false,
                    mensajeError = "No se han podido preparar los datos"
                )
            }
        }
    }
}
