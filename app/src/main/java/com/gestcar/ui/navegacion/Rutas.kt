package com.gestcar.ui.navegacion

// aqui se definen todas las rutas de navegacion de la app
// cada pantalla tiene su propia ruta como string constante
object Rutas {
    // pantallas de autenticacion
    const val INICIO_SESION = "inicio_sesion"
    const val REGISTRO = "registro"
    const val RESTABLECER_CONTRASENA = "restablecer_contrasena"

    // pantallas principales con barra de navegacion inferior
    const val LISTA_VEHICULOS = "lista_vehiculos"
    const val FORMULARIO_VEHICULO = "formulario_vehiculo/{vehiculoId}"
    const val DETALLE_VEHICULO = "detalle_vehiculo/{vehiculoId}"

    // pantallas placeholder para futuras fases
    const val GASTOS = "gastos"
    const val REPOSTAJES = "repostajes"
    const val MANTENIMIENTO = "mantenimiento"
    const val MAS_OPCIONES = "mas_opciones"

    // funciones helper para construir rutas con parametros
    fun formularioVehiculo(vehiculoId: String = "nuevo") =
        "formulario_vehiculo/$vehiculoId"

    fun detalleVehiculo(vehiculoId: String) =
        "detalle_vehiculo/$vehiculoId"
}
