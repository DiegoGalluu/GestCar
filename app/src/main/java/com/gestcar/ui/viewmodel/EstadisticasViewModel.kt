package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.entidades.Repostaje
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class GastoMensualEstadistica(
    val mes: String,
    val total: Double
)

data class CategoriaEstadistica(
    val nombre: String,
    val importe: Double
)

data class EstadoEstadisticas(
    val consumoMedio: Double = 0.0,
    val costeMedioCada100Km: Double = 0.0,
    val costePorKilometro: Double = 0.0,
    val costeTotal: Double = 0.0,
    val totalOperaciones: Int = 0,
    val categorias: List<CategoriaEstadistica> = emptyList(),
    val evolucionMensual: List<GastoMensualEstadistica> = emptyList(),
    val periodoSeleccionado: PeriodoRepostajes = PeriodoRepostajes.TODO,
    val fechaInicioPersonalizada: Long? = null,
    val fechaFinPersonalizada: Long? = null,
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

class EstadisticasViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
    private var trabajoCarga: Job? = null
    private var repostajesActuales: List<Repostaje> = emptyList()
    private var mantenimientosActuales: List<Mantenimiento> = emptyList()
    private var gastosActuales: List<GastoPeriodico> = emptyList()
    private var kilometrajeActualCache: Double = 0.0

    private val _estado = MutableStateFlow(EstadoEstadisticas())
    val estado: StateFlow<EstadoEstadisticas> = _estado.asStateFlow()

    fun cargarEstadisticas(vehiculoId: String, kilometrajeActual: Double) {
        trabajoCarga?.cancel()
        trabajoCarga = viewModelScope.launch {
            _estado.value = _estado.value.copy(estaCargando = true, mensajeError = null)

            combine(
                baseDatos.repostajeDao().obtenerPorVehiculo(vehiculoId),
                baseDatos.mantenimientoDao().obtenerPorVehiculo(vehiculoId),
                baseDatos.gastoPeriodicoDao().obtenerPorVehiculo(vehiculoId)
            ) { repostajes, mantenimientos, gastos ->
                repostajesActuales = repostajes
                mantenimientosActuales = mantenimientos
                gastosActuales = gastos
                kilometrajeActualCache = kilometrajeActual
                construirEstado(
                    repostajes = repostajes,
                    mantenimientos = mantenimientos,
                    gastos = gastos,
                    kilometrajeActual = kilometrajeActual,
                    estadoActual = _estado.value
                )
            }.collect { nuevoEstado ->
                _estado.value = nuevoEstado
            }
        }
    }

    fun seleccionarPeriodo(periodo: PeriodoRepostajes) {
        val estadoActual = _estado.value.copy(periodoSeleccionado = periodo)
        _estado.value = construirEstado(
            repostajes = repostajesActuales,
            mantenimientos = mantenimientosActuales,
            gastos = gastosActuales,
            kilometrajeActual = kilometrajeActualCache,
            estadoActual = estadoActual
        )
    }

    fun seleccionarRangoPersonalizado(fechaInicio: Long, fechaFin: Long) {
        val estadoActual = _estado.value.copy(
            periodoSeleccionado = PeriodoRepostajes.PERSONALIZADO,
            fechaInicioPersonalizada = fechaInicio,
            fechaFinPersonalizada = fechaFin
        )
        _estado.value = construirEstado(
            repostajes = repostajesActuales,
            mantenimientos = mantenimientosActuales,
            gastos = gastosActuales,
            kilometrajeActual = kilometrajeActualCache,
            estadoActual = estadoActual
        )
    }

    private fun construirEstado(
        repostajes: List<Repostaje>,
        mantenimientos: List<Mantenimiento>,
        gastos: List<GastoPeriodico>,
        kilometrajeActual: Double,
        estadoActual: EstadoEstadisticas
    ): EstadoEstadisticas {
        val repostajesFiltrados = repostajes.filter {
            fechaDentroDePeriodo(it.fecha, estadoActual)
        }
        val mantenimientosFiltrados = mantenimientos.filter {
            it.realizado && fechaDentroDePeriodo(it.fechaRealizado ?: it.fecha, estadoActual)
        }
        val gastosFiltrados = gastos.filter {
            it.pagado && fechaDentroDePeriodo(it.fechaPago ?: it.fecha, estadoActual)
        }

        val combustible = repostajesFiltrados.sumOf { it.importeTotal }
        val mantenimiento = mantenimientosFiltrados
            .filter { it.categoria == CATEGORIA_MANTENIMIENTO }
            .sumOf { it.coste }
        val reparaciones = mantenimientosFiltrados
            .filter { it.categoria == CATEGORIA_REPARACION }
            .sumOf { it.coste }
        val gastosPeriodicos = gastosFiltrados.sumOf { it.importe }
        val costeTotal = combustible + mantenimiento + reparaciones + gastosPeriodicos

        return estadoActual.copy(
            consumoMedio = calcularConsumoMedio(repostajesFiltrados),
            costeMedioCada100Km = calcularCosteMedioCada100Km(repostajesFiltrados),
            costePorKilometro = calcularCostePorKilometro(
                kilometrajeActual = kilometrajeActual,
                costeTotal = costeTotal,
                repostajes = repostajesFiltrados,
                mantenimientos = mantenimientosFiltrados
            ),
            costeTotal = costeTotal,
            totalOperaciones = repostajesFiltrados.size + mantenimientosFiltrados.size + gastosFiltrados.size,
            categorias = listOf(
                CategoriaEstadistica("Combustible", combustible),
                CategoriaEstadistica("Mantenimiento", mantenimiento),
                CategoriaEstadistica("Reparaciones", reparaciones),
                CategoriaEstadistica("Gastos periodicos", gastosPeriodicos)
            ),
            evolucionMensual = calcularEvolucionMensual(repostajesFiltrados, mantenimientosFiltrados, gastosFiltrados),
            estaCargando = false,
            mensajeError = null
        )
    }

    private fun calcularConsumoMedio(repostajes: List<Repostaje>): Double {
        val consumos = obtenerTramosValidos(repostajes).map { tramo ->
            (tramo.actual.litros / tramo.kilometrosRecorridos) * 100
        }

        return consumos.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun calcularCosteMedioCada100Km(repostajes: List<Repostaje>): Double {
        val costes = obtenerTramosValidos(repostajes).map { tramo ->
            (tramo.actual.importeTotal / tramo.kilometrosRecorridos) * 100
        }

        return costes.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun calcularCostePorKilometro(
        kilometrajeActual: Double,
        costeTotal: Double,
        repostajes: List<Repostaje>,
        mantenimientos: List<Mantenimiento>
    ): Double {
        val kilometrajeInicial = (repostajes.map { it.kilometros } + mantenimientos.mapNotNull { it.kilometros })
            .filter { it > 0 }
            .minOrNull()
            ?: return 0.0

        val kilometrosRecorridos = kilometrajeActual - kilometrajeInicial
        return if (kilometrosRecorridos > 0) costeTotal / kilometrosRecorridos else 0.0
    }

    private fun obtenerTramosValidos(repostajes: List<Repostaje>): List<TramoRepostaje> {
        val repostajesLlenos = repostajes
            .filter { it.llenoCompleto }
            .sortedBy { it.kilometros }

        return repostajesLlenos.zipWithNext().mapNotNull { (anterior, actual) ->
            val kilometrosRecorridos = actual.kilometros - anterior.kilometros
            if (kilometrosRecorridos <= 0 || actual.litros <= 0) {
                null
            } else {
                TramoRepostaje(actual = actual, kilometrosRecorridos = kilometrosRecorridos)
            }
        }
    }

    private fun calcularEvolucionMensual(
        repostajes: List<Repostaje>,
        mantenimientos: List<Mantenimiento>,
        gastos: List<GastoPeriodico>
    ): List<GastoMensualEstadistica> {
        val importesPorMes = linkedMapOf<String, Double>()
        val formato = SimpleDateFormat("MMM yyyy", Locale("es", "ES"))

        fun sumar(fecha: Long, importe: Double) {
            val clave = formato.format(fecha)
            importesPorMes[clave] = (importesPorMes[clave] ?: 0.0) + importe
        }

        repostajes.sortedBy { it.fecha }.forEach { sumar(it.fecha, it.importeTotal) }
        mantenimientos.sortedBy { it.fechaRealizado ?: it.fecha }.forEach { sumar(it.fechaRealizado ?: it.fecha, it.coste) }
        gastos.sortedBy { it.fechaPago ?: it.fecha }.forEach { sumar(it.fechaPago ?: it.fecha, it.importe) }

        return importesPorMes.map { (mes, total) ->
            GastoMensualEstadistica(mes = mes, total = total)
        }.takeLast(12)
    }

    private fun fechaDentroDePeriodo(fecha: Long, estado: EstadoEstadisticas): Boolean {
        val rango = obtenerRangoPeriodo(estado.periodoSeleccionado, estado.fechaInicioPersonalizada, estado.fechaFinPersonalizada)
            ?: return true

        return fecha in rango.first..rango.second
    }

    private fun obtenerRangoPeriodo(
        periodo: PeriodoRepostajes,
        fechaInicioPersonalizada: Long?,
        fechaFinPersonalizada: Long?
    ): Pair<Long, Long>? {
        val calendario = Calendar.getInstance()

        return when (periodo) {
            PeriodoRepostajes.TODO -> null
            PeriodoRepostajes.HOY -> inicioYFin(calendario)
            PeriodoRepostajes.SEMANA -> {
                calendario.firstDayOfWeek = Calendar.MONDAY
                calendario.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val inicio = inicioDelDia(calendario).timeInMillis
                Calendar.getInstance().let { hoy -> inicio to finDelDia(hoy).timeInMillis }
            }
            PeriodoRepostajes.MES -> {
                calendario.set(Calendar.DAY_OF_MONTH, 1)
                val inicio = inicioDelDia(calendario).timeInMillis
                Calendar.getInstance().let { hoy -> inicio to finDelDia(hoy).timeInMillis }
            }
            PeriodoRepostajes.ANIO -> {
                calendario.set(Calendar.DAY_OF_YEAR, 1)
                val inicio = inicioDelDia(calendario).timeInMillis
                Calendar.getInstance().let { hoy -> inicio to finDelDia(hoy).timeInMillis }
            }
            PeriodoRepostajes.PERSONALIZADO -> {
                val inicio = fechaInicioPersonalizada ?: return null
                val fin = fechaFinPersonalizada ?: return null
                val inicioNormalizado = inicioDelDia(Calendar.getInstance().apply { timeInMillis = minOf(inicio, fin) }).timeInMillis
                val finNormalizado = finDelDia(Calendar.getInstance().apply { timeInMillis = maxOf(inicio, fin) }).timeInMillis
                inicioNormalizado to finNormalizado
            }
        }
    }

    private fun inicioYFin(calendario: Calendar): Pair<Long, Long> {
        return inicioDelDia(calendario).timeInMillis to finDelDia(calendario).timeInMillis
    }

    private fun inicioDelDia(calendario: Calendar): Calendar {
        return calendario.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun finDelDia(calendario: Calendar): Calendar {
        return calendario.apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
    }

    private data class TramoRepostaje(
        val actual: Repostaje,
        val kilometrosRecorridos: Double
    )
}
