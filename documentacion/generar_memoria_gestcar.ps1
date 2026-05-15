$ErrorActionPreference = "Stop"

$raiz = Split-Path -Parent $PSScriptRoot
$htmlSalida = Join-Path $PSScriptRoot "Memoria_Tecnica_GestCar.html"
$docxSalida = Join-Path $PSScriptRoot "Memoria_Tecnica_GestCar.docx"
$soffice = "C:\Program Files\LibreOffice\program\soffice.exe"

function Escape-Html {
    param([string]$texto)
    if ($null -eq $texto) { return "" }
    return [System.Net.WebUtility]::HtmlEncode($texto)
}

function Parrafo {
    param([string]$texto)
    return "<p>$texto</p>"
}

function Placeholder {
    param(
        [string]$titulo,
        [string]$descripcion = ""
    )
    $descripcionHtml = ""
    if ($descripcion.Trim().Length -gt 0) {
        $descripcionHtml = "<div class='placeholder-desc'>$descripcion</div>"
    }
    return @"
<table class="placeholder">
  <tr>
    <td>
      <div class="placeholder-title">$titulo</div>
      $descripcionHtml
    </td>
  </tr>
</table>
"@
}

function Nota {
    param([string]$texto)
    return "<div class='nota'>$texto</div>"
}

function Tabla {
    param(
        [string[]]$cabeceras,
        [object[]]$filas
    )
    $html = "<table class='tabla'><thead><tr>"
    foreach ($cabecera in $cabeceras) {
        $html += "<th>$cabecera</th>"
    }
    $html += "</tr></thead><tbody>"
    foreach ($fila in $filas) {
        $html += "<tr>"
        foreach ($celda in $fila) {
            $html += "<td>$celda</td>"
        }
        $html += "</tr>"
    }
    $html += "</tbody></table>"
    return $html
}

function Lista {
    param([string[]]$items)
    $html = "<ul>"
    foreach ($item in $items) {
        $html += "<li>$item</li>"
    }
    $html += "</ul>"
    return $html
}

$requisitosFuncionales = @(
    @("RF-01", "Registro de usuario", "La aplicación permite crear una cuenta mediante correo electrónico y contraseña, solicitando confirmación por correo antes de permitir el uso completo."),
    @("RF-02", "Inicio de sesión", "El usuario puede iniciar sesión con credenciales válidas y conservar la sesión entre cierres de la aplicación."),
    @("RF-03", "Recuperación de contraseña", "La aplicación permite solicitar un enlace de restablecimiento y cambiar la contraseña desde un flujo específico."),
    @("RF-04", "Gestión de vehículos", "El usuario puede crear, consultar, editar, eliminar y ordenar vehículos, incluyendo datos técnicos, kilometraje y fotografía."),
    @("RF-05", "Vehículos habituales", "El usuario puede organizar los vehículos en habituales y secundarios para priorizar selectores y pantallas de consulta."),
    @("RF-06", "Gestión de repostajes", "El usuario puede registrar repostajes con fecha, kilómetros, litros, precio por litro, importe, gasolinera y notas."),
    @("RF-07", "Cálculo de consumo", "La aplicación calcula consumo medio en litros cada cien kilómetros y coste medio de combustible cada cien kilómetros."),
    @("RF-08", "Gestión de mantenimientos", "El usuario puede registrar mantenimientos pendientes o realizados, con componente libre, fecha, kilómetros, coste, taller y descripción."),
    @("RF-09", "Gestión de reparaciones", "La aplicación separa reparaciones de mantenimientos sin imponer listas cerradas de averías."),
    @("RF-10", "Gestión de gastos", "El usuario puede registrar gastos únicos o periódicos, distinguir pendientes y pagados y controlar vencimientos."),
    @("RF-11", "Recordatorios", "El usuario puede crear recordatorios por fecha, kilometraje o ambos, incluyendo periodicidad temporal y por kilómetros."),
    @("RF-12", "Notificaciones", "La aplicación programa avisos para vencimientos próximos y recordatorios relevantes mediante tareas en segundo plano."),
    @("RF-13", "Estadísticas", "La aplicación resume consumo, coste medio, coste acumulado, operaciones y métricas útiles por vehículo."),
    @("RF-14", "Exportación", "La aplicación ofrece opciones de exportación y resumen compartible para facilitar el uso externo de la información."),
    @("RF-15", "Documentación", "El usuario puede crear tarjetas de documentación con campos personalizados y adjuntos asociados."),
    @("RF-16", "Adjuntos", "La documentación admite imágenes, fotografías de cámara y documentos seleccionados desde el gestor del sistema."),
    @("RF-17", "Cuenta", "El usuario puede consultar datos básicos de cuenta, cerrar sesión, cambiar contraseña y solicitar eliminación de cuenta."),
    @("RF-18", "Conectividad", "La aplicación informa visualmente cuando no hay conexión y cuando se recupera la red."),
    @("RF-19", "Gasolineras", "La aplicación incorpora una pantalla para consultar gasolineras cercanas usando ubicación, rango y datos externos de precios.")
)

$requisitosNoFuncionales = @(
    @("RNF-01", "Privacidad", "Los datos deben estar asociados al usuario autenticado y no deben mezclarse entre cuentas."),
    @("RNF-02", "Seguridad", "No se deben exponer claves privadas, tokens, URLs internas sensibles ni identificadores técnicos innecesarios al usuario final."),
    @("RNF-03", "Disponibilidad offline", "La aplicación debe poder consultar y crear registros sin conexión gracias a Room."),
    @("RNF-04", "Sincronización", "Los datos locales deben sincronizarse con el backend cuando exista conexión, incluyendo reintentos en segundo plano."),
    @("RNF-05", "Usabilidad", "Los formularios deben señalar errores de validación, evitar campos rígidos innecesarios y facilitar el uso desde móvil."),
    @("RNF-06", "Mantenibilidad", "La arquitectura debe separar pantallas, ViewModels, repositorios, DAOs, entidades y utilidades."),
    @("RNF-07", "Compatibilidad", "La app debe ejecutarse en Android 8.0 o superior, con target SDK moderno."),
    @("RNF-08", "Rendimiento", "Las imágenes deben comprimirse y mantenerse en almacenamiento privado para no penalizar carga ni consumo de datos."),
    @("RNF-09", "Escalabilidad funcional", "El modelo debe permitir añadir nuevas secciones sin reescribir la arquitectura base."),
    @("RNF-10", "Accesibilidad básica", "La navegación debe ser clara y no depender exclusivamente de los botones físicos del sistema.")
)

$casosUso = @(
    @("CU-01", "Registrarse", "Usuario no autenticado", "Introduce correo y contraseña, recibe confirmación y activa la cuenta."),
    @("CU-02", "Iniciar sesión", "Usuario registrado", "Accede a sus datos y mantiene sesión persistente."),
    @("CU-03", "Crear vehículo", "Usuario autenticado", "Registra marca, modelo, matrícula, kilometraje y datos técnicos."),
    @("CU-04", "Añadir foto", "Usuario autenticado", "Selecciona cámara o galería y guarda una imagen comprimida del vehículo."),
    @("CU-05", "Registrar repostaje", "Usuario autenticado", "Introduce datos de combustible y actualiza métricas del vehículo."),
    @("CU-06", "Registrar mantenimiento", "Usuario autenticado", "Añade una tarea pendiente o una operación realizada."),
    @("CU-07", "Controlar gasto", "Usuario autenticado", "Registra pago, vencimiento y periodicidad de un gasto."),
    @("CU-08", "Crear recordatorio", "Usuario autenticado", "Configura aviso por fecha, kilómetros o recurrencia."),
    @("CU-09", "Consultar documentación", "Usuario autenticado", "Accede a tarjetas personalizadas y adjuntos del vehículo."),
    @("CU-10", "Exportar información", "Usuario autenticado", "Genera un resumen o archivo reutilizable con los datos del vehículo."),
    @("CU-11", "Buscar gasolineras", "Usuario autenticado", "Consulta puntos cercanos y precios mediante ubicación y rango."),
    @("CU-12", "Eliminar cuenta", "Usuario autenticado", "Confirma dos veces la eliminación para evitar acciones accidentales.")
)

$entidades = @(
    @("Vehiculo", "vehiculos", "Datos principales del vehículo, foto, kilometraje, orden y pertenencia al usuario."),
    @("Repostaje", "repostajes", "Historial de combustible asociado a un vehículo."),
    @("Mantenimiento", "mantenimientos", "Operaciones mecánicas pendientes o realizadas, separadas entre mantenimiento y reparación."),
    @("GastoPeriodico", "gastos_periodicos", "Gastos únicos o recurrentes, vencimientos y estado de pago."),
    @("Recordatorio", "recordatorios", "Avisos por fecha, kilometraje y periodicidad."),
    @("DocumentoVehiculo", "documentos_vehiculo", "Tarjetas de documentación personalizadas por vehículo."),
    @("CampoDocumento", "campos_documento", "Pares nombre-valor ordenables dentro de cada tarjeta documental."),
    @("AdjuntoDocumento", "adjuntos_documento", "Metadatos de archivos asociados a documentación, con copia local y ruta remota.")
)

