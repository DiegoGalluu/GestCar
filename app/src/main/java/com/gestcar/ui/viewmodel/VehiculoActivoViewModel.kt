package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.datos.repositorio.GastoPeriodicoRepositorio
import com.gestcar.datos.repositorio.MantenimientoRepositorio
import com.gestcar.datos.repositorio.RecordatorioRepositorio
import com.gestcar.datos.repositorio.RepostajeRepositorio
import com.gestcar.datos.repositorio.VehiculoRepositorio
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoVehiculoActivo(
    val vehiculos: List<Vehiculo> = emptyList(),
    val vehiculoActivo: Vehiculo? = null,
    val vehiculoPreferidoId: String? = null,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

// mantiene el vehiculo seleccionado para las secciones de la bottom bar
// asi evitamos pasar el mismo id por todas las pantallas nuevas
class VehiculoActivoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio: VehiculoRepositorio
    private val repostajeRepositorio: RepostajeRepositorio
    private val mantenimientoRepositorio: MantenimientoRepositorio
    private val gastoRepositorio: GastoPeriodicoRepositorio
    private val recordatorioRepositorio: RecordatorioRepositorio
    private var trabajoCarga: Job? = null

    init {
        val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
        repositorio = VehiculoRepositorio(baseDatos.vehiculoDao())
        repostajeRepositorio = RepostajeRepositorio(
            repostajeDao = baseDatos.repostajeDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
        mantenimientoRepositorio = MantenimientoRepositorio(
            mantenimientoDao = baseDatos.mantenimientoDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
        gastoRepositorio = GastoPeriodicoRepositorio(
            gastoPeriodicoDao = baseDatos.gastoPeriodicoDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
        recordatorioRepositorio = RecordatorioRepositorio(
            recordatorioDao = baseDatos.recordatorioDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
    }

    private val _estado = MutableStateFlow(EstadoVehiculoActivo())
    val estado: StateFlow<EstadoVehiculoActivo> = _estado.asStateFlow()

    fun cargarVehiculos(usuarioId: String, vehiculoPreferidoId: String? = null) {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            // vehiculoPreferidoId se usa al venir desde el detalle de un vehiculo
            // asi "ver todos" abre repostajes gastos o mantenimiento ya filtrado por ese coche
            _estado.value = _estado.value.copy(
                vehiculoPreferidoId = vehiculoPreferidoId ?: _estado.value.vehiculoPreferidoId,
                estaCargando = true,
                mensajeError = null
            )

            val resultadoSincronizacion = repositorio.sincronizar(usuarioId)

            // las operaciones pendientes se suben en segundo plano
            // asi la lista de vehiculos no se queda esperando si no hay conexion
            launch {
                repostajeRepositorio.sincronizarPendientesDelUsuario(usuarioId)
                mantenimientoRepositorio.sincronizarPendientesDelUsuario(usuarioId)
                gastoRepositorio.sincronizarPendientesDelUsuario(usuarioId)
                recordatorioRepositorio.sincronizarPendientesDelUsuario(usuarioId)
            }

            repositorio.obtenerVehiculos(usuarioId).collect { vehiculos ->
                val vehiculoActual = _estado.value.vehiculoActivo
                val preferidoId = _estado.value.vehiculoPreferidoId
                // prioridad de seleccion
                // primero el vehiculo pedido por navegacion, despues el actual, y si no existe el primero de la lista
                val vehiculoActivo = when {
                    vehiculos.isEmpty() -> null
                    preferidoId != null && vehiculos.any { it.id == preferidoId } -> vehiculos.first { it.id == preferidoId }
                    vehiculoActual == null -> vehiculos.first()
                    vehiculos.none { it.id == vehiculoActual.id } -> vehiculos.first()
                    else -> vehiculos.first { it.id == vehiculoActual.id }
                }

                _estado.value = EstadoVehiculoActivo(
                    vehiculos = vehiculos,
                    vehiculoActivo = vehiculoActivo,
                    vehiculoPreferidoId = preferidoId,
                    estaCargando = false,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    fun seleccionarVehiculo(vehiculo: Vehiculo) {
        _estado.value = _estado.value.copy(
            vehiculoActivo = vehiculo,
            vehiculoPreferidoId = vehiculo.id
        )
    }
}
