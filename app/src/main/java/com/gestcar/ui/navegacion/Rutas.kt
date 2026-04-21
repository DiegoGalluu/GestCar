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

    // pantallas de fase 2
    const val FORMULARIO_REPOSTAJE = "formulario_repostaje/{vehiculoId}/{repostajeId}"
    const val FORMULARIO_MANTENIMIENTO = "formulario_mantenimiento/{vehiculoId}/{mantenimientoId}"
    const val FORMULARIO_GASTO = "formulario_gasto/{vehiculoId}/{gastoId}"
    const val RECORDATORIOS = "recordatorios"
    const val FORMULARIO_RECORDATORIO = "formulario_recordatorio/{vehiculoId}/{recordatorioId}"
    const val ESTADISTICAS = "estadisticas"

    // funciones helper para construir rutas con parametros
    fun formularioVehiculo(vehiculoId: String = "nuevo") =
        "formulario_vehiculo/$vehiculoId"

    fun detalleVehiculo(vehiculoId: String) =
        "detalle_vehiculo/$vehiculoId"

    fun formularioRepostaje(vehiculoId: String, repostajeId: String = "nuevo") =
        "formulario_repostaje/$vehiculoId/$repostajeId"

    fun formularioMantenimiento(vehiculoId: String, mantenimientoId: String = "nuevo") =
        "formulario_mantenimiento/$vehiculoId/$mantenimientoId"

    fun formularioGasto(vehiculoId: String, gastoId: String = "nuevo") =
        "formulario_gasto/$vehiculoId/$gastoId"

    fun formularioRecordatorio(vehiculoId: String, recordatorioId: String = "nuevo") =
        "formulario_recordatorio/$vehiculoId/$recordatorioId"
}
