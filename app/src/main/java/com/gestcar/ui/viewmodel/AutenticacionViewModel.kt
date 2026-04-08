package com.gestcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.remoto.ClienteSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// estados posibles de la autenticacion
data class EstadoAutenticacion(
    // si el usuario esta logueado o no
    val estaAutenticado: Boolean = false,
    // id del usuario en supabase, se usa para filtrar sus datos
    val usuarioId: String = "",
    // si hay alguna operacion de auth en curso
    val estaCargando: Boolean = false,
    // mensaje de error si algo ha fallado
    val mensajeError: String? = null
)

// viewmodel que gestiona todo lo relacionado con la autenticacion
// login, registro, cerrar sesion y comprobar si hay sesion activa
class AutenticacionViewModel : ViewModel() {

    // estado observable de la autenticacion
    private val _estado = MutableStateFlow(EstadoAutenticacion())
    val estado: StateFlow<EstadoAutenticacion> = _estado.asStateFlow()

    // al crear el viewmodel comprobamos si ya hay una sesion activa
    // por si el usuario ya se habia logueado antes
    init {
        comprobarSesion()
    }

    // comprueba si hay una sesion activa en supabase
    private fun comprobarSesion() {
        viewModelScope.launch {
            try {
                val sesion = ClienteSupabase.cliente.auth.currentSessionOrNull()
                if (sesion != null) {
                    _estado.value = EstadoAutenticacion(
                        estaAutenticado = true,
                        usuarioId = sesion.user?.id ?: ""
                    )
                }
            } catch (e: Exception) {
                // si falla simplemente no hay sesion activa
            }
        }
    }

    // inicia sesion con email y contrasena
    fun iniciarSesion(email: String, contrasena: String) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(estaCargando = true, mensajeError = null)
            try {
                ClienteSupabase.cliente.auth.signInWith(Email) {
                    this.email = email
                    this.password = contrasena
                }

                val usuario = ClienteSupabase.cliente.auth.currentUserOrNull()
                _estado.value = EstadoAutenticacion(
                    estaAutenticado = true,
                    usuarioId = usuario?.id ?: ""
                )
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = "Error al iniciar sesion, comprueba tus credenciales"
                )
            }
        }
    }

    // registra una cuenta nueva con email y contrasena
    fun registrarse(email: String, contrasena: String) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(estaCargando = true, mensajeError = null)
            try {
                ClienteSupabase.cliente.auth.signUpWith(Email) {
                    this.email = email
                    this.password = contrasena
                }

                val usuario = ClienteSupabase.cliente.auth.currentUserOrNull()
                _estado.value = EstadoAutenticacion(
                    estaAutenticado = true,
                    usuarioId = usuario?.id ?: ""
                )
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = "Error al crear la cuenta, intentalo de nuevo"
                )
            }
        }
    }

    // cierra la sesion del usuario
    fun cerrarSesion() {
        viewModelScope.launch {
            try {
                ClienteSupabase.cliente.auth.signOut()
            } catch (e: Exception) {
                // aunque falle el logout remoto, cerramos la sesion local
            }
            _estado.value = EstadoAutenticacion()
        }
    }

    // limpia el mensaje de error, se usa cuando el usuario cierra el snackbar
    fun limpiarError() {
        _estado.value = _estado.value.copy(mensajeError = null)
    }
}
