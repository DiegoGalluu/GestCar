package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.repositorio.RecordatorioRepositorio
import com.gestcar.util.PlanificadorSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoListaRecordatorios(
    val recordatorios: List<Recordatorio> = emptyList(),
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

data class EstadoFormularioRecordatorio(
    val recordatorio: Recordatorio = Recordatorio(),
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class RecordatorioViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio: RecordatorioRepositorio

    init {
        val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
        repositorio = RecordatorioRepositorio(
            recordatorioDao = baseDatos.recordatorioDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
    }

    private val _estadoLista = MutableStateFlow(EstadoListaRecordatorios())
    val estadoLista: StateFlow<EstadoListaRecordatorios> = _estadoLista.asStateFlow()

    private val _estadoFormulario = MutableStateFlow(EstadoFormularioRecordatorio())
    val estadoFormulario: StateFlow<EstadoFormularioRecordatorio> = _estadoFormulario.asStateFlow()

    fun cargarRecordatorios(vehiculoId: String) {
        viewModelScope.launch {
            _estadoLista.value = _estadoLista.value.copy(estaCargando = true, mensajeError = null)
            val resultadoSincronizacion = repositorio.sincronizar(vehiculoId)

            repositorio.obtenerRecordatorios(vehiculoId).collect { recordatorios ->
                _estadoLista.value = EstadoListaRecordatorios(
                    recordatorios = recordatorios,
                    estaCargando = false,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    fun resetearFormulario(vehiculoId: String) {
        _estadoFormulario.value = EstadoFormularioRecordatorio(
            recordatorio = Recordatorio(
                id = UUID.randomUUID().toString(),
                vehiculoId = vehiculoId
            )
        )
    }

    fun cargarParaEditar(recordatorioId: String) {
        viewModelScope.launch {
            val recordatorio = repositorio.obtenerPorId(recordatorioId) ?: return@launch
            _estadoFormulario.value = EstadoFormularioRecordatorio(recordatorio = recordatorio)
        }
    }

    fun actualizarFormulario(recordatorio: Recordatorio) {
        _estadoFormulario.value = _estadoFormulario.value.copy(
            recordatorio = recordatorio,
            mensajeError = null
        )
    }

    fun guardarRecordatorio() {
        viewModelScope.launch {
            val estadoActual = _estadoFormulario.value
            val recordatorio = estadoActual.recordatorio

            if (recordatorio.vehiculoId.isBlank() || recordatorio.concepto.isBlank()) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Revisa los campos obligatorios del recordatorio"
                )
                return@launch
            }

            if (recordatorio.fechaLimite == null && recordatorio.kilometrajeLimite == null) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Indica una fecha limite o un kilometraje limite"
                )
                return@launch
            }

            if (recordatorio.kilometrajeLimite != null && recordatorio.kilometrajeLimite <= 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "El kilometraje limite debe ser mayor que cero"
                )
                return@launch
            }

            _estadoFormulario.value = estadoActual.copy(estaCargando = true, mensajeError = null)

            val resultado = repositorio.guardar(recordatorio)
            _estadoFormulario.value = if (resultado.isSuccess) {
                PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
                _estadoFormulario.value.copy(
                    estaCargando = false,
                    guardadoExitoso = true,
                    mensajeError = null
                )
            } else {
                _estadoFormulario.value.copy(
                    estaCargando = false,
                    guardadoExitoso = false,
                    mensajeError = "No se ha podido guardar el recordatorio"
                )
            }
        }
    }

    fun marcarComoCompletado(recordatorio: Recordatorio) {
        viewModelScope.launch {
            repositorio.guardar(
                recordatorio.copy(
                    completado = true,
                    fechaCompletado = System.currentTimeMillis()
                )
            )
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    fun reabrirRecordatorio(recordatorio: Recordatorio) {
        viewModelScope.launch {
            repositorio.guardar(
                recordatorio.copy(
                    completado = false,
                    fechaCompletado = null
                )
            )
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    fun eliminarRecordatorio(recordatorio: Recordatorio) {
        viewModelScope.launch {
            repositorio.eliminar(recordatorio)
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }
}
