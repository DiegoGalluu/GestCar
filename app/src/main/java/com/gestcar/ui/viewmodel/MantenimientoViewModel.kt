package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.repositorio.MantenimientoRepositorio
import com.gestcar.util.PlanificadorSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

const val CATEGORIA_MANTENIMIENTO = "MANTENIMIENTO"
const val CATEGORIA_REPARACION = "REPARACION"
const val FILTRO_TODOS_MANTENIMIENTOS = "TODOS"

val tiposMantenimientoPredefinidos = listOf(
    "Aceite",
    "Neumáticos",
    "Frenos",
    "Filtros",
    "Motor",
    "Caja de cambios",
    "Embrague",
    "Correa de distribución",
    "Batería",
    "Amortiguadores",
    "Suspensión",
    "Sistema eléctrico",
    "Climatización",
    "Escape",
    "Dirección",
    "Revisión general"
)

data class EstadoListaMantenimientos(
    val mantenimientos: List<Mantenimiento> = emptyList(),
    val filtroCategoria: String = FILTRO_TODOS_MANTENIMIENTOS,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
) {
    private val mantenimientosPorCategoria: List<Mantenimiento>
        get() = when (filtroCategoria) {
            CATEGORIA_MANTENIMIENTO -> mantenimientos.filter { it.categoria == CATEGORIA_MANTENIMIENTO }
            CATEGORIA_REPARACION -> mantenimientos.filter { it.categoria == CATEGORIA_REPARACION }
            else -> mantenimientos
        }

    val mantenimientosFiltrados: List<Mantenimiento>
        get() = mantenimientosPorCategoria

    val mantenimientosPendientes: List<Mantenimiento>
        get() = mantenimientosPorCategoria.filter { !it.realizado }

    val mantenimientosRealizados: List<Mantenimiento>
        get() = mantenimientosPorCategoria.filter { it.realizado }
}

data class EstadoFormularioMantenimiento(
    val mantenimiento: Mantenimiento = Mantenimiento(),
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class MantenimientoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio: MantenimientoRepositorio

    init {
        val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
        repositorio = MantenimientoRepositorio(
            mantenimientoDao = baseDatos.mantenimientoDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
    }

    private val _estadoLista = MutableStateFlow(EstadoListaMantenimientos())
    val estadoLista: StateFlow<EstadoListaMantenimientos> = _estadoLista.asStateFlow()

    private val _estadoFormulario = MutableStateFlow(EstadoFormularioMantenimiento())
    val estadoFormulario: StateFlow<EstadoFormularioMantenimiento> = _estadoFormulario.asStateFlow()

    fun cargarMantenimientos(vehiculoId: String) {
        viewModelScope.launch {
            _estadoLista.value = _estadoLista.value.copy(estaCargando = true, mensajeError = null)
            val resultadoSincronizacion = repositorio.sincronizar(vehiculoId)

            repositorio.obtenerMantenimientos(vehiculoId).collect { mantenimientos ->
                _estadoLista.value = _estadoLista.value.copy(
                    mantenimientos = mantenimientos,
                    estaCargando = false,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    fun refrescarMantenimientosLocales(vehiculoId: String) {
        viewModelScope.launch {
            val mantenimientos = repositorio.obtenerMantenimientosLocales(vehiculoId)
            _estadoLista.value = _estadoLista.value.copy(
                mantenimientos = mantenimientos,
                estaCargando = false
            )
        }
    }

    fun cambiarFiltroCategoria(filtro: String) {
        _estadoLista.value = _estadoLista.value.copy(filtroCategoria = filtro)
    }

    fun resetearFormulario(vehiculoId: String) {
        _estadoFormulario.value = EstadoFormularioMantenimiento(
            mantenimiento = Mantenimiento(
                id = UUID.randomUUID().toString(),
                vehiculoId = vehiculoId,
                realizado = false,
                fechaRealizado = null
            )
        )
    }

    fun cargarParaEditar(mantenimientoId: String) {
        viewModelScope.launch {
            val mantenimiento = repositorio.obtenerPorId(mantenimientoId) ?: return@launch
            _estadoFormulario.value = EstadoFormularioMantenimiento(mantenimiento = mantenimiento)
        }
    }

    fun actualizarFormulario(mantenimiento: Mantenimiento) {
        _estadoFormulario.value = _estadoFormulario.value.copy(
            mantenimiento = mantenimiento,
            mensajeError = null
        )
    }

    fun guardarMantenimiento() {
        viewModelScope.launch {
            val estadoActual = _estadoFormulario.value
            val mantenimiento = estadoActual.mantenimiento

            if (mantenimiento.vehiculoId.isBlank() || mantenimiento.tipo.isBlank() || mantenimiento.coste < 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Revisa los campos obligatorios del mantenimiento"
                )
                return@launch
            }

            _estadoFormulario.value = estadoActual.copy(estaCargando = true, mensajeError = null)

            val resultado = repositorio.guardar(mantenimiento)
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
                    mensajeError = "No se ha podido guardar el mantenimiento"
                )
            }
        }
    }

    fun eliminarMantenimiento(mantenimiento: Mantenimiento) {
        viewModelScope.launch {
            repositorio.eliminar(mantenimiento)
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }

    fun cambiarEstadoRealizado(mantenimiento: Mantenimiento) {
        viewModelScope.launch {
            val ahora = System.currentTimeMillis()
            val mantenimientoActualizado = if (mantenimiento.realizado) {
                mantenimiento.copy(
                    realizado = false,
                    fechaRealizado = null
                )
            } else {
                mantenimiento.copy(
                    realizado = true,
                    fechaRealizado = ahora
                )
            }

            repositorio.guardar(mantenimientoActualizado)
            _estadoFormulario.value = _estadoFormulario.value.copy(mantenimiento = mantenimientoActualizado)
            PlanificadorSincronizacion.encolarSincronizacionPuntual(getApplication())
        }
    }
}
