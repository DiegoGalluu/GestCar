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

    // secciones principales de la barra inferior
    const val GASTOS = "gastos"
    const val REPOSTAJES = "repostajes"
    const val MANTENIMIENTO = "mantenimiento"
    const val MAS_OPCIONES = "mas_opciones"
    const val GASTOS_VEHICULO = "gastos/{vehiculoId}"
    const val REPOSTAJES_VEHICULO = "repostajes/{vehiculoId}"
    const val MANTENIMIENTO_VEHICULO = "mantenimiento/{vehiculoId}"
    const val RECORDATORIOS_VEHICULO = "recordatorios/{vehiculoId}"

    // pantallas secundarias de gestion
    const val FORMULARIO_REPOSTAJE = "formulario_repostaje/{vehiculoId}/{repostajeId}"
    const val DETALLE_REPOSTAJE = "detalle_repostaje/{vehiculoId}/{repostajeId}"
    const val FORMULARIO_MANTENIMIENTO = "formulario_mantenimiento/{vehiculoId}/{mantenimientoId}"
    const val DETALLE_MANTENIMIENTO = "detalle_mantenimiento/{vehiculoId}/{mantenimientoId}"
    const val FORMULARIO_GASTO = "formulario_gasto/{vehiculoId}/{gastoId}"
    const val DETALLE_GASTO = "detalle_gasto/{vehiculoId}/{gastoId}"
    const val RECORDATORIOS = "recordatorios"
    const val FORMULARIO_RECORDATORIO = "formulario_recordatorio/{vehiculoId}/{recordatorioId}"
    const val DETALLE_RECORDATORIO = "detalle_recordatorio/{vehiculoId}/{recordatorioId}"
    const val ESTADISTICAS = "estadisticas"
    const val CUENTA = "cuenta"

    // funciones helper para construir rutas con parametros
    // centralizar esto evita errores de escribir a mano strings con ids
    // tambien facilita cambiar una ruta sin tocar todas las pantallas
    fun formularioVehiculo(vehiculoId: String = "nuevo") =
        "formulario_vehiculo/$vehiculoId"

    fun detalleVehiculo(vehiculoId: String) =
        "detalle_vehiculo/$vehiculoId"

    fun formularioRepostaje(vehiculoId: String, repostajeId: String = "nuevo") =
        "formulario_repostaje/$vehiculoId/$repostajeId"

    fun repostajesVehiculo(vehiculoId: String) =
        "repostajes/$vehiculoId"

    fun detalleRepostaje(vehiculoId: String, repostajeId: String) =
        "detalle_repostaje/$vehiculoId/$repostajeId"

    fun formularioMantenimiento(vehiculoId: String, mantenimientoId: String = "nuevo") =
        "formulario_mantenimiento/$vehiculoId/$mantenimientoId"

    fun mantenimientoVehiculo(vehiculoId: String) =
        "mantenimiento/$vehiculoId"

    fun detalleMantenimiento(vehiculoId: String, mantenimientoId: String) =
        "detalle_mantenimiento/$vehiculoId/$mantenimientoId"

    fun formularioGasto(vehiculoId: String, gastoId: String = "nuevo") =
        "formulario_gasto/$vehiculoId/$gastoId"

    fun gastosVehiculo(vehiculoId: String) =
        "gastos/$vehiculoId"

    fun detalleGasto(vehiculoId: String, gastoId: String) =
        "detalle_gasto/$vehiculoId/$gastoId"

    fun formularioRecordatorio(vehiculoId: String, recordatorioId: String = "nuevo") =
        "formulario_recordatorio/$vehiculoId/$recordatorioId"

    fun recordatoriosVehiculo(vehiculoId: String) =
        "recordatorios/$vehiculoId"

    fun detalleRecordatorio(vehiculoId: String, recordatorioId: String) =
        "detalle_recordatorio/$vehiculoId/$recordatorioId"
}
