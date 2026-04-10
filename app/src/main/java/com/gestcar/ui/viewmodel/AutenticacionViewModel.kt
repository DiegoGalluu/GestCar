package com.gestcar.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.remoto.ClienteSupabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
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
    val mensajeError: String? = null,
    // indica si debemos mostrar la pantalla de nueva contrasena
    val modoRestablecerContrasena: Boolean = false
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
    fun comprobarSesion() {
        viewModelScope.launch {
            try {
                ClienteSupabase.cliente.auth.awaitInitialization()
                ClienteSupabase.cliente.auth.loadFromStorage()
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
                ClienteSupabase.cliente.auth.clearSession()
                ClienteSupabase.cliente.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = contrasena
                }

                actualizarEstadoDesdeSesion(
                    errorSinSesion = "No se pudo recuperar la sesion. Comprueba tu correo y vuelve a intentarlo."
                )
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = mensajeErrorLegible(
                        porDefecto = "Error al iniciar sesion. Comprueba tus credenciales y que el correo este confirmado.",
                        error = e
                    )
                )
            }
        }
    }

    // registra una cuenta nueva con email y contrasena
    fun registrarse(email: String, contrasena: String) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(estaCargando = true, mensajeError = null)
            try {
                ClienteSupabase.cliente.auth.clearSession()
                ClienteSupabase.cliente.auth.signUpWith(
                    provider = Email,
                    redirectUrl = ClienteSupabase.AUTH_DEEP_LINK
                ) {
                    this.email = email.trim()
                    this.password = contrasena
                }

                actualizarEstadoDesdeSesion(
                    errorSinSesion = "Cuenta creada. Revisa tu correo para confirmarla antes de iniciar sesion."
                )
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = mensajeErrorLegible(
                        porDefecto = "Error al crear la cuenta. Intentalo de nuevo.",
                        error = e
                    )
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

    fun solicitarRestablecimientoContrasena(email: String) {
        viewModelScope.launch {
            val correo = email.trim()
            if (correo.isBlank()) {
                _estado.value = _estado.value.copy(
                    mensajeError = "Introduce tu correo electronico para enviarte el enlace de recuperacion."
                )
                return@launch
            }

            _estado.value = _estado.value.copy(estaCargando = true, mensajeError = null)
            try {
                ClienteSupabase.cliente.auth.resetPasswordForEmail(
                    email = correo,
                    redirectUrl = ClienteSupabase.AUTH_DEEP_LINK
                )
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = "Te hemos enviado un correo para restablecer la contrasena."
                )
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = mensajeErrorLegible(
                        porDefecto = "No se ha podido enviar el correo de recuperacion.",
                        error = e
                    )
                )
            }
        }
    }

    fun activarModoRestablecerContrasena() {
        _estado.value = _estado.value.copy(
            modoRestablecerContrasena = true,
            mensajeError = null
        )
    }

    fun salirModoRestablecerContrasena() {
        _estado.value = _estado.value.copy(modoRestablecerContrasena = false)
    }

    fun actualizarContrasena(nuevaContrasena: String) {
        viewModelScope.launch {
            if (nuevaContrasena.isBlank()) {
                _estado.value = _estado.value.copy(
                    mensajeError = "La nueva contrasena no puede estar vacia."
                )
                return@launch
            }

            _estado.value = _estado.value.copy(estaCargando = true, mensajeError = null)
            try {
                ClienteSupabase.cliente.auth.updateUser {
                    password = nuevaContrasena
                }
                comprobarSesion()
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    modoRestablecerContrasena = false,
                    mensajeError = "Contrasena actualizada. Ya puedes iniciar sesion con la nueva clave."
                )
            } catch (e: Exception) {
                _estado.value = _estado.value.copy(
                    estaCargando = false,
                    mensajeError = mensajeErrorLegible(
                        porDefecto = "No se ha podido actualizar la contrasena.",
                        error = e
                    )
                )
            }
        }
    }

    private suspend fun actualizarEstadoDesdeSesion(errorSinSesion: String) {
        ClienteSupabase.cliente.auth.awaitInitialization()
        val sesion = ClienteSupabase.cliente.auth.currentSessionOrNull()
        val usuario = sesion?.user

        _estado.value = if (sesion != null && usuario?.id?.isNotBlank() == true) {
            EstadoAutenticacion(
                estaAutenticado = true,
                usuarioId = usuario.id
            )
        } else {
            EstadoAutenticacion(
                estaAutenticado = false,
                usuarioId = "",
                estaCargando = false,
                mensajeError = errorSinSesion
            )
        }
    }

    private fun mensajeErrorLegible(porDefecto: String, error: Exception): String {
        val detalle = when (error) {
            is AuthRestException -> error.description
            else -> error.message
        }?.trim()

        return if (detalle.isNullOrBlank()) porDefecto else "$porDefecto\n$detalle"
    }
}
