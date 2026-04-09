package com.gestcar.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.datos.repositorio.VehiculoRepositorio
import com.gestcar.util.GestorImagenesVehiculo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// estado de la pantalla de lista de vehiculos
data class EstadoListaVehiculos(
    val vehiculos: List<Vehiculo> = emptyList(),
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

// estado del formulario de vehiculo, para crear o editar
data class EstadoFormularioVehiculo(
    val vehiculo: Vehiculo = Vehiculo(),
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

// viewmodel que gestiona los vehiculos, tanto la lista como el formulario
// usa androidviewmodel porque necesita el contexto para crear la base de datos
class VehiculoViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    // creamos el repositorio con el dao de la base de datos
    private val repositorio: VehiculoRepositorio

    init {
        val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
        repositorio = VehiculoRepositorio(baseDatos.vehiculoDao())
    }

    // estado de la lista de vehiculos
    private val _estadoLista = MutableStateFlow(EstadoListaVehiculos())
    val estadoLista: StateFlow<EstadoListaVehiculos> = _estadoLista.asStateFlow()

    // estado del formulario
    private val _estadoFormulario = MutableStateFlow(EstadoFormularioVehiculo())
    val estadoFormulario: StateFlow<EstadoFormularioVehiculo> = _estadoFormulario.asStateFlow()

    // estado del detalle de un vehiculo
    private val _vehiculoDetalle = MutableStateFlow<Vehiculo?>(null)
    val vehiculoDetalle: StateFlow<Vehiculo?> = _vehiculoDetalle.asStateFlow()

    // carga la lista de vehiculos del usuario y se suscribe a los cambios
    fun cargarVehiculos(usuarioId: String) {
        viewModelScope.launch {
            _estadoLista.value = _estadoLista.value.copy(estaCargando = true)

            // primero intentamos subir pendientes locales y luego traer lo remoto
            val resultadoSincronizacion = repositorio.sincronizar(usuarioId)

            // nos suscribimos al flow de room para recibir actualizaciones en tiempo real
            repositorio.obtenerVehiculos(usuarioId).collect { lista ->
                _estadoLista.value = EstadoListaVehiculos(
                    vehiculos = lista,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    // carga un vehiculo concreto para el formulario de edicion
    fun cargarParaEditar(vehiculoId: String) {
        viewModelScope.launch {
            val vehiculo = repositorio.obtenerPorId(vehiculoId)
            if (vehiculo != null) {
                _estadoFormulario.value = EstadoFormularioVehiculo(vehiculo = vehiculo)
            }
        }
    }

    // carga un vehiculo concreto para la pantalla de detalle
    fun cargarDetalle(vehiculoId: String) {
        viewModelScope.launch {
            _vehiculoDetalle.value = repositorio.obtenerPorId(vehiculoId)
        }
    }

    // actualiza el estado del formulario cuando el usuario cambia algun campo
    fun actualizarFormulario(vehiculo: Vehiculo) {
        _estadoFormulario.value = _estadoFormulario.value.copy(vehiculo = vehiculo)
    }

    // resetea el formulario para crear un vehiculo nuevo
    fun resetearFormulario(usuarioId: String) {
        _estadoFormulario.value = EstadoFormularioVehiculo(
            vehiculo = Vehiculo(
                id = UUID.randomUUID().toString(),
                usuarioId = usuarioId
            )
        )
    }

    // guarda el vehiculo del formulario, ya sea nuevo o editado
    fun guardarVehiculo() {
        viewModelScope.launch {
            val vehiculo = _estadoFormulario.value.vehiculo
            _estadoFormulario.value = _estadoFormulario.value.copy(estaCargando = true)

            // validamos los campos obligatorios
            if (vehiculo.marca.isBlank() || vehiculo.modelo.isBlank() || vehiculo.matricula.isBlank()) {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    estaCargando = false,
                    mensajeError = "Rellena todos los campos obligatorios"
                )
                return@launch
            }

            if (vehiculo.anioFabricacion <= 0) {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    estaCargando = false,
                    mensajeError = "El año de fabricación es obligatorio"
                )
                return@launch
            }

            if (vehiculo.diaFabricacion != null && vehiculo.mesFabricacion == null) {
                _estadoFormulario.value = _estadoFormulario.value.copy(
                    estaCargando = false,
                    mensajeError = "Si indicas un día, también debes indicar el mes"
                )
                return@launch
            }

            val resultado = repositorio.guardar(vehiculo)
            _estadoFormulario.value = if (resultado.isSuccess) {
                _estadoFormulario.value.copy(
                    estaCargando = false,
                    guardadoExitoso = true,
                    mensajeError = null
                )
            } else {
                _estadoFormulario.value.copy(
                    estaCargando = false,
                    guardadoExitoso = false,
                    mensajeError = "Se ha guardado en local, pero Supabase ha rechazado la sincronizacion. ${resultado.exceptionOrNull()?.message ?: ""}".trim()
                )
            }
        }
    }

    // elimina un vehiculo
    fun eliminarVehiculo(vehiculo: Vehiculo) {
        viewModelScope.launch {
            repositorio.eliminar(vehiculo)
        }
    }

    fun actualizarImagenVehiculo(origenUri: Uri) {
        val vehiculoActual = _vehiculoDetalle.value ?: return

        viewModelScope.launch {
            try {
                val imagenLocalUri = GestorImagenesVehiculo.guardarImagenComprimida(
                    context = getApplication(),
                    origenUri = origenUri,
                    usuarioId = vehiculoActual.usuarioId,
                    vehiculoId = vehiculoActual.id
                )

                val resultado = repositorio.actualizarImagenVehiculo(vehiculoActual, imagenLocalUri)
                if (resultado.isSuccess) {
                    _vehiculoDetalle.value = resultado.getOrNull()
                } else {
                    _estadoLista.value = _estadoLista.value.copy(
                        mensajeError = "No se ha podido actualizar la foto del vehiculo"
                    )
                }
            } catch (e: Exception) {
                _estadoLista.value = _estadoLista.value.copy(
                    mensajeError = "No se ha podido procesar la imagen seleccionada"
                )
            }
        }
    }
}
