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
import java.util.Calendar
import java.util.UUID

enum class PeriodoRepostajes {
    HOY,
    SEMANA,
    MES,
    ANIO,
    TODO,
    PERSONALIZADO
}

data class EstadoListaRepostajes(
    val repostajes: List<Repostaje> = emptyList(),
    val repostajesFiltrados: List<Repostaje> = emptyList(),
    val consumoMedio: Double = 0.0,
    val costeMedioCada100Km: Double = 0.0,
    val periodoSeleccionado: PeriodoRepostajes = PeriodoRepostajes.TODO,
    val fechaInicioPersonalizada: Long? = null,
    val fechaFinPersonalizada: Long? = null,
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
                _estadoLista.value = construirEstadoLista(
                    repostajes = repostajes,
                    estadoActual = _estadoLista.value,
                    mensajeError = resultadoSincronizacion.exceptionOrNull()?.message
                )
            }
        }
    }

    fun seleccionarPeriodo(periodo: PeriodoRepostajes) {
        _estadoLista.value = construirEstadoLista(
            repostajes = _estadoLista.value.repostajes,
            estadoActual = _estadoLista.value.copy(periodoSeleccionado = periodo)
        )
    }

    fun seleccionarRangoPersonalizado(fechaInicio: Long, fechaFin: Long) {
        _estadoLista.value = construirEstadoLista(
            repostajes = _estadoLista.value.repostajes,
            estadoActual = _estadoLista.value.copy(
                periodoSeleccionado = PeriodoRepostajes.PERSONALIZADO,
                fechaInicioPersonalizada = fechaInicio,
                fechaFinPersonalizada = fechaFin
            )
        )
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
        val tramos = obtenerTramosValidos(repostajes)
        val consumos = tramos.map { tramo ->
            (tramo.actual.litros / tramo.kilometrosRecorridos) * 100
        }

        return consumos.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun calcularCosteMedioCada100Km(repostajes: List<Repostaje>): Double {
        val tramos = obtenerTramosValidos(repostajes)
        val costes = tramos.map { tramo ->
            (tramo.actual.importeTotal / tramo.kilometrosRecorridos) * 100
        }

        return costes.takeIf { it.isNotEmpty() }?.average() ?: 0.0
    }

    private fun construirEstadoLista(
        repostajes: List<Repostaje>,
        estadoActual: EstadoListaRepostajes,
        mensajeError: String? = estadoActual.mensajeError
    ): EstadoListaRepostajes {
        val filtrados = filtrarPorPeriodo(
            repostajes = repostajes,
            periodo = estadoActual.periodoSeleccionado,
            fechaInicioPersonalizada = estadoActual.fechaInicioPersonalizada,
            fechaFinPersonalizada = estadoActual.fechaFinPersonalizada
        )

        return estadoActual.copy(
            repostajes = repostajes,
            repostajesFiltrados = filtrados,
            consumoMedio = calcularConsumoMedioDesdeLista(filtrados),
            costeMedioCada100Km = calcularCosteMedioCada100Km(filtrados),
            estaCargando = false,
            mensajeError = mensajeError
        )
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

    private fun filtrarPorPeriodo(
        repostajes: List<Repostaje>,
        periodo: PeriodoRepostajes,
        fechaInicioPersonalizada: Long?,
        fechaFinPersonalizada: Long?
    ): List<Repostaje> {
        val rango = obtenerRangoPeriodo(periodo, fechaInicioPersonalizada, fechaFinPersonalizada)
            ?: return repostajes

        return repostajes.filter { it.fecha in rango.first..rango.second }
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
