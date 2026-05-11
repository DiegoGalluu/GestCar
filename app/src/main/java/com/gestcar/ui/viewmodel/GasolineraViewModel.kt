package com.gestcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.remoto.Gasolinera
import com.gestcar.datos.repositorio.GasolineraRepositorio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class EstadoGasolineras(
    val gasolineras: List<Gasolinera> = emptyList(),
    val gasolinerasFiltradas: List<Gasolinera> = emptyList(),
    val totalGasolinerasFiltradas: Int = 0,
    val centroLatitud: Double = 40.4168,
    val centroLongitud: Double = -3.7038,
    val nombreCentro: String = "Madrid",
    val radioKm: Int = 10,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

class GasolineraViewModel : ViewModel() {

    private val repositorio = GasolineraRepositorio()

    private val _estado = MutableStateFlow(EstadoGasolineras())
    val estado: StateFlow<EstadoGasolineras> = _estado.asStateFlow()

    init {
        cargarGasolineras()
    }

    fun cargarGasolineras() {
        viewModelScope.launch {
            _estado.update { it.copy(estaCargando = true, mensajeError = null) }

            val resultado = repositorio.obtenerGasolineras()
            resultado.onSuccess { gasolineras ->
                _estado.update { estadoActual ->
                    val filtro = gasolineras.filtrarPorRadio(
                        centroLatitud = estadoActual.centroLatitud,
                        centroLongitud = estadoActual.centroLongitud,
                        radioKm = estadoActual.radioKm
                    )

                    estadoActual.copy(
                        gasolineras = gasolineras,
                        gasolinerasFiltradas = filtro.marcadores,
                        totalGasolinerasFiltradas = filtro.total,
                        estaCargando = false,
                        mensajeError = null
                    )
                }
            }.onFailure { error ->
                _estado.update {
                    it.copy(
                        estaCargando = false,
                        mensajeError = "No se han podido cargar las gasolineras: ${error.message}"
                    )
                }
            }
        }
    }

    fun cambiarRadio(radioKm: Int) {
        _estado.update { estadoActual ->
            val filtro = estadoActual.gasolineras.filtrarPorRadio(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = radioKm
            )

            estadoActual.copy(
                radioKm = radioKm,
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    fun aplicarFiltro() {
        _estado.update { estadoActual ->
            val filtro = estadoActual.gasolineras.filtrarPorRadio(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = estadoActual.radioKm
            )

            estadoActual.copy(
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    fun usarUbicacion(latitud: Double, longitud: Double) {
        _estado.update { estadoActual ->
            val filtro = estadoActual.gasolineras.filtrarPorRadio(
                centroLatitud = latitud,
                centroLongitud = longitud,
                radioKm = estadoActual.radioKm
            )

            estadoActual.copy(
                centroLatitud = latitud,
                centroLongitud = longitud,
                nombreCentro = "Tu ubicación",
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    private fun List<Gasolinera>.filtrarPorRadio(
        centroLatitud: Double,
        centroLongitud: Double,
        radioKm: Int
    ): ResultadoFiltroGasolineras {
        val gasolinerasOrdenadas = map { gasolinera ->
            gasolinera to distanciaKm(
                latitudOrigen = centroLatitud,
                longitudOrigen = centroLongitud,
                latitudDestino = gasolinera.latitud,
                longitudDestino = gasolinera.longitud
            )
        }
            .filter { (_, distancia) -> distancia <= radioKm }
            .sortedBy { (_, distancia) -> distancia }
            .map { (gasolinera, _) -> gasolinera }

        return ResultadoFiltroGasolineras(
            total = gasolinerasOrdenadas.size,
            marcadores = gasolinerasOrdenadas.take(MAXIMO_MARCADORES)
        )
    }

    private fun distanciaKm(
        latitudOrigen: Double,
        longitudOrigen: Double,
        latitudDestino: Double,
        longitudDestino: Double
    ): Double {
        val radioTierraKm = 6371.0
        val diferenciaLatitud = Math.toRadians(latitudDestino - latitudOrigen)
        val diferenciaLongitud = Math.toRadians(longitudDestino - longitudOrigen)
        val origenRad = Math.toRadians(latitudOrigen)
        val destinoRad = Math.toRadians(latitudDestino)

        val a = sin(diferenciaLatitud / 2).pow(2) +
            cos(origenRad) * cos(destinoRad) * sin(diferenciaLongitud / 2).pow(2)
        return 2 * radioTierraKm * asin(sqrt(a))
    }

    companion object {
        private const val MAXIMO_MARCADORES = 350
    }
}

private data class ResultadoFiltroGasolineras(
    val total: Int,
    val marcadores: List<Gasolinera>
)
