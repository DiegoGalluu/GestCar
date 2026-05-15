package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.remoto.ClienteSupabase
import io.github.jan.supabase.auth.auth
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EstadoCuenta(
    val estaEliminando: Boolean = false,
    val cuentaEliminada: Boolean = false,
    val mensajeError: String? = null
)

class CuentaViewModel(application: Application) : AndroidViewModel(application) {

    // se usa androidviewmodel porque al eliminar cuenta tambien limpiamos archivos locales
    // para eso necesitamos acceder al almacenamiento privado de la app
    private val baseDatos = GestCarBaseDatos.obtenerInstancia(application)

    private val _estado = MutableStateFlow(EstadoCuenta())
    val estado: StateFlow<EstadoCuenta> = _estado.asStateFlow()

    fun eliminarCuenta(usuarioId: String) {
        if (usuarioId.isBlank() || _estado.value.estaEliminando) {
            return
        }

        viewModelScope.launch {
            _estado.value = _estado.value.copy(estaEliminando = true, mensajeError = null)
            try {
                llamarFuncionEliminarCuenta()

                // limpiamos tambien la cache local para no dejar datos sensibles en el dispositivo
                // al borrar vehiculos room elimina en cascada el resto de tablas relacionadas
                eliminarAdjuntosLocalesDelUsuario(usuarioId)
                baseDatos.vehiculoDao().eliminarTodosPorUsuario(usuarioId)
                File(getApplication<Application>().filesDir, "vehiculos_imagenes/$usuarioId")
                    .deleteRecursively()

                ClienteSupabase.cliente.auth.clearSession()
                _estado.value = EstadoCuenta(cuentaEliminada = true)
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaEliminando = false,
                    mensajeError = e.message ?: "No se ha podido eliminar la cuenta."
                )
            }
        }
    }

    private suspend fun llamarFuncionEliminarCuenta() {
        // la eliminacion real vive en una edge function con permisos de servidor
        // asi storage se limpia con su api oficial y la app nunca guarda service role
        ClienteSupabase.cliente.auth.awaitInitialization()
        ClienteSupabase.cliente.auth.loadFromStorage()
        val token = ClienteSupabase.cliente.auth.currentSessionOrNull()
            ?.accessToken
            ?: error("No hay sesion activa para eliminar la cuenta.")

        withContext(Dispatchers.IO) {
            val conexion = (URL("${ClienteSupabase.SUPABASE_URL}/functions/v1/eliminar-cuenta")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("apikey", ClienteSupabase.SUPABASE_KEY)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            runCatching {
                conexion.outputStream.use { salida ->
                    salida.write("{}".toByteArray(Charsets.UTF_8))
                }

                val codigo = conexion.responseCode
                val cuerpo = if (codigo in 200..299) {
                    conexion.inputStream.bufferedReader().use { it.readText() }
                } else {
                    conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                if (codigo !in 200..299) {
                    error("Error al eliminar cuenta. Codigo $codigo: $cuerpo")
                }
            }.also {
                conexion.disconnect()
            }.getOrThrow()
        }
    }

    private suspend fun eliminarAdjuntosLocalesDelUsuario(usuarioId: String) {
        val filesDir = getApplication<Application>().filesDir
        baseDatos.vehiculoDao()
            .obtenerVehiculosPorUsuarioLista(usuarioId)
            .forEach { vehiculo ->
                baseDatos.documentoVehiculoDao()
                    .obtenerPorVehiculoLista(vehiculo.id)
                    .forEach { documento ->
                        File(filesDir, "documentos_adjuntos/${documento.id}")
                            .deleteRecursively()
                    }
            }
    }

    fun limpiarError() {
        _estado.value = _estado.value.copy(mensajeError = null)
    }
}
