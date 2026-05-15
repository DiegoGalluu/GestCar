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
import java.util.Calendar
import java.util.UUID

data class EstadoListaRecordatorios(
    val recordatorios: List<Recordatorio> = emptyList(),
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

data class EstadoFormularioRecordatorio(
    val recordatorio: Recordatorio = Recordatorio(),
    val kilometrajeVehiculoActual: Double = 0.0,
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class RecordatorioViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio: RecordatorioRepositorio
    private val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)

    init {
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
        viewModelScope.launch {
            val vehiculo = baseDatos.vehiculoDao().obtenerPorId(vehiculoId)
            _estadoFormulario.value = EstadoFormularioRecordatorio(
                recordatorio = Recordatorio(
                    id = UUID.randomUUID().toString(),
                    vehiculoId = vehiculoId
                ),
                kilometrajeVehiculoActual = vehiculo?.kilometraje ?: 0.0
            )
        }
    }

    fun cargarParaEditar(recordatorioId: String) {
        viewModelScope.launch {
            val recordatorio = repositorio.obtenerPorId(recordatorioId) ?: return@launch
            val vehiculo = baseDatos.vehiculoDao().obtenerPorId(recordatorio.vehiculoId)
            _estadoFormulario.value = EstadoFormularioRecordatorio(
                recordatorio = recordatorio,
                kilometrajeVehiculoActual = vehiculo?.kilometraje ?: 0.0
            )
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
            val recordatorio = prepararRecordatorioParaGuardar(
                recordatorio = estadoActual.recordatorio,
                kilometrajeActual = estadoActual.kilometrajeVehiculoActual
            )

            if (recordatorio.vehiculoId.isBlank() || recordatorio.concepto.isBlank()) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Revisa los campos obligatorios del recordatorio"
                )
                return@launch
            }

            if (recordatorio.kilometrajeLimite != null && recordatorio.kilometrajeLimite <= 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "El kilometraje límite debe ser mayor que cero"
                )
                return@launch
            }

            if (recordatorio.periodicidadTiempoCantidad != null && recordatorio.periodicidadTiempoCantidad <= 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "La periodicidad por tiempo debe ser mayor que cero"
                )
                return@launch
            }

            if (recordatorio.periodicidadKilometros != null && recordatorio.periodicidadKilometros <= 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "La periodicidad por kilómetros debe ser mayor que cero"
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

    private fun prepararRecordatorioParaGuardar(
        recordatorio: Recordatorio,
        kilometrajeActual: Double
    ): Recordatorio {
        val intervaloKm = recordatorio.periodicidadKilometros?.takeIf { it > 0 }
        val kilometrajeLimite = if (intervaloKm != null) {
            kilometrajeActual + intervaloKm
        } else {
            recordatorio.kilometrajeLimite
        }

        return recordatorio.copy(kilometrajeLimite = kilometrajeLimite)
    }

    fun marcarComoCompletado(recordatorio: Recordatorio) {
        viewModelScope.launch {
            val fechaActual = System.currentTimeMillis()
            val recordatorioCompletado = recordatorio.copy(
                completado = true,
                fechaCompletado = fechaActual
            )

            val resultado = repositorio.guardar(recordatorioCompletado)
            if (resultado.isSuccess) {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    recordatorio = recordatorioCompletado,
                    mensajeError = null
                )
            } else {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    mensajeError = "No se ha podido sincronizar el estado con Supabase"
                )
                return@launch
            }
            crearSiguienteRecordatorioSiProcede(recordatorioCompletado)?.let { siguiente ->
                repositorio.guardar(siguiente)
            }
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    fun reabrirRecordatorio(recordatorio: Recordatorio) {
        viewModelScope.launch {
            val recordatorioReabierto = recordatorio.copy(
                completado = false,
                fechaCompletado = null
            )
            val resultado = repositorio.guardar(recordatorioReabierto)
            if (resultado.isSuccess) {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    recordatorio = recordatorioReabierto,
                    mensajeError = null
                )
            } else {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    mensajeError = "No se ha podido sincronizar el estado con Supabase"
                )
                return@launch
            }
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    fun eliminarRecordatorio(recordatorio: Recordatorio) {
        viewModelScope.launch {
            repositorio.eliminar(recordatorio)
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    private fun crearSiguienteRecordatorioSiProcede(recordatorio: Recordatorio): Recordatorio? {
        val siguienteFecha = calcularSiguienteFecha(recordatorio)
        val siguienteKilometraje = recordatorio.periodicidadKilometros
            ?.takeIf { it > 0 }
            ?.let { intervalo -> recordatorio.kilometrajeLimite?.plus(intervalo) }

        if (siguienteFecha == null && siguienteKilometraje == null) {
            return null
        }

        // dejamos el aviso completado como historico y creamos uno nuevo pendiente
        // asi se conserva cuando se hizo cada operacion y cual es la proxima
        return recordatorio.copy(
            id = UUID.randomUUID().toString(),
            fechaLimite = siguienteFecha,
            kilometrajeLimite = siguienteKilometraje,
            completado = false,
            fechaCompletado = null,
            actualizadoEn = System.currentTimeMillis()
        )
    }

    private fun calcularSiguienteFecha(recordatorio: Recordatorio): Long? {
        val cantidad = recordatorio.periodicidadTiempoCantidad?.takeIf { it > 0 } ?: return null
        val unidad = recordatorio.periodicidadTiempoUnidad ?: return null
        val fechaBase = recordatorio.fechaLimite ?: return null

        val campoCalendario = when (unidad) {
            UNIDAD_TIEMPO_DIAS -> Calendar.DAY_OF_YEAR
            UNIDAD_TIEMPO_SEMANAS -> Calendar.WEEK_OF_YEAR
            UNIDAD_TIEMPO_MESES -> Calendar.MONTH
            UNIDAD_TIEMPO_ANIOS -> Calendar.YEAR
            else -> return null
        }

        return Calendar.getInstance().apply {
            timeInMillis = fechaBase
            add(campoCalendario, cantidad)
        }.timeInMillis
    }
}

const val UNIDAD_TIEMPO_DIAS = "DIAS"
const val UNIDAD_TIEMPO_SEMANAS = "SEMANAS"
const val UNIDAD_TIEMPO_MESES = "MESES"
const val UNIDAD_TIEMPO_ANIOS = "ANIOS"
