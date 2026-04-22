package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.Repostaje
import com.gestcar.datos.repositorio.RepostajeRepositorio
import com.gestcar.util.PlanificadorSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoListaRepostajes(
    val repostajes: List<Repostaje> = emptyList(),
    val consumoMedio: Double = 0.0,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

data class EstadoFormularioRepostaje(
    val repostaje: Repostaje = Repostaje(),
    val ultimoKilometraje: Double? = null,
    val necesitaConfirmarKilometrajeMenor: Boolean = false,
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class RepostajeViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio: RepostajeRepositorio

    init {
        val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
        repositorio = RepostajeRepositorio(
            repostajeDao = baseDatos.repostajeDao(),
            vehiculoDao = baseDatos.vehiculoDao()
        )
    }

    private val _estadoLista = MutableStateFlow(EstadoListaRepostajes())
    val estadoLista: StateFlow<EstadoListaRepostajes> = _estadoLista.asStateFlow()

    private val _estadoFormulario = MutableStateFlow(EstadoFormularioRepostaje())
    val estadoFormulario: StateFlow<EstadoFormularioRepostaje> = _estadoFormulario.asStateFlow()

    fun cargarRepostajes(vehiculoId: String) {
        viewModelScope.launch {
            _estadoLista.value = _estadoLista.value.copy(estaCargando = true, mensajeError = null)
            val resultadoSincronizacion = repositorio.sincronizar(vehiculoId)

            repositorio.obtenerRepostajes(vehiculoId).collect { repostajes ->
                _estadoLista.value = EstadoListaRepostajes(
                    repostajes = repostajes,
                    consumoMedio = calcularConsumoMedioDesdeLista(repostajes),
                    estaCargando = false,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    fun resetearFormulario(vehiculoId: String) {
        viewModelScope.launch {
            _estadoFormulario.value = EstadoFormularioRepostaje(
                repostaje = Repostaje(
                    id = UUID.randomUUID().toString(),
                    vehiculoId = vehiculoId
                ),
                ultimoKilometraje = repositorio.obtenerUltimoKilometraje(vehiculoId)
            )
        }
    }

    fun cargarParaEditar(repostajeId: String) {
        viewModelScope.launch {
            val repostaje = repositorio.obtenerPorId(repostajeId) ?: return@launch
            _estadoFormulario.value = EstadoFormularioRepostaje(
                repostaje = repostaje,
                ultimoKilometraje = repositorio.obtenerUltimoKilometraje(repostaje.vehiculoId)
            )
        }
    }

    fun actualizarFormulario(repostaje: Repostaje) {
        _estadoFormulario.value = _estadoFormulario.value.copy(
            repostaje = repostaje.copy(importeTotal = repostaje.litros * repostaje.precioPorLitro),
            mensajeError = null
        )
    }

    fun guardarRepostaje(permitirKilometrajeMenor: Boolean = false) {
        viewModelScope.launch {
            val estadoActual = _estadoFormulario.value
            val repostaje = estadoActual.repostaje
            val ultimoKilometraje = estadoActual.ultimoKilometraje

            if (repostaje.kilometros < 0 || repostaje.litros <= 0 || repostaje.precioPorLitro <= 0) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Revisa los campos obligatorios del repostaje"
                )
                return@launch
            }

            if (!permitirKilometrajeMenor && ultimoKilometraje != null && repostaje.kilometros < ultimoKilometraje) {
                _estadoFormulario.value = estadoActual.copy(necesitaConfirmarKilometrajeMenor = true)
                return@launch
            }

            _estadoFormulario.value = estadoActual.copy(
                estaCargando = true,
                necesitaConfirmarKilometrajeMenor = false,
                mensajeError = null
            )

            val resultado = repositorio.guardar(repostaje)
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
                    mensajeError = "Se ha guardado en local, pero no se ha podido sincronizar con Supabase"
                )
            }
        }
    }

    fun cancelarConfirmacionKilometrajeMenor() {
        _estadoFormulario.value = _estadoFormulario.value.copy(
            necesitaConfirmarKilometrajeMenor = false
        )
    }

    fun eliminarRepostaje(repostaje: Repostaje) {
        viewModelScope.launch {
            repositorio.eliminar(repostaje)
        }
    }

    private fun calcularConsumoMedioDesdeLista(repostajes: List<Repostaje>): Double {
        val repostajesLlenos = repostajes
            .filter { it.llenoCompleto }
            .sortedBy { it.kilometros }

        val consumos = repostajesLlenos.zipWithNext().mapNotNull { (anterior, actual) ->
            val kilometrosRecorridos = actual.kilometros - anterior.kilometros
            if (kilometrosRecorridos <= 0 || actual.litros <= 0) {
                null
            } else {
                (actual.litros / kilometrosRecorridos) * 100
            }
        }

        return consumos.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }
}
