package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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
    val combustiblesDisponibles: List<String> = emptyList(),
    val combustibleSeleccionado: String? = null,
    val ordenGasolineras: OrdenGasolineras = OrdenGasolineras.DISTANCIA,
    val centroLatitud: Double = 40.4168,
    val centroLongitud: Double = -3.7038,
    val nombreCentro: String = "Madrid",
    val radioKm: Int = 10,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

enum class OrdenGasolineras {
    DISTANCIA,
    PRECIO
}

class GasolineraViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val repositorio = GasolineraRepositorio(aplicacion)

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
                    val filtro = gasolineras.filtrarGasolineras(
                        centroLatitud = estadoActual.centroLatitud,
                        centroLongitud = estadoActual.centroLongitud,
                        radioKm = estadoActual.radioKm,
                        combustible = estadoActual.combustibleSeleccionado,
                        orden = estadoActual.ordenGasolineras
                    )

                    estadoActual.copy(
                        gasolineras = gasolineras,
                        gasolinerasFiltradas = filtro.marcadores,
                        totalGasolinerasFiltradas = filtro.total,
                        combustiblesDisponibles = gasolineras.combustiblesDisponiblesOrdenados(),
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
            val filtro = estadoActual.gasolineras.filtrarGasolineras(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = radioKm,
                combustible = estadoActual.combustibleSeleccionado,
                orden = estadoActual.ordenGasolineras
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
            val filtro = estadoActual.gasolineras.filtrarGasolineras(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = estadoActual.radioKm,
                combustible = estadoActual.combustibleSeleccionado,
                orden = estadoActual.ordenGasolineras
            )

            estadoActual.copy(
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    fun usarUbicacion(latitud: Double, longitud: Double) {
        _estado.update { estadoActual ->
            val filtro = estadoActual.gasolineras.filtrarGasolineras(
                centroLatitud = latitud,
                centroLongitud = longitud,
                radioKm = estadoActual.radioKm,
                combustible = estadoActual.combustibleSeleccionado,
                orden = estadoActual.ordenGasolineras
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

    fun cambiarFiltroCombustible(combustible: String?) {
        _estado.update { estadoActual ->
            val nuevoOrden = if (combustible == null) OrdenGasolineras.DISTANCIA else estadoActual.ordenGasolineras
            val filtro = estadoActual.gasolineras.filtrarGasolineras(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = estadoActual.radioKm,
                combustible = combustible,
                orden = nuevoOrden
            )

            estadoActual.copy(
                combustibleSeleccionado = combustible,
                ordenGasolineras = nuevoOrden,
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    fun cambiarOrdenGasolineras(orden: OrdenGasolineras) {
        _estado.update { estadoActual ->
            val nuevoOrden = if (estadoActual.combustibleSeleccionado == null) {
                OrdenGasolineras.DISTANCIA
            } else {
                orden
            }
            val filtro = estadoActual.gasolineras.filtrarGasolineras(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = estadoActual.radioKm,
                combustible = estadoActual.combustibleSeleccionado,
                orden = nuevoOrden
            )

            estadoActual.copy(
                ordenGasolineras = nuevoOrden,
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    fun limpiarFiltrosAvanzados() {
        _estado.update { estadoActual ->
            val filtro = estadoActual.gasolineras.filtrarGasolineras(
                centroLatitud = estadoActual.centroLatitud,
                centroLongitud = estadoActual.centroLongitud,
                radioKm = estadoActual.radioKm,
                combustible = null,
                orden = OrdenGasolineras.DISTANCIA
            )

            estadoActual.copy(
                combustibleSeleccionado = null,
                ordenGasolineras = OrdenGasolineras.DISTANCIA,
                gasolinerasFiltradas = filtro.marcadores,
                totalGasolinerasFiltradas = filtro.total
            )
        }
    }

    private fun List<Gasolinera>.filtrarGasolineras(
        centroLatitud: Double,
        centroLongitud: Double,
        radioKm: Int,
        combustible: String?,
        orden: OrdenGasolineras
    ): ResultadoFiltroGasolineras {
        val gasolinerasConDistancia = map { gasolinera ->
            GasolineraConDistancia(
                gasolinera = gasolinera,
                distanciaKm = distanciaKm(
                    latitudOrigen = centroLatitud,
                    longitudOrigen = centroLongitud,
                    latitudDestino = gasolinera.latitud,
                    longitudDestino = gasolinera.longitud
                )
            )
        }
            .filter { gasolineraConDistancia -> gasolineraConDistancia.distanciaKm <= radioKm }
            .filter { gasolineraConDistancia ->
                combustible == null || gasolineraConDistancia.gasolinera.precioDeCombustible(combustible) != null
            }

        val gasolinerasOrdenadas = when {
            combustible != null && orden == OrdenGasolineras.PRECIO -> gasolinerasConDistancia
                .sortedWith(
                    compareBy<GasolineraConDistancia> {
                        it.gasolinera.precioDeCombustible(combustible) ?: Double.MAX_VALUE
                    }.thenBy { it.distanciaKm }
                )

            else -> gasolinerasConDistancia.sortedBy { gasolineraConDistancia -> gasolineraConDistancia.distanciaKm }
        }
            .map { gasolineraConDistancia -> gasolineraConDistancia.gasolinera }

        return ResultadoFiltroGasolineras(
            total = gasolinerasOrdenadas.size,
            marcadores = gasolinerasOrdenadas.take(MAXIMO_MARCADORES)
        )
    }

    private fun Gasolinera.precioDeCombustible(combustible: String): Double? =
        precios.firstOrNull { precio -> precio.nombre == combustible }?.precio

    private fun List<Gasolinera>.combustiblesDisponiblesOrdenados(): List<String> {
        val ordenCombustibles = listOf(
            "SP95",
            "SP98",
            "Diésel",
            "Diésel+",
            "GLP",
            "GNC",
            "GNL",
            "Biodiésel",
            "Bioetanol"
        )

        return flatMap { gasolinera -> gasolinera.precios.map { precio -> precio.nombre } }
            .distinct()
            .sortedWith(
                compareBy<String> { combustible ->
                    val indice = ordenCombustibles.indexOf(combustible)
                    if (indice == -1) Int.MAX_VALUE else indice
                }.thenBy { combustible -> combustible }
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

private data class GasolineraConDistancia(
    val gasolinera: Gasolinera,
    val distanciaKm: Double
)
