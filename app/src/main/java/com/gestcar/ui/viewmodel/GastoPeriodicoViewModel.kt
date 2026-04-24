package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.repositorio.GastoPeriodicoRepositorio
import com.gestcar.util.PlanificadorSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID

const val PERIODICIDAD_MENSUAL = "MENSUAL"
const val PERIODICIDAD_TRIMESTRAL = "TRIMESTRAL"
const val PERIODICIDAD_SEMESTRAL = "SEMESTRAL"
const val PERIODICIDAD_ANUAL = "ANUAL"
const val PERIODICIDAD_UNICO = "UNICO"

val conceptosGastoPredefinidos = listOf(
    "Seguro",
    "ITV",
    "Impuesto de circulación",
    "Parking",
    "Peajes",
    "Lavados",
    "Otro"
)

val periodicidadesGasto = listOf(
    PERIODICIDAD_UNICO,
    PERIODICIDAD_MENSUAL,
    PERIODICIDAD_TRIMESTRAL,
    PERIODICIDAD_SEMESTRAL,
    PERIODICIDAD_ANUAL
)

data class EstadoListaGastos(
    val gastos: List<GastoPeriodico> = emptyList(),
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

data class EstadoFormularioGasto(
    val gasto: GastoPeriodico = GastoPeriodico(periodicidad = PERIODICIDAD_UNICO),
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class GastoPeriodicoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio: GastoPeriodicoRepositorio

    init {
        val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
        repositorio = GastoPeriodicoRepositorio(
            gastoPeriodicoDao = baseDatos.gastoPeriodicoDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
    }

    private val _estadoLista = MutableStateFlow(EstadoListaGastos())
    val estadoLista: StateFlow<EstadoListaGastos> = _estadoLista.asStateFlow()

    private val _estadoFormulario = MutableStateFlow(EstadoFormularioGasto())
    val estadoFormulario: StateFlow<EstadoFormularioGasto> = _estadoFormulario.asStateFlow()

    fun cargarGastos(vehiculoId: String) {
        viewModelScope.launch {
            _estadoLista.value = _estadoLista.value.copy(estaCargando = true, mensajeError = null)
            val resultadoSincronizacion = repositorio.sincronizar(vehiculoId)

            repositorio.obtenerGastos(vehiculoId).collect { gastos ->
                _estadoLista.value = EstadoListaGastos(
                    gastos = gastos,
                    estaCargando = false,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    fun resetearFormulario(vehiculoId: String) {
        _estadoFormulario.value = EstadoFormularioGasto(
            gasto = GastoPeriodico(
                id = UUID.randomUUID().toString(),
                vehiculoId = vehiculoId,
                periodicidad = PERIODICIDAD_UNICO
            )
        )
    }

    fun cargarParaEditar(gastoId: String) {
        viewModelScope.launch {
            val gasto = repositorio.obtenerPorId(gastoId) ?: return@launch
            _estadoFormulario.value = EstadoFormularioGasto(gasto = gasto)
        }
    }

    fun actualizarFormulario(gasto: GastoPeriodico) {
        _estadoFormulario.value = _estadoFormulario.value.copy(
            gasto = gasto,
            mensajeError = null
        )
    }

    fun guardarGasto() {
        viewModelScope.launch {
            val estadoActual = _estadoFormulario.value
            val gasto = prepararGastoParaGuardar(estadoActual.gasto)

            if (gasto.vehiculoId.isBlank() || gasto.concepto.isBlank() || gasto.importe <= 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Revisa los campos obligatorios del gasto"
                )
                return@launch
            }

            _estadoFormulario.value = estadoActual.copy(estaCargando = true, mensajeError = null)

            val resultado = repositorio.guardar(gasto)
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
                    mensajeError = "No se ha podido guardar el gasto"
                )
            }
        }
    }

    private fun prepararGastoParaGuardar(gasto: GastoPeriodico): GastoPeriodico {
        return if (gasto.pagado) {
            gasto.copy(fechaPago = gasto.fechaPago ?: gasto.fecha)
        } else {
            gasto.copy(fechaPago = null)
        }
    }

    fun eliminarGasto(gasto: GastoPeriodico) {
        viewModelScope.launch {
            repositorio.eliminar(gasto)
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    fun marcarComoPagado(
        gasto: GastoPeriodico,
        crearSiguienteAviso: Boolean
    ) {
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val gastoPagado = gasto.copy(
                pagado = true,
                fechaPago = ahora,
                actualizadoEn = ahora
            )

            val resultadoPago = repositorio.guardar(gastoPagado)
            if (resultadoPago.isFailure) {
                _estadoLista.value = _estadoLista.value.copy(
                    mensajeError = "No se ha podido marcar el gasto como pagado"
                )
                return@launch
            }

            if (crearSiguienteAviso && esRecurrente(gasto.periodicidad)) {
                val siguiente = crearSiguienteGasto(gasto, ahora)
                val resultadoSiguiente = repositorio.guardar(siguiente)

                if (resultadoSiguiente.isFailure) {
                    _estadoLista.value = _estadoLista.value.copy(
                        mensajeError = "Se ha marcado como pagado, pero no se ha podido crear el siguiente aviso"
                    )
                }
            }

            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    private fun esRecurrente(periodicidad: String?): Boolean {
        return periodicidad != null && periodicidad != PERIODICIDAD_UNICO
    }

    private fun crearSiguienteGasto(
        gasto: GastoPeriodico,
        fechaPago: Long
    ): GastoPeriodico {
        val siguienteVencimiento = sumarPeriodo(fechaPago, gasto.periodicidad)

        return gasto.copy(
            id = UUID.randomUUID().toString(),
            fecha = fechaPago,
            fechaVencimiento = siguienteVencimiento,
            pagado = false,
            fechaPago = null,
            actualizadoEn = fechaPago
        )
    }

    private fun sumarPeriodo(
        fechaBase: Long,
        periodicidad: String?
    ): Long {
        val calendario = Calendar.getInstance().apply {
            timeInMillis = fechaBase
        }

        when (periodicidad) {
            PERIODICIDAD_MENSUAL -> calendario.add(Calendar.MONTH, 1)
            PERIODICIDAD_TRIMESTRAL -> calendario.add(Calendar.MONTH, 3)
            PERIODICIDAD_SEMESTRAL -> calendario.add(Calendar.MONTH, 6)
            PERIODICIDAD_ANUAL -> calendario.add(Calendar.YEAR, 1)
        }

        return calendario.timeInMillis
    }
}
