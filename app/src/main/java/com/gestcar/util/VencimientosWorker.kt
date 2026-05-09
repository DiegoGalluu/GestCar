package com.gestcar.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gestcar.ActividadPrincipal
import com.gestcar.R
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.datos.remoto.ClienteSupabase
import io.github.jan.supabase.auth.auth
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.absoluteValue

class VencimientosWorker(
    contexto: Context,
    parametros: WorkerParameters
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        return try {
            if (!puedeMostrarNotificaciones()) {
                return Result.success()
            }

            ClienteSupabase.cliente.auth.awaitInitialization()
            ClienteSupabase.cliente.auth.loadFromStorage()

            val usuarioId = ClienteSupabase.cliente.auth
                .currentSessionOrNull()
                ?.user
                ?.id
                .orEmpty()

            if (usuarioId.isBlank()) {
                return Result.success()
            }

            val baseDatos = GestCarBaseDatos.obtenerInstancia(applicationContext)
            val vehiculos = baseDatos.vehiculoDao().obtenerVehiculosPorUsuarioLista(usuarioId)
            val avisos = vehiculos.flatMap { vehiculo ->
                baseDatos.gastoPeriodicoDao()
                    .obtenerPorVehiculoLista(vehiculo.id)
                    .mapNotNull { gasto -> crearAvisoSiToca(vehiculo, gasto) }
            }

            if (avisos.isNotEmpty()) {
                crearCanalNotificaciones()
                mostrarNotificacion(avisos)
                guardarAvisosEnviados(avisos)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun crearAvisoSiToca(vehiculo: Vehiculo, gasto: GastoPeriodico): AvisoVencimiento? {
        val fechaVencimiento = gasto.fechaVencimiento ?: return null
        if (gasto.pagado) return null

        val hoy = LocalDate.now()
        val fecha = fechaVencimiento.aFechaLocal()
        val diasRestantes = ChronoUnit.DAYS.between(hoy, fecha).toInt()
        val tipoAviso = tipoAvisoPara(diasRestantes, hoy) ?: return null
        val clave = claveAviso(gasto.id, tipoAviso, hoy)

        if (avisoYaEnviado(clave)) {
            return null
        }

        return AvisoVencimiento(
            gasto = gasto,
            vehiculo = vehiculo,
            tipo = tipoAviso,
            clave = clave,
            diasRestantes = diasRestantes
        )
    }

    private fun tipoAvisoPara(diasRestantes: Int, hoy: LocalDate): TipoAvisoVencimiento? {
        return when {
            diasRestantes == 30 -> TipoAvisoVencimiento.TREINTA_DIAS
            diasRestantes == 15 -> TipoAvisoVencimiento.QUINCE_DIAS
            diasRestantes in 1..7 -> TipoAvisoVencimiento.ESTA_SEMANA
            diasRestantes == 0 -> TipoAvisoVencimiento.HOY
            diasRestantes < 0 && diasRestantes.absoluteValue <= 7 -> TipoAvisoVencimiento.VENCIDO_DIARIO
            diasRestantes < 0 && hoy.dayOfWeek.value == 1 -> TipoAvisoVencimiento.VENCIDO_SEMANAL
            else -> null
        }
    }

    private fun claveAviso(gastoId: String, tipoAviso: TipoAvisoVencimiento, hoy: LocalDate): String {
        return when (tipoAviso) {
            TipoAvisoVencimiento.TREINTA_DIAS -> "$gastoId-30"
            TipoAvisoVencimiento.QUINCE_DIAS -> "$gastoId-15"
            TipoAvisoVencimiento.ESTA_SEMANA -> "$gastoId-semana-$hoy"
            TipoAvisoVencimiento.HOY -> "$gastoId-hoy-$hoy"
            TipoAvisoVencimiento.VENCIDO_DIARIO -> "$gastoId-vencido-$hoy"
            TipoAvisoVencimiento.VENCIDO_SEMANAL -> "$gastoId-vencido-${hoy.anioSemana()}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun mostrarNotificacion(avisos: List<AvisoVencimiento>) {
        val notificacion = if (avisos.size == 1) {
            crearNotificacionIndividual(avisos.first())
        } else {
            crearNotificacionResumen(avisos)
        }

        NotificationManagerCompat.from(applicationContext).notify(ID_NOTIFICACION_VENCIMIENTOS, notificacion)
    }

    private fun crearNotificacionIndividual(aviso: AvisoVencimiento) =
        NotificationCompat.Builder(applicationContext, CANAL_VENCIMIENTOS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(aviso.titulo)
            .setContentText(aviso.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(aviso.textoCompleto))
            .setContentIntent(intentAbrirApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    private fun crearNotificacionResumen(avisos: List<AvisoVencimiento>) =
        NotificationCompat.Builder(applicationContext, CANAL_VENCIMIENTOS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Tienes ${avisos.size} gastos con aviso")
            .setContentText(avisos.take(3).joinToString(", ") { it.gasto.concepto })
            .setStyle(
                NotificationCompat.InboxStyle().also { estilo ->
                    avisos.take(5).forEach { estilo.addLine(it.textoCompleto) }
                }
            )
            .setContentIntent(intentAbrirApp())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    private fun intentAbrirApp(): PendingIntent {
        val intent = Intent(applicationContext, ActividadPrincipal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canal = NotificationChannel(
            CANAL_VENCIMIENTOS,
            "Vencimientos de GestCar",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de gastos próximos a vencer o vencidos"
        }

        val gestor = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gestor.createNotificationChannel(canal)
    }

    private fun puedeMostrarNotificaciones(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun avisoYaEnviado(clave: String): Boolean {
        return preferenciasAvisos().getBoolean(clave, false)
    }

    private fun guardarAvisosEnviados(avisos: List<AvisoVencimiento>) {
        preferenciasAvisos().edit().apply {
            avisos.forEach { putBoolean(it.clave, true) }
        }.apply()
    }

    private fun preferenciasAvisos() =
        applicationContext.getSharedPreferences("avisos_vencimientos", Context.MODE_PRIVATE)

    private fun Long.aFechaLocal(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun LocalDate.anioSemana(): String {
        val camposSemana = WeekFields.of(Locale("es", "ES"))
        return "${get(camposSemana.weekBasedYear())}-${get(camposSemana.weekOfWeekBasedYear())}"
    }

    private val AvisoVencimiento.titulo: String
        get() = when (tipo) {
            TipoAvisoVencimiento.HOY -> "Gasto vence hoy"
            TipoAvisoVencimiento.VENCIDO_DIARIO,
            TipoAvisoVencimiento.VENCIDO_SEMANAL -> "Gasto vencido"
            else -> "Gasto con vencimiento"
        }

    private val AvisoVencimiento.texto: String
        get() = when (tipo) {
            TipoAvisoVencimiento.TREINTA_DIAS -> "${gasto.concepto} vence en 30 días"
            TipoAvisoVencimiento.QUINCE_DIAS -> "${gasto.concepto} vence en 15 días"
            TipoAvisoVencimiento.ESTA_SEMANA -> "${gasto.concepto} vence esta semana"
            TipoAvisoVencimiento.HOY -> "${gasto.concepto} vence hoy"
            TipoAvisoVencimiento.VENCIDO_DIARIO,
            TipoAvisoVencimiento.VENCIDO_SEMANAL -> "${gasto.concepto} está pendiente de pago"
        }

    private val AvisoVencimiento.textoCompleto: String
        get() = "${vehiculo.marca} ${vehiculo.modelo} - $texto"

    companion object {
        private const val CANAL_VENCIMIENTOS = "gestcar_vencimientos"
        private const val ID_NOTIFICACION_VENCIMIENTOS = 5401
    }
}

private data class AvisoVencimiento(
    val gasto: GastoPeriodico,
    val vehiculo: Vehiculo,
    val tipo: TipoAvisoVencimiento,
    val clave: String,
    val diasRestantes: Int
)

private enum class TipoAvisoVencimiento {
    TREINTA_DIAS,
    QUINCE_DIAS,
    ESTA_SEMANA,
    HOY,
    VENCIDO_DIARIO,
    VENCIDO_SEMANAL
}
