package com.gestcar.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.remoto.ClienteSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoCuenta(
    val estaEliminando: Boolean = false,
    val cuentaEliminada: Boolean = false,
    val mensajeError: String? = null
)

class CuentaViewModel(application: Application) : AndroidViewModel(application) {

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
                // la funcion vive en supabase y solo puede borrar la cuenta autenticada
                ClienteSupabase.cliente.postgrest.rpc("eliminar_cuenta_actual")

                // limpiamos tambien la cache local para no dejar datos sensibles en el dispositivo
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

    fun limpiarError() {
        _estado.value = _estado.value.copy(mensajeError = null)
    }
}