$pruebas = @(
    @("Autenticación", "Registro, confirmación por correo, login, persistencia de sesión, cierre de sesión y recuperación de contraseña.", "Validado en emulador y dispositivos físicos durante el desarrollo."),
    @("Vehículos", "CRUD, fotografía, edición, borrado, orden habitual/secundario y selectores.", "Validado mediante uso funcional y sincronización entre pantallas."),
    @("Repostajes", "Alta, edición, detalle, borrado, cálculo de consumo y filtros temporales.", "Validado con datos reales de prueba y actualización de kilometraje."),
    @("Mantenimientos", "Pendientes, realizados, coste cero, componentes libres, filtros y totales.", "Validado con casos de mantenimiento y reparación."),
    @("Gastos", "Pendientes, pagados, vencimientos, renovación periódica y estados visuales.", "Validado con ITV, impuesto, parking, peajes y lavados."),
    @("Recordatorios", "Fecha, kilometraje, periodicidad y completado.", "Validado con recordatorios manuales y escenarios de vencimiento."),
    @("Documentación", "Tarjetas, campos dinámicos, reordenación, adjuntos, cámara, galería, documentos y borrado remoto.", "Validado con sincronización entre dispositivos."),
    @("Offline-first", "Creación sin red, aviso de conectividad y subida posterior.", "Validado desactivando conexión en emulador y dispositivo."),
    @("Notificaciones", "Programación de avisos mediante WorkManager.", "Pendiente de ampliar con pruebas de larga duración."),
    @("Gasolineras", "Ubicación, rango, API externa y mapa.", "En fase de estabilización funcional y revisión visual.")
)

$referencias = @(
    "Android Developers. (2026). <em>Jetpack Compose documentation</em>. https://developer.android.com/compose",
    "Android Developers. (2026). <em>Room persistence library</em>. https://developer.android.com/training/data-storage/room",
    "Android Developers. (2026). <em>WorkManager</em>. https://developer.android.com/topic/libraries/architecture/workmanager",
    "Android Developers. (2026). <em>App architecture guide</em>. https://developer.android.com/topic/architecture",
    "JetBrains. (2026). <em>Kotlin language documentation</em>. https://kotlinlang.org/docs/home.html",
    "Supabase. (2026). <em>Supabase documentation</em>. https://supabase.com/docs",
    "Supabase. (2026). <em>Row Level Security</em>. https://supabase.com/docs/guides/database/postgres/row-level-security",
    "Ktor. (2026). <em>Ktor client documentation</em>. https://ktor.io/docs/client-create-new-application.html",
    "Coil. (2026). <em>Coil image loading for Android</em>. https://coil-kt.github.io/coil/",
    "OWASP Foundation. (2026). <em>Mobile Application Security Verification Standard</em>. https://mas.owasp.org/MASVS/",
    "OpenStreetMap Foundation. (2026). <em>OpenStreetMap</em>. https://www.openstreetmap.org/",
    "Ministerio para la Transición Ecológica y el Reto Demográfico. (2026). <em>Datos públicos de estaciones de servicio y precios de carburantes</em>. https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/"
)

