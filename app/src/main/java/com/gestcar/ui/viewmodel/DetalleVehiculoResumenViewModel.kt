package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.entidades.Repostaje
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class EstadoResumenDetalleVehiculo(
    val ultimosRepostajes: List<Repostaje> = emptyList(),
    val ultimosMantenimientos: List<Mantenimiento> = emptyList(),
    val gastosActivos: List<GastoPeriodico> = emptyList(),
    val recordatoriosPendientes: List<Recordatorio> = emptyList(),
    val consumoMedio: Double = 0.0,
    val costePorKilometro: Double = 0.0,
    val costeTotal: Double = 0.0
)

class DetalleVehiculoResumenViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
    private var trabajoCarga: Job? = null

    private val _estado = MutableStateFlow(EstadoResumenDetalleVehiculo())
    val estado: StateFlow<EstadoResumenDetalleVehiculo> = _estado.asStateFlow()

    fun cargarResumen(vehiculoId: String, kilometrajeActual: Double) {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            combine(
                baseDatos.repostajeDao().obtenerPorVehiculo(vehiculoId),
                baseDatos.mantenimientoDao().obtenerPorVehiculo(vehiculoId),
                baseDatos.gastoPeriodicoDao().obtenerPorVehiculo(vehiculoId),
                baseDatos.recordatorioDao().obtenerPorVehiculo(vehiculoId)
            ) { repostajes, mantenimientos, gastos, recordatorios ->
                EstadoResumenDetalleVehiculo(
                    ultimosRepostajes = repostajes.take(3),
                    ultimosMantenimientos = mantenimientos.filter { it.realizado }.take(3),
                    gastosActivos = gastos
                        .filter { !it.pagado }
                        .sortedWith(compareBy(nullsLast()) { it.fechaVencimiento })
                        .take(3),
                    recordatoriosPendientes = recordatorios
                        .filter { !it.completado }
                        .take(3),
                    consumoMedio = calcularConsumoMedio(repostajes),
                    costePorKilometro = calcularCostePorKilometro(
                        kilometrajeActual = kilometrajeActual,
                        repostajes = repostajes,
                        mantenimientos = mantenimientos,
                        gastos = gastos
                    ),
                    costeTotal = calcularCosteTotal(repostajes, mantenimientos, gastos)
                )
            }.collect { resumen ->
                _estado.value = resumen
            }
        }
    }

    private fun calcularConsumoMedio(repostajes: List<Repostaje>): Double {
        val llenos = repostajes
            .filter { it.llenoCompleto }
            .sortedBy { it.kilometros }

        if (llenos.size < 2) {
            return 0.0
        }

        val consumos = llenos.zipWithNext().mapNotNull { (anterior, actual) ->
            val distancia = actual.kilometros - anterior.kilometros
            if (distancia <= 0 || actual.litros <= 0) {
                null
            } else {
                (actual.litros / distancia) * 100
            }
        }

        return consumos.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun calcularCosteTotal(
        repostajes: List<Repostaje>,
        mantenimientos: List<Mantenimiento>,
        gastos: List<GastoPeriodico>
    ): Double {
        return repostajes.sumOf { it.importeTotal } +
            mantenimientos.filter { it.realizado }.sumOf { it.coste } +
            gastos.sumOf { it.importe }
    }

    private fun calcularCostePorKilometro(
        kilometrajeActual: Double,
        repostajes: List<Repostaje>,
        mantenimientos: List<Mantenimiento>,
        gastos: List<GastoPeriodico>
    ): Double {
        val kilometrajeInicial = (repostajes.map { it.kilometros } + mantenimientos.filter { it.realizado }.mapNotNull { it.kilometros })
            .filter { it > 0 }
            .minOrNull()
            ?: return 0.0

        val kilometrosRecorridos = kilometrajeActual - kilometrajeInicial
        if (kilometrosRecorridos <= 0) {
            return 0.0
        }

        return calcularCosteTotal(repostajes, mantenimientos, gastos) / kilometrosRecorridos
    }
}