$html = @"
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Memoria Técnica GestCar</title>
<style>
  @page { size: A4; margin: 2.1cm 2.0cm 2.1cm 2.0cm; }
  body { font-family: Aptos, Calibri, Arial, sans-serif; color: #111111; background: #ffffff; font-size: 11pt; line-height: 1.38; }
  h1 { color: #1F4E79; font-size: 24pt; margin-top: 28pt; margin-bottom: 12pt; page-break-before: always; border-bottom: 2px solid #B7D7EA; padding-bottom: 6pt; }
  h2 { color: #1F4E79; font-size: 17pt; margin-top: 20pt; margin-bottom: 8pt; }
  h3 { color: #255E8F; font-size: 13.5pt; margin-top: 14pt; margin-bottom: 6pt; }
  h4 { color: #255E8F; font-size: 11.5pt; margin-top: 10pt; margin-bottom: 4pt; }
  p { margin: 0 0 8pt 0; text-align: justify; }
  ul { margin-top: 2pt; margin-bottom: 8pt; }
  li { margin-bottom: 3pt; }
  .cover { page-break-after: always; text-align: center; padding-top: 110pt; }
  .cover h1 { page-break-before: auto; border: none; font-size: 32pt; color: #1F4E79; margin-bottom: 18pt; }
  .cover .subtitulo { font-size: 17pt; color: #345; margin-bottom: 50pt; }
  .cover .meta { margin-top: 60pt; font-size: 11pt; color: #333; line-height: 1.6; }
  .aviso-indice { background: #EEF7F6; border-left: 6px solid #75B8A8; padding: 10pt 12pt; margin: 10pt 0 14pt 0; }
  .nota { background: #F3F8FB; border-left: 5px solid #8FC5E8; padding: 9pt 12pt; margin: 10pt 0; }
  .decision { background: #F7F2E8; border-left: 5px solid #E1BD78; padding: 9pt 12pt; margin: 10pt 0; }
  .seguridad { background: #F4F8F3; border-left: 5px solid #8CBF8B; padding: 9pt 12pt; margin: 10pt 0; }
  table { border-collapse: collapse; width: 100%; margin: 8pt 0 14pt 0; }
  .tabla th { background: #DCEEF8; color: #173B57; font-weight: bold; border: 1px solid #9CBFD6; padding: 7pt; text-align: left; }
  .tabla td { border: 1px solid #C8D8E2; padding: 7pt; vertical-align: top; }
  .tabla tr:nth-child(even) td { background: #F8FBFD; }
  .placeholder { border-collapse: collapse; width: 100%; margin: 12pt 0 18pt 0; }
  .placeholder td { border: 2px dashed #8FB2C9; background: #F7FBFD; height: 150pt; text-align: center; vertical-align: middle; padding: 18pt; }
  .placeholder-title { color: #1F4E79; font-size: 14pt; font-weight: bold; margin-bottom: 8pt; }
  .placeholder-desc { color: #415566; font-size: 10.5pt; }
  .caption { color: #555; font-size: 9.5pt; text-align: center; margin-top: -8pt; margin-bottom: 12pt; }
  .diagram { background: #FFFFFF; border: 1px solid #BFD6E5; padding: 10pt; margin: 8pt 0 16pt 0; }
  .diagram table td { border: 1px solid #BCD4E4; background: #F8FCFE; text-align: center; padding: 8pt; }
  .soft { color: #4A6675; }
  .sin-salto h1:first-of-type { page-break-before: auto; }
  code { font-family: Consolas, 'Courier New', monospace; background: #F3F5F6; padding: 1pt 3pt; }
</style>
</head>
<body>

<div class="cover">
  <h1>Memoria Técnica</h1>
  <div class="subtitulo">GestCar: aplicación Android para la gestión integral de vehículos particulares</div>
  <div class="meta">
    Trabajo de Fin de Grado<br>
    Desarrollo de Aplicaciones Multiplataforma<br>
    Autor: Diego Gallardo<br>
    Fecha: mayo de 2026<br>
    Versión del documento: borrador editable para revisión
  </div>
</div>

<p style="page-break-before: always;"></p>
<div class="aviso-indice">
  <strong>Nota para edición final.</strong> Este documento no incluye índice automático porque se insertará manualmente desde Word antes de generar el PDF final. Los títulos están estructurados con jerarquía suficiente para crear una tabla de contenido automática.
</div>

<h1>Resumen</h1>
<p>GestCar es una aplicación Android nativa desarrollada en Kotlin con Jetpack Compose cuyo objetivo es centralizar la gestión de vehículos particulares. La aplicación permite registrar vehículos, repostajes, mantenimientos, reparaciones, gastos, recordatorios, documentación y archivos asociados, manteniendo además un enfoque offline-first mediante Room y sincronización remota mediante Supabase.</p>
<p>El proyecto surge de una necesidad práctica: la información de un vehículo suele estar repartida entre papeles físicos, aplicaciones de aseguradoras, correos electrónicos, facturas, fotografías, notas personales y recuerdos del propio usuario. GestCar propone reunir esa información en una aplicación móvil sencilla, visual y orientada al uso cotidiano.</p>
<p>La solución implementa autenticación mediante correo electrónico, persistencia de sesión, recuperación de contraseña, almacenamiento local, sincronización con backend, gestión de imágenes, adjuntos documentales, notificaciones, estadísticas, exportación de datos, control de vencimientos y consulta de gasolineras cercanas con precios de carburante.</p>
<p>Desde el punto de vista técnico, el proyecto aplica una arquitectura MVVM con separación de responsabilidades entre pantallas Compose, ViewModels, repositorios, DAOs, entidades Room, DTOs remotos y utilidades. La decisión de guardar primero en local y sincronizar después permite que la aplicación siga funcionando aunque el usuario no tenga conexión en el momento de introducir un dato.</p>
<p>El resultado es una aplicación funcional avanzada, con una base extensible y orientada a resolver un caso real. La memoria documenta objetivos, análisis, diseño, arquitectura, modelo de datos, implementación, seguridad, pruebas, manual de usuario, instalación, despliegue y posibles líneas de mejora.</p>

<h1>Abstract</h1>
<p>GestCar is a native Android application built with Kotlin and Jetpack Compose. Its main purpose is to centralize the management of private vehicles, including vehicle records, fuel refills, maintenance operations, repairs, recurring expenses, reminders, documentation cards and attached files. The application follows an offline-first approach using Room as a local database and Supabase as a remote backend.</p>
<p>The project addresses a common real-life problem: vehicle-related information is usually scattered across physical documents, insurance applications, emails, invoices, photos and personal notes. GestCar provides a single mobile tool where users can store, consult and update relevant data from their own devices.</p>
<p>The system includes email authentication, session persistence, password recovery, local storage, remote synchronization, image management, document attachments, notifications, statistics, data export, due-date control and nearby gas station lookup using external fuel price data.</p>
<p>From a technical perspective, the application is structured around the MVVM pattern and separates UI, state management, repositories, local database access, remote DTO mapping and utility logic. The local-first persistence strategy improves resilience, allowing users to continue using the app even when network connectivity is temporarily unavailable.</p>

<h1>Palabras clave</h1>
<p>Android, Kotlin, Jetpack Compose, Room, Supabase, MVVM, offline-first, gestión de vehículos, sincronización, documentación digital, mantenimiento, repostajes, gastos, recordatorios.</p>

<h1>1. Introducción</h1>
<h2>1.1 Contexto del proyecto</h2>
<p>El mantenimiento de un vehículo no se limita únicamente a repostar combustible o pasar revisiones. En la práctica, un usuario puede necesitar recordar cuándo cambió el aceite, cuánto costó una reparación, cuándo vence el seguro, qué matrícula tiene un vehículo secundario, dónde guardó la póliza, qué documentación lleva asociada una moto o qué consumo real está teniendo un coche con los años.</p>
<p>La mayoría de usuarios resuelve estas necesidades de forma fragmentada. Algunas personas usan notas del móvil, otras fotografías, otras una hoja de cálculo, otras correos electrónicos, y muchas dependen directamente de la memoria. Esta dispersión aumenta el riesgo de perder información, duplicar esfuerzos o no tener datos importantes cuando se necesitan.</p>
<p>GestCar nace como una respuesta a ese problema. El proyecto no pretende sustituir a una aplicación oficial de una aseguradora ni a un sistema profesional de flotas, sino ofrecer a un usuario particular una herramienta centralizada, flexible y suficientemente potente para gestionar sus vehículos de forma ordenada.</p>

<h2>1.2 Problema detectado</h2>
<p>El problema principal es la falta de un punto único de consulta para información relevante del vehículo. Aunque existen aplicaciones especializadas en repostajes o mantenimiento, muchas se centran en un único aspecto y no integran documentación, recordatorios, gastos, fotografías y funcionamiento offline de forma coherente.</p>
<p>Además, en escenarios reales el usuario puede no tener conexión a internet. Por ejemplo, puede registrar un repostaje en una gasolinera con poca cobertura, guardar una reparación en un taller subterráneo o consultar una documentación en un momento en el que la red no funciona. Si la aplicación dependiera exclusivamente de internet, el uso quedaría limitado.</p>
<p>Por ese motivo, GestCar se ha planteado desde el inicio con una base local sólida y sincronización posterior. El dato importante se guarda primero en el dispositivo y se sube al backend cuando las condiciones de red lo permiten.</p>

<h2>1.3 Alcance</h2>
<p>El alcance del proyecto incluye el desarrollo de una aplicación Android funcional con autenticación, persistencia local, sincronización remota, gestión de vehículos, repostajes, mantenimientos, gastos, recordatorios, documentación, adjuntos, estadísticas, exportación, cuenta de usuario, avisos visuales de conectividad, notificaciones y consulta de gasolineras.</p>
<p>No se incluye en el alcance una versión iOS, una aplicación web pública, un panel administrativo completo ni un sistema de flotas profesional multiempresa. Tampoco se incluye la publicación final en Google Play, aunque la estructura del proyecto permite generar APKs de prueba y preparar una versión release en una fase posterior.</p>

<h2>1.4 Estado actual</h2>
<p>La aplicación se encuentra en un estado funcional avanzado. Las principales secciones ya están implementadas y conectadas con la arquitectura local-remota. Existen todavía áreas que pueden seguir puliéndose, especialmente en validación visual, pruebas automatizadas y estabilización de la pantalla de gasolineras, pero el núcleo funcional de la aplicación está desarrollado.</p>
"@

$html += Placeholder "Captura pendiente: pantalla de carga de GestCar" "Debe mostrar la pantalla inicial o splash antes de acceder al login o a la lista de vehículos."
$html += Placeholder "Captura pendiente: pantalla principal de vehículos" "Debe mostrar una lista de vehículos reales de prueba y el botón de creación."

$html += @"
<h1>2. Objetivos</h1>
<h2>2.1 Objetivo general</h2>
<p>Desarrollar una aplicación Android nativa que permita a usuarios particulares gestionar de forma centralizada la información relevante de sus vehículos, combinando una experiencia de uso sencilla con una arquitectura robusta basada en persistencia local, sincronización remota y medidas de seguridad adecuadas para datos personales.</p>

<h2>2.2 Objetivos específicos</h2>
"@
$html += Lista @(
    "Implementar un sistema de autenticación con registro, confirmación por correo, inicio de sesión, recuperación de contraseña y persistencia de sesión.",
    "Permitir la gestión completa de vehículos, incluyendo datos técnicos, matrícula, kilometraje, foto, notas y organización por prioridad.",
    "Registrar repostajes y calcular métricas útiles como consumo medio y coste medio de combustible.",
    "Registrar mantenimientos y reparaciones, diferenciando operaciones pendientes y realizadas.",
    "Controlar gastos únicos y periódicos, incluyendo vencimientos, pagos y renovación de avisos.",
    "Crear recordatorios por fecha, kilometraje o periodicidad.",
    "Centralizar documentación del vehículo mediante tarjetas configurables por el usuario.",
    "Permitir adjuntar fotografías y documentos a la documentación del vehículo.",
    "Mantener funcionamiento sin conexión mediante Room y sincronización posterior con Supabase.",
    "Implementar avisos de conectividad, sincronización en segundo plano y notificaciones del sistema.",
    "Ofrecer estadísticas y opciones de exportación útiles para el usuario.",
    "Incorporar una sección de gasolineras cercanas basada en ubicación y datos externos.",
    "Documentar el proyecto de forma clara, justificando decisiones técnicas y funcionales."
)

$html += @"
<h2>2.3 Objetivos personales y formativos</h2>
<p>Además de los objetivos funcionales, el proyecto persigue objetivos formativos propios de un Trabajo de Fin de Grado. Entre ellos destacan aplicar arquitectura MVVM en un proyecto real, trabajar con almacenamiento local y remoto, gestionar autenticación, diseñar interfaces con Jetpack Compose, tratar permisos del dispositivo, manejar archivos, programar tareas en segundo plano y preparar documentación técnica profesional.</p>
<p>El proyecto también permite demostrar capacidad de iteración. Durante el desarrollo se han corregido decisiones iniciales, se han simplificado flujos de usuario, se han reforzado aspectos de seguridad y se han añadido funcionalidades a partir de pruebas reales en dispositivos.</p>

<h1>3. Análisis de requisitos</h1>
<h2>3.1 Actores del sistema</h2>
<p>El sistema está diseñado principalmente para un actor: el usuario autenticado. No obstante, también se considera el usuario no autenticado durante los flujos de registro, inicio de sesión y recuperación de contraseña.</p>
"@
$html += Tabla @("Actor", "Descripción", "Acciones principales") @(
    @("Usuario no autenticado", "Persona que abre la app sin sesión activa.", "Registrarse, iniciar sesión, recuperar contraseña."),
    @("Usuario autenticado", "Persona con sesión activa y datos asociados.", "Gestionar vehículos, repostajes, gastos, mantenimientos, documentación, recordatorios, cuenta y gasolineras."),
    @("Backend", "Servicios remotos utilizados por la aplicación.", "Autenticación, persistencia remota, almacenamiento de archivos y seguridad por usuario."),
    @("Sistema Android", "Sistema operativo del dispositivo.", "Permisos, cámara, galería, documentos, notificaciones, conectividad y tareas en segundo plano.")
)

$html += @"
<h2>3.2 Requisitos funcionales</h2>
"@
$html += Tabla @("Código", "Requisito", "Descripción") $requisitosFuncionales
$html += @"
<h2>3.3 Requisitos no funcionales</h2>
"@
$html += Tabla @("Código", "Requisito", "Descripción") $requisitosNoFuncionales

$html += @"
<h2>3.4 Casos de uso principales</h2>
"@
$html += Tabla @("Código", "Caso de uso", "Actor", "Resumen") $casosUso
$html += @"
<div class="diagram">
  <strong>Diagrama conceptual de casos de uso.</strong>
  <table>
    <tr><td>Usuario no autenticado</td><td>Registro<br>Inicio de sesión<br>Recuperación de contraseña</td></tr>
    <tr><td>Usuario autenticado</td><td>Vehículos<br>Repostajes<br>Mantenimientos<br>Gastos<br>Recordatorios<br>Documentación<br>Estadísticas<br>Cuenta<br>Gasolineras</td></tr>
    <tr><td>Sistema Android</td><td>Cámara<br>Galería<br>Gestor de documentos<br>Notificaciones<br>Ubicación<br>Conectividad</td></tr>
    <tr><td>Backend</td><td>Autenticación<br>Base de datos remota<br>Storage privado<br>Políticas de acceso</td></tr>
  </table>
</div>
"@
$html += Placeholder "Diagrama pendiente: casos de uso en formato gráfico" "Sustituir por un diagrama UML de casos de uso si se desea presentar visualmente."

$html += @"
<h1>4. Tecnologías utilizadas y justificación</h1>
<h2>4.1 Kotlin</h2>
<p>Kotlin se ha elegido por ser el lenguaje recomendado para desarrollo Android moderno. Permite escribir código conciso, seguro frente a nulos, compatible con corrutinas y con una integración excelente con Jetpack Compose, Room y el ecosistema Android.</p>
<p>En el proyecto se utiliza Kotlin tanto para la lógica de negocio como para las pantallas, ViewModels, repositorios, entidades, DTOs y utilidades. La elección evita mezclar Java y Kotlin y permite mantener un estilo uniforme.</p>

<h2>4.2 Jetpack Compose</h2>
<p>Jetpack Compose se ha utilizado como framework de interfaz. La principal ventaja es que permite construir pantallas declarativas, basadas en estado, con menos código repetitivo que el sistema tradicional de vistas XML.</p>
<p>Compose encaja especialmente bien con StateFlow y ViewModel. Las pantallas observan el estado y se recomponen cuando cambian los datos. Esto resulta útil en una aplicación con datos locales reactivos, formularios y múltiples listas.</p>

<h2>4.3 Material Design 3</h2>
<p>Material Design 3 se utiliza como base visual. La aplicación no adopta una plantilla genérica sin personalidad, sino una paleta propia basada en azul oscuro, azul claro, tonos suaves para tarjetas y colores calmados para estados visuales.</p>
<p>La elección de colores evita transmitir connotaciones negativas innecesarias. Por ejemplo, en mantenimientos y reparaciones se han utilizado colores diferenciados sin asociar una categoría a rojo o verde, ya que ambas son acciones normales dentro del mantenimiento de un vehículo.</p>

<h2>4.4 Room</h2>
<p>Room se utiliza como base de datos local. Esta decisión es clave porque GestCar no debe depender completamente de la conexión. Room permite guardar vehículos, repostajes, gastos, mantenimientos, recordatorios, documentación y adjuntos localmente.</p>
<p>El uso de Room aporta además consultas reactivas mediante Flow, migraciones controladas y una capa DAO clara. La base de datos local se encuentra en versión 10, reflejando la evolución funcional del proyecto.</p>

<h2>4.5 Supabase</h2>
<p>Supabase se utiliza como backend remoto para autenticación, base de datos y almacenamiento. Ofrece una solución adecuada para un proyecto académico con presupuesto cero, ya que proporciona autenticación, PostgREST, base de datos PostgreSQL, Storage y Row Level Security.</p>
<p>En la app solo se utiliza configuración pública permitida para clientes móviles. Las claves privilegiadas no forman parte del APK ni del repositorio público.</p>

<h2>4.6 WorkManager</h2>
<p>WorkManager se utiliza para tareas en segundo plano, principalmente sincronización y notificaciones de vencimientos. Es una herramienta adecuada cuando no se necesita ejecutar una tarea en un segundo exacto, sino garantizar que se ejecute de forma razonable respetando batería, conectividad y decisiones del sistema Android.</p>

<h2>4.7 Coil</h2>
<p>Coil se utiliza para cargar imágenes en Compose. Es una librería ligera y moderna, apropiada para mostrar fotografías de vehículos y previsualizaciones de adjuntos sin cargar manualmente bitmaps en cada pantalla.</p>

<h2>4.8 Ktor y Kotlin Serialization</h2>
<p>Ktor se utiliza como cliente HTTP en el ecosistema Supabase y para integraciones externas. Kotlin Serialization permite mapear DTOs entre Kotlin y formatos remotos, utilizando anotaciones para transformar nombres de campos entre camelCase y snake_case.</p>

<h2>4.9 Servicios externos de correo</h2>
<p>Para correos transaccionales se configuró un proveedor SMTP externo. Esta decisión evita depender de los límites restrictivos del servicio de correo integrado del backend y permite gestionar confirmaciones de cuenta y recuperación de contraseña de forma más estable.</p>

<h2>4.10 Datos públicos de gasolineras</h2>
<p>La sección de gasolineras se plantea usando datos públicos de estaciones de servicio y precios de carburantes. El objetivo es ofrecer información útil sin depender de APIs de pago ni de servicios que puedan generar costes inesperados.</p>

<h1>5. Arquitectura del sistema</h1>
<h2>5.1 Visión general</h2>
<p>La arquitectura del proyecto sigue el patrón MVVM. Esta separación permite que las pantallas se centren en la interfaz, los ViewModels gestionen estado y validaciones, los repositorios coordinen Room y Supabase, y las entidades/DTOs representen los datos en cada capa.</p>
<div class="diagram">
  <strong>Diagrama de arquitectura por capas.</strong>
  <table>
    <tr><td>Pantallas Compose</td><td>Componentes reutilizables</td><td>Navegación</td></tr>
    <tr><td colspan="3">ViewModels con StateFlow y corrutinas</td></tr>
    <tr><td colspan="3">Repositorios offline-first</td></tr>
    <tr><td>DAOs Room</td><td>DTOs remotos</td><td>Utilidades</td></tr>
    <tr><td>Base de datos local</td><td>Supabase PostgREST</td><td>Supabase Storage / Auth</td></tr>
  </table>
</div>

<h2>5.2 Capa de interfaz</h2>
<p>La capa de interfaz se encuentra principalmente dentro de <code>ui/pantallas</code> y <code>ui/componentes</code>. Las pantallas son Composables que representan vistas completas: lista de vehículos, formulario de repostaje, detalle de mantenimiento, pantalla de cuenta, documentación, gasolineras, etc.</p>
<p>Los componentes reutilizables permiten evitar duplicación. Entre ellos destacan el selector de vehículo activo, campo de fecha, tarjetas de cada tipo de entidad, barra superior compacta, filtro de periodo y componentes de imagen.</p>

<h2>5.3 Capa ViewModel</h2>
<p>Los ViewModels se ubican en <code>ui/viewmodel</code>. Cada sección importante dispone de su propio ViewModel. Esta capa expone estados mediante StateFlow, recibe eventos de usuario, valida formularios y lanza operaciones en corrutinas.</p>
<p>El uso de ViewModel evita que una pantalla Compose tenga lógica de persistencia directa. Esto facilita pruebas, mantenimiento y futuras ampliaciones.</p>

<h2>5.4 Capa de repositorios</h2>
<p>Los repositorios están en <code>datos/repositorio</code>. Su papel es coordinar la base local y el backend remoto. La regla general del proyecto es guardar primero en Room, actualizar la interfaz y después intentar sincronizar con Supabase.</p>
<p>Este patrón reduce el riesgo de pérdida de datos cuando no hay conexión. Si falla la subida, el dato queda localmente disponible y puede sincronizarse más adelante.</p>

<h2>5.5 Capa local Room</h2>
<p>Room contiene entidades y DAOs. Las entidades representan tablas locales y los DAOs encapsulan consultas. La base de datos principal es <code>GestCarBaseDatos</code>, que registra todas las entidades y las migraciones necesarias.</p>

<h2>5.6 Capa remota</h2>
<p>La capa remota incluye DTOs y cliente de Supabase. Los DTOs permiten separar el modelo local del formato remoto, especialmente cuando los nombres de campos difieren entre Kotlin y PostgreSQL.</p>

<h2>5.7 Flujo de sincronización</h2>
<p>El flujo general de sincronización sigue una estrategia offline-first. Cuando el usuario guarda un dato, la app lo escribe en Room. Después intenta enviarlo al backend. Si no puede, el registro conserva su marca temporal y podrá reintentarse.</p>
<div class="diagram">
  <strong>Secuencia conceptual de guardado offline-first.</strong>
  <table>
    <tr><td>1</td><td>Usuario rellena formulario</td></tr>
    <tr><td>2</td><td>ViewModel valida campos</td></tr>
    <tr><td>3</td><td>Repositorio guarda en Room</td></tr>
    <tr><td>4</td><td>UI se actualiza desde Flow local</td></tr>
    <tr><td>5</td><td>Repositorio intenta upsert remoto</td></tr>
    <tr><td>6</td><td>Si falla, WorkManager reintentará posteriormente</td></tr>
  </table>
</div>

<h1>6. Modelo de datos</h1>
<h2>6.1 Entidades principales</h2>
"@
$html += Tabla @("Entidad", "Tabla local", "Responsabilidad") $entidades

$html += @"
<h2>6.2 Relaciones</h2>
<p>La entidad central es el vehículo. Repostajes, mantenimientos, gastos, recordatorios y documentación dependen de un vehículo mediante clave foránea. A su vez, una tarjeta de documentación puede tener múltiples campos personalizados y múltiples adjuntos.</p>
<div class="diagram">
  <strong>Diagrama conceptual de relaciones.</strong>
  <table>
    <tr><td rowspan="5">Vehiculo</td><td>1 : N</td><td>Repostaje</td></tr>
    <tr><td>1 : N</td><td>Mantenimiento</td></tr>
    <tr><td>1 : N</td><td>GastoPeriodico</td></tr>
    <tr><td>1 : N</td><td>Recordatorio</td></tr>
    <tr><td>1 : N</td><td>DocumentoVehiculo</td></tr>
    <tr><td>DocumentoVehiculo</td><td>1 : N</td><td>CampoDocumento</td></tr>
    <tr><td>DocumentoVehiculo</td><td>1 : N</td><td>AdjuntoDocumento</td></tr>
  </table>
</div>

<h2>6.3 Migraciones</h2>
<p>La base de datos local se encuentra en versión 10. Las migraciones reflejan la evolución del proyecto: cambio de año de fabricación a fecha parcial, incorporación de repostajes, mantenimientos, gastos, recordatorios, estados de pago, orden de vehículos, periodicidad de recordatorios, documentación flexible y adjuntos.</p>
"@
$html += Tabla @("Migración", "Cambio principal", "Justificación") @(
    @("1 → 2", "Reestructuración de fecha de fabricación del vehículo.", "Permitir año obligatorio y mes/día opcionales."),
    @("2 → 3", "Creación de repostajes, mantenimientos, gastos y recordatorios.", "Ampliar la app más allá del CRUD de vehículos."),
    @("3 → 4", "Campos de pagado y fecha de pago en gastos.", "Diferenciar gastos pendientes e históricos."),
    @("4 → 5", "Campos de realizado y fecha realizada en mantenimientos.", "Permitir checklist de reparaciones y mantenimientos."),
    @("5 → 6", "Campos habitual y ordenLista en vehículos.", "Priorizar vehículos habituales y mejorar selectores."),
    @("6 → 7", "Periodicidad en recordatorios.", "Crear avisos repetibles por tiempo y kilómetros."),
    @("7 → 8", "Documentación flexible.", "Permitir tarjetas documentales configurables."),
    @("8 → 9", "Adjuntos documentales.", "Asociar archivos a la documentación."),
    @("9 → 10", "Ruta remota y fecha de sincronización en adjuntos.", "Sincronizar archivos con almacenamiento remoto sin perder copia local.")
)

$html += @"
<h2>6.4 Identificadores y fechas</h2>
<p>Los identificadores se gestionan como cadenas UUID. Esta decisión facilita la compatibilidad entre Room y Supabase, evita depender de autoincrementos locales y reduce conflictos de sincronización.</p>
<p>Las fechas se almacenan como valores Long en milisegundos desde epoch. Este formato simplifica comparaciones, filtros temporales, cálculos de vencimiento y serialización entre capas.</p>

<h1>7. Diseño de interfaz y experiencia de usuario</h1>
<h2>7.1 Principios visuales</h2>
<p>La interfaz busca ser clara, tranquila y práctica. Se utilizan tarjetas, formularios simples, botones flotantes y una barra inferior con iconos. La app prioriza lectura rápida y acciones directas.</p>
<p>La paleta evita saturar al usuario. El azul se utiliza como color principal, los tonos suaves como fondo de tarjetas y colores diferenciados para estados o categorías sin connotaciones agresivas.</p>

<h2>7.2 Navegación principal</h2>
<p>La navegación principal se organiza mediante una barra inferior con cinco secciones: vehículos, gastos, repostajes, mantenimiento y más. Esta decisión deja a mano las operaciones principales sin esconderlas en menús profundos.</p>
"@
$html += Placeholder "Captura pendiente: barra inferior con iconos principales" "Debe mostrar las cinco secciones principales de la aplicación."

$html += @"
<h2>7.3 Barras superiores</h2>
<p>Las pantallas utilizan una barra superior compacta con título y acciones contextuales. En pantallas secundarias se incluye flecha de retroceso para no depender únicamente de la navegación del sistema.</p>

<h2>7.4 Selectores de vehículo</h2>
<p>Las pantallas relacionadas con datos de vehículo incluyen un selector de vehículo activo. El orden de ese selector respeta la organización definida por el usuario en la pantalla de vehículos, priorizando los vehículos habituales.</p>
"@
$html += Placeholder "Captura pendiente: selector de vehículo activo" "Debe mostrar un vehículo seleccionado y desplegable de cambio."

$html += @"
<h2>7.5 Formularios</h2>
<p>Los formularios utilizan campos claros y validaciones visuales. Los campos obligatorios se señalan con asterisco, y cuando falta información se resalta el campo correspondiente. Los campos numéricos se han ajustado para aceptar coma o punto decimal según la costumbre del usuario.</p>

<h2>7.6 Placeholder de capturas</h2>
<p>Las capturas reales deben incorporarse antes de la entrega final. En esta memoria se han dejado cajas de una sola celda para indicar dónde debe ir cada evidencia visual.</p>

<h1>8. Desarrollo funcional</h1>
<h2>8.1 Autenticación</h2>
<p>La autenticación permite registrar usuarios mediante correo electrónico, confirmar la cuenta, iniciar sesión, persistir sesión y recuperar contraseña. El flujo de recuperación utiliza deep links para devolver al usuario a la aplicación tras abrir el enlace del correo.</p>
<p>Se prestó especial atención a los campos de contraseña. Estos campos se configuran para evitar sugerencias predictivas inseguras y mantener un comportamiento coherente entre login, registro y cambio de contraseña.</p>
"@
$html += Placeholder "Captura pendiente: pantalla de inicio de sesión" "Debe mostrar correo, contraseña, recuperación de contraseña y enlace de registro."
$html += Placeholder "Captura pendiente: pantalla de registro" "Debe mostrar campos de correo, contraseña, confirmación y mensaje de revisión de correo."
$html += Placeholder "Captura pendiente: pantalla de restablecimiento de contraseña" "Debe mostrar campos protegidos y acción de guardar nueva contraseña."

$html += @"
<h2>8.2 Vehículos</h2>
<p>La sección de vehículos permite registrar los datos básicos del vehículo. Se admiten fecha de fabricación completa o parcial, siendo obligatorio únicamente el año. La matrícula se normaliza con un enfoque flexible para no bloquear al usuario ante formatos antiguos o poco habituales.</p>
<p>Además, el usuario puede añadir una foto del vehículo. Si no se añade foto, la app mantiene iconos por defecto. Las imágenes se comprimen, se guardan localmente y se suben al storage remoto cuando es posible.</p>
"@
$html += Placeholder "Captura pendiente: formulario de vehículo" "Debe mostrar campos obligatorios, opcionales, fecha de fabricación y validaciones."
$html += Placeholder "Captura pendiente: detalle de vehículo con fotografía" "Debe mostrar foto, matrícula, datos técnicos y resumen de actividad."
$html += Placeholder "Captura pendiente: organización de vehículos habituales y otros vehículos" "Debe mostrar modo de edición con arrastre y secciones."

$html += @"
<h2>8.3 Repostajes</h2>
<p>Los repostajes permiten calcular el consumo real del vehículo. El usuario introduce kilometraje, litros, precio por litro, gasolinera y notas. La aplicación calcula importes y métricas como consumo medio y coste medio por cada cien kilómetros.</p>
<p>La pantalla admite filtros temporales para consultar únicamente repostajes de un periodo. Esto evita que el histórico completo oculte la información más reciente.</p>
"@
$html += Placeholder "Captura pendiente: lista de repostajes con métricas" "Debe mostrar consumo medio, coste medio, tarjetas de repostaje y selector de periodo."
$html += Placeholder "Captura pendiente: formulario de repostaje" "Debe mostrar fecha, kilómetros, litros, precio por litro, importe y notas."
$html += Placeholder "Captura pendiente: detalle de repostaje" "Debe mostrar todos los datos guardados antes de editar."

$html += @"
<h2>8.4 Mantenimientos y reparaciones</h2>
<p>La pantalla de mantenimiento separa operaciones pendientes y realizadas. También diferencia mantenimientos y reparaciones, pero sin forzar una lista cerrada de componentes. El usuario puede escribir libremente el componente o la operación.</p>
<p>El coste no se limita a valores positivos estrictos porque existen reparaciones gratuitas por garantía, campañas de fabricante o cobertura de seguros. El coste cero es por tanto un caso válido.</p>
"@
$html += Placeholder "Captura pendiente: lista de mantenimientos con filtros" "Debe mostrar pendientes, realizadas, chip de mantenimiento/reparación y total económico."
$html += Placeholder "Captura pendiente: formulario de mantenimiento" "Debe mostrar tipo o componente libre, estado, categoría, fecha, kilómetros, coste, taller y descripción."
$html += Placeholder "Captura pendiente: detalle de mantenimiento" "Debe mostrar información completa y acción para marcar como realizada si procede."

$html += @"
<h2>8.5 Gastos</h2>
<p>Los gastos cubren pagos no necesariamente mecánicos: seguro, ITV, impuesto, parking, peajes, lavados y otros conceptos. La app diferencia pendientes y pagados, y utiliza la fecha de vencimiento para reflejar visualmente si un gasto está vencido, próximo o al día.</p>
<p>Cuando un gasto periódico se marca como pagado, la aplicación puede proponer la creación del siguiente aviso con la misma periodicidad. Esta lógica evita que el usuario tenga que crear manualmente un gasto nuevo cada año o mes.</p>
"@
$html += Placeholder "Captura pendiente: lista de gastos pendientes y pagados" "Debe mostrar estados visuales, sección pagados plegable y gastos periódicos."
$html += Placeholder "Captura pendiente: detalle de gasto" "Debe mostrar datos del gasto y botón de marcar como pagado."
$html += Placeholder "Captura pendiente: modal de creación del siguiente gasto" "Debe mostrar confirmación para generar el siguiente vencimiento."

$html += @"
<h2>8.6 Recordatorios</h2>
<p>Los recordatorios permiten avisar al usuario por fecha, kilometraje o una combinación de ambos. La interfaz se simplificó para que el usuario no tenga que gestionar demasiados interruptores. Si configura periodicidad, la app interpreta el valor como repetición; si no, lo trata como aviso puntual.</p>
<p>Esta flexibilidad permite casos como cambio de aceite cada cierto número de kilómetros, revisión de neumáticos cada cierto tiempo o inspecciones que dependen de fecha y kilometraje.</p>
"@
$html += Placeholder "Captura pendiente: lista de recordatorios" "Debe mostrar pendientes, completados y estados por fecha/kilometraje."
$html += Placeholder "Captura pendiente: formulario simplificado de recordatorio" "Debe mostrar fecha límite, kilometraje límite, periodicidad y comentarios."
$html += Placeholder "Captura pendiente: detalle de recordatorio" "Debe mostrar datos y acción de completar."

$html += @"
<h2>8.7 Estadísticas</h2>
<p>La sección de estadísticas resume información económica y de uso. El objetivo no es solo mostrar números, sino convertir los registros diarios en información útil: consumo, coste medio, coste acumulado, coste por kilómetro y operaciones realizadas.</p>
"@
$html += Placeholder "Captura pendiente: pantalla de estadísticas" "Debe mostrar tarjetas de indicadores, filtros y gráficos o resúmenes visuales."

$html += @"
<h2>8.8 Exportación de datos</h2>
<p>La exportación permite sacar información fuera de la aplicación. Se plantean formatos útiles para usuario final como CSV, Excel, PDF y texto compartible. El texto compartible está pensado para enviar por WhatsApp o similar un resumen breve de un vehículo.</p>
"@
$html += Placeholder "Captura pendiente: modal de exportación" "Debe mostrar opciones CSV, PDF, Excel y texto compartible."

$html += @"
<h2>8.9 Documentación del vehículo</h2>
<p>La documentación es una de las secciones más diferenciales del proyecto. En lugar de imponer tipos cerrados de documentos, la app permite crear tarjetas personalizadas. Cada tarjeta puede tener un título, notas, campos configurables y adjuntos.</p>
<p>Por ejemplo, el usuario puede crear una tarjeta llamada Seguro y añadir campos como aseguradora, número de póliza, teléfono, coberturas o cualquier otro dato relevante. También puede crear tarjetas para permiso de circulación, certificado histórico, parking, documentación de empresa, accesorios o revisiones específicas.</p>
<p>Los campos personalizados se pueden reordenar mediante interacción táctil. Esto permite que el usuario adapte la presentación a su propio criterio sin depender de un orden fijo.</p>
"@
$html += Placeholder "Captura pendiente: lista de documentación" "Debe mostrar tarjetas documentales por vehículo."
$html += Placeholder "Captura pendiente: detalle de documentación" "Debe mostrar campos personalizados, notas y archivos asociados."
$html += Placeholder "Captura pendiente: edición de documentación con reordenación" "Debe mostrar campos arrastrables y orden personalizados."
$html += Placeholder "Captura pendiente: adjuntos de documentación" "Debe mostrar previsualización de imagen o documento, similar a una tarjeta de archivo."

$html += @"
<h2>8.10 Cuenta de usuario</h2>
<p>La pantalla de cuenta muestra el correo electrónico y acciones relevantes: cerrar sesión, cambiar contraseña y eliminar cuenta. Por seguridad no se muestran identificadores internos del backend ni información que no aporte valor al usuario final.</p>
<p>La eliminación de cuenta requiere doble confirmación. La primera ventana explica el alcance de la acción y la segunda solicita confirmación final para evitar borrados accidentales.</p>
"@
$html += Placeholder "Captura pendiente: pantalla de cuenta" "Debe mostrar correo, cerrar sesión, cambio de contraseña y eliminar cuenta."
$html += Placeholder "Captura pendiente: doble confirmación de eliminación de cuenta" "Debe mostrar advertencia de borrado de datos asociados."

$html += @"
<h2>8.11 Gasolineras</h2>
<p>La pantalla de gasolineras se plantea como una mejora de valor para el usuario. Permite consultar estaciones de servicio cercanas y precios de carburante dentro de un radio seleccionado. La navegación final se delega en Google Maps, ya que GestCar informa, pero no pretende convertirse en una aplicación de navegación.</p>
<p>Esta funcionalidad utiliza ubicación del dispositivo, datos externos y representación visual en mapa. Por su naturaleza, es una de las partes más sensibles a permisos, conectividad y disponibilidad del servicio externo.</p>
"@
$html += Placeholder "Captura pendiente: pantalla de gasolineras con mapa" "Debe mostrar mapa, rango de búsqueda, ubicación y puntos de estaciones."
$html += Placeholder "Captura pendiente: detalle de gasolinera" "Debe mostrar nombre, dirección, precios disponibles y botón Ir."

$html += @"
<h1>9. Seguridad y privacidad</h1>
<h2>9.1 Datos tratados</h2>
<p>GestCar puede almacenar información sensible: matrículas, fotografías, historial de uso, gastos, mantenimientos, documentación, adjuntos y datos de cuenta. Aunque no se trata de una aplicación bancaria, el conjunto de datos puede revelar hábitos, posesiones, direcciones indirectas o documentación personal.</p>
<p>Por ese motivo, la seguridad no se considera un añadido, sino una parte central del diseño.</p>

<h2>9.2 Separación por usuario</h2>
<p>Los datos remotos se asocian al usuario autenticado. Las políticas de acceso del backend impiden que un usuario consulte o modifique registros que no le pertenecen. En la app, los registros también incorporan identificadores de usuario o relaciones indirectas mediante vehículo.</p>

<h2>9.3 Storage privado</h2>
<p>Las imágenes y adjuntos se tratan mediante almacenamiento privado. La app no depende de buckets públicos para exponer ficheros sensibles. Cuando necesita acceder a un archivo remoto, se utiliza una ruta controlada y mecanismos temporales o autorizados según la configuración del backend.</p>

<h2>9.4 Credenciales y repositorio</h2>
<p>El repositorio público no debe contener credenciales, tokens, contraseñas, claves privilegiadas, scripts sensibles ni datos reales de usuarios. La configuración privada debe gestionarse fuera de la documentación pública.</p>

<h2>9.5 Eliminación de cuenta</h2>
<p>La eliminación de cuenta se plantea como una operación protegida en backend. Desde la interfaz se solicita doble confirmación porque la acción afecta a datos personales y registros asociados a vehículos.</p>

<h2>9.6 Riesgos mitigados</h2>
"@
$html += Tabla @("Riesgo", "Medida aplicada", "Resultado esperado") @(
    @("Acceso cruzado entre usuarios", "Row Level Security y asociación por usuario.", "Cada usuario accede únicamente a sus datos."),
    @("Exposición de archivos", "Storage privado y rutas controladas.", "Los adjuntos no quedan públicamente listables."),
    @("Pérdida de datos sin conexión", "Room y sincronización posterior.", "El usuario conserva datos locales."),
    @("Contraseñas visibles o sugeridas", "Campos protegidos y sin texto predictivo.", "Menor riesgo de exposición en pantalla o teclado."),
    @("Borrado accidental", "Doble confirmación para eliminar cuenta.", "Menor probabilidad de pérdida irreversible."),
    @("Secretos en GitHub", "README sin claves ni SQL sensible.", "Menor superficie de exposición pública.")
)

$html += @"
<h1>10. Funcionamiento offline y sincronización</h1>
<h2>10.1 Enfoque offline-first</h2>
<p>El enfoque offline-first significa que la aplicación no espera a que el backend confirme una operación para reflejarla localmente. Primero se guarda en Room, después se intenta sincronizar. Esta decisión mejora la experiencia de usuario y protege datos en situaciones sin cobertura.</p>

<h2>10.2 Sincronización automática</h2>
<p>La sincronización se activa en diferentes momentos: al abrir pantallas, al recuperar conexión, al guardar cambios y mediante tareas programadas. El objetivo es reducir inconsistencias entre dispositivos sin obligar al usuario a pulsar un botón manual.</p>

<h2>10.3 Banner de conectividad</h2>
<p>La aplicación muestra avisos cuando no hay conexión y cuando se recupera. Estos avisos aparecen bajo la barra superior para no interrumpir el trabajo, pero informan claramente del estado.</p>
"@
$html += Placeholder "Captura pendiente: banner No hay conexión" "Debe mostrar aviso gris oscuro centrado bajo la barra superior."
$html += Placeholder "Captura pendiente: banner Vuelves a tener conexión" "Debe mostrar aviso verde centrado bajo la barra superior."

$html += @"
<h2>10.4 Tareas en segundo plano</h2>
<p>WorkManager permite ejecutar sincronizaciones y avisos respetando restricciones de Android. Esto es importante porque los usuarios no mantienen la app abierta constantemente. Si registran un dato sin conexión y cierran la app, una tarea posterior puede intentar sincronizarlo cuando el sistema lo permita.</p>

<h1>11. Notificaciones</h1>
<h2>11.1 Objetivo</h2>
<p>Las notificaciones se usan para avisar de vencimientos y recordatorios importantes. La intención no es saturar al usuario, sino ayudarle a no olvidar gastos o tareas relevantes.</p>

<h2>11.2 Estrategia de avisos</h2>
<p>Para gastos con vencimiento se plantea aviso con antelación, aviso intermedio y aviso durante la semana del vencimiento. El sistema puede ajustar la hora exacta de ejecución según las políticas de Android, por lo que no se fuerza una hora exacta crítica.</p>
"@
$html += Tabla @("Situación", "Mensaje orientativo", "Justificación") @(
    @("30 días antes", "Tienes un gasto próximo a vencer.", "Primer aviso tranquilo, sin urgencia."),
    @("15 días antes", "El gasto con nombre X vence en 15 días.", "Recordatorio intermedio."),
    @("Semana del vencimiento", "El gasto con nombre X vence esta semana.", "Aviso más visible sin repetir fecha innecesariamente."),
    @("Último día", "El gasto con nombre X vence hoy.", "Aviso final para evitar descuido."),
    @("Recordatorio por kilometraje", "Revisa el recordatorio X.", "Depende del kilometraje registrado del vehículo.")
)

$html += @"
<h1>12. Validaciones y usabilidad</h1>
<h2>12.1 Formularios</h2>
<p>Los formularios validan campos obligatorios y resaltan errores en rojo cuando falta información. Se evita mostrar textos largos como “obligatorio” u “opcional” en cada etiqueta, utilizando asteriscos en los campos necesarios y mensajes de advertencia al guardar.</p>

<h2>12.2 Entradas numéricas</h2>
<p>Los importes y precios aceptan coma o punto decimal. Esta decisión mejora la usabilidad en España, donde es frecuente escribir decimales con coma. La app normaliza internamente el valor para calcular y guardar correctamente.</p>

<h2>12.3 Matrículas</h2>
<p>La normalización de matrículas se diseñó con flexibilidad. En España conviven formatos modernos, formatos provinciales antiguos y formatos especiales. La aplicación evita bloquear al usuario ante casos raros, pero puede advertir si el formato no parece reconocible.</p>

<h2>12.4 Navegación</h2>
<p>La navegación se reforzó para evitar pantallas sin salida. Las pantallas secundarias incluyen flecha de retroceso y los accesos desde tarjetas conservan el contexto del vehículo.</p>

<h1>13. Pruebas y validación</h1>
<h2>13.1 Estrategia de pruebas</h2>
<p>La validación se ha realizado principalmente mediante pruebas funcionales manuales en emuladores y dispositivos físicos. Se han probado flujos completos, sincronización entre dispositivos, uso offline, formularios, imágenes, adjuntos, navegación y autenticación.</p>
"@
$html += Tabla @("Módulo", "Pruebas realizadas", "Estado") $pruebas

$html += @"
<h2>13.2 Dispositivos y entornos utilizados</h2>
"@
$html += Tabla @("Entorno", "Sistema", "Resultado") @(
    @("PC Intel", "Windows 10, Android Studio Otter 3 Feature Drop 2025.2.3", "Proyecto clonado y ejecutado correctamente en emulador."),
    @("PC AMD", "Windows 11, Android Studio Panda 3 2025.3.3", "Proyecto clonado y ejecutado correctamente."),
    @("Máquina virtual AMD", "Windows 11, Android Studio Panda 4 2025.3.4", "Proyecto descargado y abierto correctamente."),
    @("BlueStacks", "BlueStacks 5.22.168.1001", "APK instalado y ejecutado."),
    @("MuMu Player", "Versión 5.27.2.3530", "APK instalado y ejecutado."),
    @("Xiaomi Redmi 7", "Android 10", "APK instalado y ejecutado."),
    @("Samsung A20e", "Android 11", "APK instalado y ejecutado."),
    @("Xiaomi Poco F5", "Android 13", "APK instalado y ejecutado."),
    @("Xiaomi 11T Pro", "Android 14", "APK instalado y ejecutado.")
)

$html += Placeholder "Captura pendiente: evidencia de ejecución en emulador" "Añadir captura de Android Studio con la app abierta."
$html += Placeholder "Captura pendiente: evidencia de ejecución en dispositivo físico" "Añadir captura del móvil con GestCar funcionando."
$html += Placeholder "Captura pendiente: evidencia de sincronización entre dispositivos" "Mostrar el mismo dato creado en un dispositivo y consultado en otro."

$html += @"
<h2>13.3 Pruebas offline</h2>
<p>Se han realizado pruebas desactivando la red del dispositivo, creando registros y recuperando conexión posteriormente. El objetivo es verificar que los datos no desaparecen si Supabase no está disponible en el momento de guardar.</p>

<h2>13.4 Pruebas de seguridad funcional</h2>
<p>Se revisó que la aplicación no mostrara identificadores internos de usuario, que las contraseñas no usaran sugerencias predictivas, que la eliminación de cuenta requiriera doble confirmación y que los adjuntos se trataran como datos sensibles.</p>

<h2>13.5 Limitaciones de prueba</h2>
<p>No se dispone todavía de una batería completa de tests automatizados. La validación manual ha cubierto muchos escenarios reales, pero como mejora futura sería recomendable incorporar pruebas unitarias sobre repositorios, normalizadores, cálculos de estadísticas y validadores de formularios.</p>

<h1>14. Manual de instalación y despliegue</h1>
<h2>14.1 Instalación como usuario final mediante APK</h2>
<p>La forma más sencilla de probar GestCar es instalar un APK generado desde el proyecto. El repositorio de GitHub contiene código fuente, pero no es directamente una aplicación instalable en un teléfono.</p>
"@
$html += Lista @(
    "Recibir el archivo APK generado por el desarrollador.",
    "Copiarlo al dispositivo Android mediante cable, Drive, correo, mensajería o gestor de archivos.",
    "Abrir el APK desde el dispositivo.",
    "Permitir instalación desde la fuente utilizada si Android lo solicita.",
    "Instalar GestCar.",
    "Abrir la aplicación.",
    "Registrarse o iniciar sesión.",
    "Confirmar el correo electrónico si se está usando una cuenta nueva.",
    "Crear un primer vehículo para comenzar a usar las secciones funcionales."
)

$html += @"
<h2>14.2 Instalación desde Android Studio</h2>
<p>Esta opción está pensada para revisión técnica o desarrollo. Permite compilar el proyecto desde el código fuente.</p>
"@
$html += Lista @(
    "Instalar Android Studio actualizado.",
    "Instalar el SDK de Android requerido.",
    "Descargar o clonar el repositorio.",
    "Abrir la carpeta raíz del proyecto desde Android Studio.",
    "Esperar a que Gradle sincronice dependencias.",
    "Seleccionar el módulo app.",
    "Crear o iniciar un emulador Android.",
    "Ejecutar la aplicación desde el botón Run.",
    "Si se usa un dispositivo físico, activar depuración USB y aceptar la autorización RSA."
)

$html += @"
<h2>14.3 Generación de APK debug</h2>
<p>Desde Android Studio puede generarse un APK debug mediante la opción de menú Build &gt; Build Bundle(s) / APK(s) &gt; Build APK(s). También puede generarse desde terminal ejecutando Gradle.</p>
<div class="nota">Ruta habitual del APK debug: <code>app/build/outputs/apk/debug/</code></div>

<h2>14.4 Configuración privada</h2>
<p>Para conectar la aplicación con servicios externos reales es necesario disponer de configuración privada del backend, autenticación, storage, políticas y SMTP. Esos valores no deben incluirse en repositorios públicos ni en anexos entregables si pueden comprometer la seguridad.</p>

<h2>14.5 Problemas frecuentes</h2>
"@
$html += Tabla @("Problema", "Causa probable", "Solución recomendada") @(
    @("La app no inicia sesión", "Correo sin confirmar, contraseña incorrecta, red desactivada o backend pausado.", "Comprobar correo, contraseña, conexión y estado del proyecto remoto."),
    @("El APK no instala", "APK release sin firma válida o archivo dañado.", "Usar APK debug generado correctamente o firmar release."),
    @("No llegan correos", "SMTP no configurado o límite del proveedor.", "Revisar proveedor SMTP y panel de autenticación."),
    @("No se ven imágenes", "Sin red y sin copia local, storage no accesible o ruta desactualizada.", "Comprobar copia local, sincronización y permisos de storage."),
    @("Gradle no sincroniza", "Dependencias sin descargar o SDK incorrecto.", "Actualizar SDK, revisar conexión y limpiar proyecto."),
    @("Mapa en blanco", "Problema de WebView, tiles, permisos o red.", "Revisar diagnóstico de la pantalla, permisos y Logcat.")
)

$html += @"
<h1>15. Manual de usuario</h1>
<h2>15.1 Primer uso</h2>
<p>Al abrir GestCar por primera vez, el usuario verá la pantalla de inicio de sesión. Si todavía no tiene cuenta, debe acceder a registro, introducir correo y contraseña, confirmar el correo recibido y volver a iniciar sesión.</p>

<h2>15.2 Crear vehículo</h2>
<p>Después de iniciar sesión, el primer paso recomendado es crear un vehículo. Sin vehículos, las pantallas de repostajes, gastos, mantenimientos, recordatorios y documentación no tienen una referencia sobre la que trabajar.</p>

<h2>15.3 Gestionar datos del vehículo</h2>
<p>Desde la ficha del vehículo se puede consultar información principal y acceder a resúmenes de actividad. La app centraliza repostajes, gastos, mantenimientos, recordatorios y documentación.</p>

<h2>15.4 Consultar métricas</h2>
<p>Las métricas ayudan a interpretar el uso del vehículo. El consumo medio actual muestra litros cada cien kilómetros, mientras que el coste medio de combustible indica cuánto cuesta recorrer cien kilómetros según los repostajes registrados.</p>

<h2>15.5 Gestionar documentación</h2>
<p>La documentación permite crear tarjetas libres. El usuario decide el título y los campos. Esta flexibilidad evita que la app limite documentos que no han sido previstos durante el diseño.</p>

<h2>15.6 Uso sin conexión</h2>
<p>Si el dispositivo no tiene conexión, la aplicación avisa mediante un banner. El usuario puede seguir creando y editando muchos datos. La sincronización se realizará cuando vuelva la conexión.</p>

<h1>16. Conclusiones</h1>
<p>GestCar ha evolucionado desde una aplicación centrada en vehículos hasta una herramienta integral de gestión. La arquitectura elegida ha permitido añadir módulos complejos sin romper el núcleo inicial: repostajes, mantenimientos, gastos, recordatorios, documentación, adjuntos, estadísticas, cuenta, notificaciones y gasolineras.</p>
<p>La principal fortaleza del proyecto es que resuelve un problema real con una solución práctica. No se limita a demostrar una tecnología aislada, sino que combina persistencia local, sincronización remota, seguridad, UX móvil, manejo de archivos, permisos, background work y datos externos.</p>
<p>El enfoque offline-first es especialmente relevante porque diferencia la aplicación de un simple cliente remoto. Un usuario puede crear información sin conexión y no perderla. Esto encaja con casos reales como garajes, talleres, gasolineras con poca cobertura o uso en carretera.</p>
<p>La sección de documentación aporta valor diferencial al permitir que el usuario modele su propia información. En lugar de imponer formularios rígidos para todos los documentos posibles, GestCar permite tarjetas y campos personalizados, lo que aumenta la vida útil del diseño.</p>
<p>Como líneas de mejora, se recomienda estabilizar completamente la pantalla de gasolineras, ampliar pruebas automatizadas, preparar firma release, mejorar accesibilidad, revisar textos finales y completar capturas reales antes de la entrega definitiva.</p>

<h1>17. Líneas futuras</h1>
"@
$html += Lista @(
    "Publicación controlada en Google Play con firma release y política de privacidad.",
    "Pruebas unitarias y de integración para repositorios, cálculos y validadores.",
    "Cifrado adicional de documentación sensible en local.",
    "Mejoras avanzadas en mapa de gasolineras, buscador de localidad y caché de resultados.",
    "Exportación PDF con diseño definitivo y logo de la aplicación.",
    "Soporte para varios idiomas si el proyecto crece.",
    "Panel de ajustes para preferencias de notificaciones y sincronización.",
    "Mejoras de accesibilidad para tamaños de fuente elevados y lectores de pantalla.",
    "Sistema de copias de seguridad manuales cifradas.",
    "Comparativa de consumos entre vehículos del mismo usuario."
)

$html += @"
<h1>18. Bibliografía</h1>
<p>Las siguientes referencias se presentan en formato APA 7 orientativo y deberán revisarse antes de la entrega final para ajustar fecha de consulta si el centro lo exige.</p>
<ul>
"@
foreach ($referencia in $referencias) {
    $html += "<li>$referencia</li>"
}
$html += @"
</ul>

<h1>19. Anexos</h1>
<h2>Anexo A. Lista de capturas recomendadas</h2>
"@
$html += Tabla @("Número", "Captura", "Objetivo") @(
    @("A-01", "Splash", "Demostrar carga inicial sin mostrar login de forma brusca."),
    @("A-02", "Login", "Mostrar autenticación."),
    @("A-03", "Registro", "Mostrar alta de usuario."),
    @("A-04", "Confirmación de correo", "Evidenciar flujo de activación."),
    @("A-05", "Vehículos", "Mostrar pantalla principal."),
    @("A-06", "Detalle vehículo", "Mostrar ficha completa."),
    @("A-07", "Foto vehículo", "Mostrar cámara/galería."),
    @("A-08", "Repostajes", "Mostrar consumo y coste medio."),
    @("A-09", "Mantenimientos", "Mostrar pendientes y realizados."),
    @("A-10", "Gastos", "Mostrar vencimientos y pagados."),
    @("A-11", "Recordatorios", "Mostrar avisos."),
    @("A-12", "Documentación", "Mostrar tarjetas libres."),
    @("A-13", "Adjuntos", "Mostrar previsualización de archivo."),
    @("A-14", "Cuenta", "Mostrar acciones de usuario."),
    @("A-15", "Estadísticas", "Mostrar métricas."),
    @("A-16", "Gasolineras", "Mostrar mapa y rango."),
    @("A-17", "Offline", "Mostrar banner sin conexión."),
    @("A-18", "Sincronización", "Mostrar dato replicado en otro dispositivo.")
)

$html += @"
<h2>Anexo B. Estructura de paquetes</h2>
"@
$html += Tabla @("Paquete", "Contenido", "Finalidad") @(
    @("datos/basedatos", "GestCarBaseDatos y migraciones.", "Configurar Room."),
    @("datos/dao", "Interfaces DAO.", "Encapsular consultas locales."),
    @("datos/entidades", "Entidades Room.", "Modelo local."),
    @("datos/remoto", "DTOs y cliente remoto.", "Modelo remoto y Supabase."),
    @("datos/repositorio", "Repositorios.", "Coordinar local y remoto."),
    @("ui/pantallas", "Pantallas Compose.", "Interfaz principal."),
    @("ui/componentes", "Componentes reutilizables.", "Evitar duplicación visual."),
    @("ui/navegacion", "Rutas y grafo.", "Navegación."),
    @("ui/viewmodel", "ViewModels.", "Estado y lógica de presentación."),
    @("util", "Utilidades.", "Imágenes, archivos, sincronización, notificaciones, normalizadores.")
)

$html += @"
<h2>Anexo C. Checklist previo a entrega</h2>
"@
$html += Lista @(
    "Actualizar índice automático en Word.",
    "Sustituir todos los placeholders por capturas reales.",
    "Revisar ortografía y coherencia de estilos.",
    "Comprobar que no aparecen claves, URLs sensibles ni SQL privado.",
    "Revisar bibliografía y fecha de consulta.",
    "Generar PDF final desde DOCX.",
    "Probar APK final en al menos un emulador y un dispositivo físico.",
    "Revisar que el README público no expone información sensible.",
    "Adjuntar APK si el procedimiento de entrega lo permite.",
    "Preparar una explicación breve de despliegue y casos de uso para defensa."
)

$html += @"
<h2>Anexo D. Glosario</h2>
"@
$html += Tabla @("Término", "Definición") @(
    @("APK", "Paquete instalable de una aplicación Android."),
    @("Backend", "Servicios remotos que gestionan autenticación, base de datos y almacenamiento."),
    @("DTO", "Objeto utilizado para transferir datos entre app y backend."),
    @("MVVM", "Patrón Modelo-Vista-ViewModel para separar responsabilidades."),
    @("Offline-first", "Diseño que prioriza guardar datos localmente antes de depender de red."),
    @("Room", "Librería de persistencia local de Android basada en SQLite."),
    @("RLS", "Row Level Security, política de seguridad por fila en base de datos."),
    @("StateFlow", "Flujo observable de estado utilizado en Kotlin."),
    @("Storage", "Servicio de almacenamiento de archivos."),
    @("WorkManager", "Librería de Android para tareas en segundo plano.")
)

$html += @"
</body>
</html>
"@

# escribimos el html con bom para que libreoffice detecte bien utf 8
# si no lo hacemos convierte los acentos como texto mojibakeado
$utf8ConBom = New-Object System.Text.UTF8Encoding($true)
[System.IO.File]::WriteAllText($htmlSalida, $html, $utf8ConBom)

if (Test-Path $docxSalida) {
    Remove-Item -LiteralPath $docxSalida -Force
}

& $soffice --headless --norestore --nolockcheck --nodefault "-env:UserInstallation=file:///C:/Users/Diego/Desktop/TFG/GestCar-Fase1/documentacion/lo_profile" --infilter="HTML (StarWriter):UTF8" --convert-to docx:"Office Open XML Text" --outdir $PSScriptRoot $htmlSalida | Out-Null

if (-not (Test-Path $docxSalida)) {
    throw "No se ha generado el DOCX esperado en $docxSalida"
}

Write-Host "Memoria generada: $docxSalida"
