# GestCar

GestCar es una aplicación Android nativa desarrollada en Kotlin con Jetpack Compose para gestionar vehículos particulares, sus repostajes, mantenimientos, reparaciones, gastos, recordatorios y fotografías.

El objetivo principal del proyecto es ofrecer una herramienta sencilla para registrar y consultar información importante del vehículo, manteniendo una copia local para que la aplicación siga funcionando sin conexión y sincronizando los datos con un backend remoto cuando vuelve a haber red.

Este repositorio contiene el código fuente del proyecto. Para probar la aplicación como usuario final, lo más recomendable es instalar un APK generado desde este proyecto. El repositorio por sí solo no es una app instalable directamente en un móvil.

## Aviso De Seguridad

Este README está preparado para poder publicarse en GitHub sin exponer información sensible.

Por ese motivo, no se incluyen:

- URLs reales del backend.
- Claves de API.
- Tokens.
- Contraseñas.
- Configuración SMTP privada.
- Scripts SQL completos.
- Funciones internas sensibles de la base de datos.
- Datos reales de usuarios.
- Datos reales de vehículos.

La configuración privada necesaria para conectar la app con servicios externos debe mantenerse fuera de la documentación pública y gestionarse con cuidado.

## Estado Actual

GestCar se encuentra en un estado funcional avanzado. Actualmente permite:

- Registro de usuarios mediante correo electrónico.
- Confirmación de cuenta por correo.
- Inicio de sesión.
- Persistencia de sesión.
- Cierre de sesión.
- Recuperación de contraseña.
- Navegación principal con barra inferior.
- Gestión completa de vehículos.
- Organización de vehículos habituales y secundarios.
- Fotografías de vehículos desde cámara o galería.
- Copia local comprimida de imágenes.
- Sincronización remota de imágenes.
- Gestión de repostajes.
- Cálculo de consumo medio.
- Cálculo de coste medio de combustible.
- Filtros temporales para consultar repostajes por periodo.
- Gestión de mantenimientos y reparaciones.
- Separación entre operaciones pendientes y realizadas.
- Gestión de gastos.
- Control de gastos pendientes y pagados.
- Gestión visual de vencimientos.
- Gestión de recordatorios.
- Resumen de actividad dentro de la ficha del vehículo.
- Banner de conectividad.
- Funcionamiento offline-first.
- Sincronización al recuperar conexión.
- Sincronización periódica en segundo plano.
- Pantalla de cuenta.
- Eliminación de cuenta mediante operación protegida en backend.
- Tema visual propio basado en Material Design 3.
- Icono personalizado de la aplicación.

## Idea General De La Aplicación

GestCar nace para resolver un problema muy cotidiano: tener dispersa la información del vehículo.

Con la app se puede guardar:

- Qué vehículos tiene el usuario.
- Kilometraje actual.
- Matrícula.
- Tipo de combustible.
- Foto identificativa del vehículo.
- Repostajes realizados.
- Litros repostados.
- Precio por litro.
- Coste total del repostaje.
- Mantenimientos realizados.
- Reparaciones pendientes.
- Reparaciones ya realizadas.
- Gastos como seguros, ITV, impuestos, parking, peajes o lavados.
- Recordatorios por fecha o kilometraje.

La aplicación está pensada para usarse en situaciones reales. Por ejemplo, si el usuario registra un repostaje en una zona sin cobertura, el dato se guarda localmente y queda pendiente de sincronización. Cuando el dispositivo vuelve a tener conexión, la app intenta subirlo al backend.

## Tecnologías Utilizadas

### Aplicación Android

- Kotlin.
- Jetpack Compose.
- Material Design 3.
- Navigation Compose.
- ViewModel.
- StateFlow.
- Room.
- WorkManager.
- FileProvider.
- Coil.
- Kotlin Serialization.

### Persistencia Y Backend

- Room como base de datos local.
- Backend remoto para autenticación, base de datos y almacenamiento de imágenes.
- Row Level Security en el backend.
- Storage privado para fotografías.
- Servicio SMTP externo para correos transaccionales.

No se documenta públicamente la configuración concreta del backend ni del SMTP por motivos de seguridad.

## Arquitectura

La arquitectura sigue un patrón MVVM con repositorios.

```text
Pantallas Compose
    |
ViewModels
    |
Repositorios
    |
Room + Backend remoto
```

### Pantallas Compose

Las pantallas se encargan de:

- Mostrar datos.
- Capturar interacción del usuario.
- Mostrar formularios.
- Mostrar tarjetas.
- Mostrar diálogos.
- Navegar entre pantallas.

No deben acceder directamente a la base de datos ni al backend.

### ViewModels

Los ViewModels se encargan de:

- Mantener el estado de cada pantalla.
- Validar formularios.
- Lanzar operaciones en corrutinas.
- Pedir datos a los repositorios.
- Exponer datos mediante `StateFlow`.

### Repositorios

Los repositorios son la capa que conecta los datos locales con los remotos.

La regla general es:

1. Guardar primero en Room.
2. Actualizar la interfaz inmediatamente.
3. Intentar sincronizar con el backend remoto.
4. Si no hay conexión o la sincronización falla, conservar el dato local.
5. Reintentar más adelante.

Este enfoque permite que la aplicación sea offline-first.

### Room

Room se utiliza como base de datos local. Permite que el usuario pueda abrir la app y consultar sus datos aunque no tenga internet.

También permite que la interfaz se actualice de forma reactiva cuando cambian los datos locales.

### WorkManager

WorkManager se utiliza para programar sincronizaciones en segundo plano.

Esto es importante porque el usuario puede crear datos sin conexión, cerrar la app y no volver a abrirla durante horas o días. La sincronización periódica ayuda a que esos datos se suban cuando el dispositivo recupere conexión.

## Modelo Funcional

### Vehículos

La sección de vehículos permite:

- Ver todos los vehículos del usuario.
- Crear un vehículo.
- Editar un vehículo.
- Eliminar un vehículo.
- Ver la ficha completa.
- Añadir o cambiar foto.
- Ordenar vehículos.
- Separar vehículos habituales y secundarios.

El primer vehículo habitual se usa como referencia inicial en otras pantallas, como repostajes, gastos o mantenimientos.

### Repostajes

La sección de repostajes permite:

- Registrar un repostaje.
- Ver el historial de repostajes.
- Consultar el detalle de un repostaje.
- Editar un repostaje.
- Eliminar un repostaje.
- Filtrar por periodo.
- Calcular consumo medio.
- Calcular coste medio por cada 100 km.

Campos habituales de un repostaje:

- Vehículo.
- Fecha.
- Kilómetros.
- Litros.
- Precio por litro.
- Importe total.
- Si el depósito se llenó completo.
- Gasolinera.
- Notas.

El importe puede calcularse a partir de litros y precio por litro.

### Mantenimientos Y Reparaciones

La sección de mantenimiento permite registrar operaciones relacionadas con el estado mecánico del vehículo.

La app diferencia:

- Mantenimientos.
- Reparaciones.
- Operaciones pendientes.
- Operaciones realizadas.

No se usa una lista cerrada de componentes obligatorios, porque un vehículo puede tener averías muy variadas. El usuario puede introducir el componente o tipo de operación con libertad.

Ejemplos:

- Cambio de aceite.
- Frenos.
- Neumáticos.
- Batería.
- Correa de distribución.
- Amortiguadores.
- Caja de cambios.
- Motor.
- Reparación de carrocería.
- Otro componente.

Campos habituales:

- Tipo o componente.
- Categoría.
- Estado.
- Fecha.
- Kilómetros.
- Coste.
- Taller.
- Descripción.

El coste puede ser cero, ya que algunas reparaciones pueden estar cubiertas por garantía, campañas de marca o seguros.

### Gastos

La sección de gastos permite registrar costes no necesariamente ligados a una reparación.

Ejemplos:

- Seguro.
- ITV.
- Impuesto de circulación.
- Parking.
- Peajes.
- Lavados.
- Otros gastos.

La app diferencia:

- Gastos pendientes.
- Gastos pagados.
- Gastos vencidos.
- Gastos próximos.
- Gastos al día.
- Gastos únicos.
- Gastos periódicos.

Cuando un gasto periódico se marca como pagado, la app puede ayudar a crear el siguiente aviso según la periodicidad.

### Recordatorios

Los recordatorios permiten crear avisos manuales para tareas o controles del vehículo.

Pueden basarse en:

- Fecha.
- Kilometraje.
- Fecha y kilometraje.

Ejemplos:

- Revisar presión de neumáticos.
- Cambiar aceite a cierto kilometraje.
- Revisar niveles.
- Renovar documentación.
- Comprobar una reparación pendiente.

### Ficha Del Vehículo

La ficha del vehículo centraliza información relevante:

- Foto.
- Marca.
- Modelo.
- Matrícula.
- Tipo.
- Año de fabricación.
- Kilometraje.
- Combustible.
- Fecha de alta.
- Métricas rápidas.
- Últimos repostajes.
- Últimos mantenimientos.
- Gastos activos.
- Recordatorios.

La idea es que el usuario pueda abrir un vehículo y entender rápidamente su estado general.

### Cuenta

La pantalla de cuenta muestra información básica del usuario y permite solicitar la eliminación de cuenta.

Por seguridad, no se muestran identificadores internos del backend al usuario final.

## Funcionamiento Offline

GestCar está diseñada para no depender totalmente de internet.

Si no hay conexión:

- El usuario puede seguir consultando datos guardados localmente.
- Puede crear nuevos registros.
- Puede editar registros existentes.
- Los cambios se guardan en Room.
- La app muestra un aviso de falta de conexión.
- La sincronización queda pendiente.

Cuando vuelve la conexión:

- La app muestra un aviso de recuperación de conexión.
- Se encola una sincronización puntual.
- WorkManager puede sincronizar más adelante en segundo plano.

## Gestión De Imágenes

Las fotos de los vehículos se tratan con especial cuidado.

La app:

- Permite elegir imagen desde galería.
- Permite hacer foto con cámara.
- Comprime la imagen.
- Guarda una copia local privada.
- Sube la imagen al almacenamiento remoto cuando puede.
- Usa rutas privadas.
- Evita depender de una imagen remota para mostrar la foto sin conexión.

Esto mejora rendimiento, privacidad y funcionamiento offline.

## Seguridad Del Proyecto

El proyecto está planteado teniendo en cuenta que trata información sensible.

Puede incluir:

- Matrículas.
- Fotos de vehículos.
- Kilometraje.
- Historial de repostajes.
- Gastos.
- Reparaciones.
- Datos vinculados a usuarios.

Medidas aplicadas:

- Autenticación obligatoria.
- Datos separados por usuario.
- Row Level Security en el backend.
- Almacenamiento privado de imágenes.
- No se incluyen claves privilegiadas en la app.
- No se muestran identificadores internos al usuario final.
- La eliminación de cuenta se realiza mediante una operación protegida en backend.
- El README público no contiene SQL ni credenciales.

## Configuración Privada No Incluida

Para ejecutar GestCar contra un backend real es necesario configurar servicios externos.

Esa configuración no se documenta con valores reales en este README.

Cada entorno debe configurar de forma privada:

- Proyecto backend.
- Autenticación.
- Base de datos.
- Políticas de seguridad.
- Almacenamiento privado.
- Proveedor SMTP.
- URLs de redirección.
- Claves públicas permitidas.

Las claves privilegiadas nunca deben incluirse en el código Android ni en documentación pública.

## Instalación Para Probar La App Como Usuario

Si el objetivo es probar la aplicación en un móvil, lo recomendable es instalar un APK generado por el desarrollador.

### Opción Recomendada: Instalar APK

Pasos para probar la app en un dispositivo Android:

1. Recibir el archivo APK generado desde el proyecto.
2. Copiar el APK al móvil.
3. Abrir el APK desde el gestor de archivos, WhatsApp, Drive, correo o similar.
4. Si Android lo solicita, permitir la instalación desde esa fuente.
5. Instalar la aplicación.
6. Abrir GestCar.
7. Crear una cuenta o iniciar sesión con una cuenta existente.
8. Confirmar el correo si el sistema lo solicita.
9. Volver a abrir la app.
10. Registrar un vehículo.
11. Probar repostajes, gastos, mantenimientos y recordatorios.

### Importante

El enlace de GitHub contiene el código fuente. No es equivalente a un APK instalable.

Para probar la app como usuario final, debe usarse un APK.

Para revisar el código, puede usarse el repositorio.

Son dos usos distintos.

## Instalación Desde Android Studio

Esta opción es para quien quiera compilar la app desde el código fuente.

### Requisitos

Se recomienda tener:

- Android Studio actualizado.
- JDK compatible con Android Studio.
- SDK de Android instalado.
- Conexión a internet para descargar dependencias.
- Un emulador Android o un dispositivo físico con depuración USB.

### Abrir El Proyecto

1. Descargar o clonar el repositorio.
2. Abrir Android Studio.
3. Seleccionar `File > Open`.
4. Elegir la carpeta raíz del proyecto.
5. Esperar a que Android Studio sincronice Gradle.
6. Revisar que el módulo seleccionado sea `app`.
7. Ejecutar la app desde el botón Run.

### Ejecutar En Emulador

1. Crear o abrir un emulador Android.
2. Esperar a que el emulador arranque por completo.
3. Seleccionar el emulador como dispositivo de destino.
4. Pulsar Run.
5. Esperar a que Android Studio instale y abra la app.

### Ejecutar En Dispositivo Físico

1. Activar las opciones de desarrollador en el móvil.
2. Activar depuración USB.
3. Conectar el móvil al ordenador.
4. Aceptar la huella RSA si Android lo pregunta.
5. Seleccionar el dispositivo en Android Studio.
6. Pulsar Run.

### Problemas Frecuentes Al Compilar

Si Android Studio no compila:

- Comprobar que Gradle ha sincronizado correctamente.
- Comprobar que el SDK requerido está instalado.
- Comprobar que hay conexión a internet.
- Limpiar y reconstruir el proyecto.
- Revisar la pestaña Build para ver errores.
- Asegurarse de abrir la carpeta raíz correcta.

Si la app se instala pero no abre:

- Revisar Logcat.
- Comprobar que el APK instalado corresponde a la versión actual.
- Desinstalar versiones antiguas.
- Reinstalar.
- Comprobar que el dispositivo tiene conexión si se va a registrar o iniciar sesión.

## Generar APK Debug

Desde Android Studio:

1. Abrir el proyecto.
2. Seleccionar `Build`.
3. Seleccionar `Build Bundle(s) / APK(s)`.
4. Seleccionar `Build APK(s)`.
5. Esperar a que termine.
6. Abrir la ubicación del APK generado.

Desde terminal en Windows:

```powershell
.\gradlew.bat assembleDebug
```

El APK debug se genera en:

```text
app/build/outputs/apk/debug/
```

El APK debug está firmado con la clave debug de Android y sirve para pruebas.

## Uso Básico De La App

### 1. Abrir La Aplicación

Al abrir GestCar aparece una pantalla inicial de carga. Si hay sesión guardada, la app accede directamente a la pantalla principal.

Si no hay sesión guardada, se muestra la pantalla de inicio de sesión.

### 2. Crear Cuenta

1. Pulsar en la opción de registro.
2. Introducir correo electrónico.
3. Introducir contraseña.
4. Confirmar contraseña.
5. Pulsar registrarse.
6. Revisar el correo.
7. Confirmar la cuenta desde el enlace recibido.
8. Volver a la app e iniciar sesión.

### 3. Iniciar Sesión

1. Introducir correo.
2. Introducir contraseña.
3. Pulsar iniciar sesión.
4. Esperar a que cargue la pantalla de vehículos.

### 4. Crear Un Vehículo

1. Entrar en la pestaña Vehículos.
2. Pulsar el botón `+`.
3. Rellenar los campos obligatorios.
4. Añadir información opcional si se desea.
5. Guardar.

Después de crear un vehículo, aparecerá en la lista principal.

### 5. Añadir Foto A Un Vehículo

1. Entrar en la ficha del vehículo.
2. Pulsar el botón de foto.
3. Elegir entre cámara o galería.
4. Seleccionar o tomar la imagen.
5. Esperar a que se procese.

La app guarda una copia local comprimida para que la imagen se pueda ver incluso sin conexión.

### 6. Registrar Un Repostaje

1. Entrar en la pestaña Repostajes.
2. Seleccionar el vehículo correspondiente.
3. Pulsar `+`.
4. Introducir fecha, kilómetros, litros y precio.
5. Añadir gasolinera o notas si se desea.
6. Guardar.

El repostaje aparecerá en el historial y se usará para calcular consumo y coste medio.

### 7. Registrar Un Mantenimiento O Reparación

1. Entrar en la pestaña Mantenimiento.
2. Seleccionar vehículo.
3. Pulsar `+`.
4. Indicar tipo o componente.
5. Elegir si es mantenimiento o reparación.
6. Marcar si está pendiente o realizada.
7. Añadir fecha, coste, taller o descripción.
8. Guardar.

### 8. Registrar Un Gasto

1. Entrar en la pestaña Gastos.
2. Seleccionar vehículo.
3. Pulsar `+`.
4. Indicar concepto.
5. Indicar importe.
6. Indicar fecha.
7. Elegir periodicidad si procede.
8. Guardar.

Los gastos periódicos pueden servir para controlar vencimientos.

### 9. Marcar Un Gasto Como Pagado

1. Entrar en el detalle del gasto.
2. Pulsar la opción de marcar como pagado.
3. Confirmar.
4. Si el gasto es periódico, decidir si se quiere crear el siguiente aviso.

### 10. Crear Un Recordatorio

1. Entrar en Más.
2. Abrir Recordatorios.
3. Pulsar `+`.
4. Elegir vehículo.
5. Introducir concepto.
6. Añadir fecha límite, kilometraje o ambos.
7. Guardar.

### 11. Cerrar Sesión

1. Ir a Más.
2. Pulsar cerrar sesión.
3. Confirmar si procede.

### 12. Eliminar Cuenta

1. Ir a Más.
2. Entrar en Cuenta.
3. Pulsar eliminar cuenta.
4. Leer la advertencia.
5. Confirmar la primera ventana.
6. Confirmar la segunda ventana.

Esta acción está pensada para eliminar la cuenta y los datos asociados.

## Recomendaciones Para Evaluación Académica

Para revisar el proyecto, se recomienda:

1. Leer este README.
2. Abrir el proyecto en Android Studio.
3. Revisar la estructura de paquetes.
4. Empezar por `ActividadPrincipal`.
5. Revisar el grafo de navegación.
6. Revisar los ViewModels.
7. Revisar los repositorios.
8. Revisar las entidades Room.
9. Revisar los DAOs.
10. Revisar las pantallas principales.

Para probar la app como usuario:

1. Instalar un APK debug generado por el desarrollador.
2. Crear una cuenta.
3. Confirmar el correo.
4. Crear un vehículo.
5. Añadir una foto.
6. Crear repostajes.
7. Crear gastos.
8. Crear mantenimientos.
9. Crear recordatorios.
10. Probar el modo sin conexión.

## Estructura Principal Del Código

```text
app/src/main/java/com/gestcar/
```

Paquetes principales:

```text
datos/
  basedatos/
  dao/
  entidades/
  remoto/
  repositorio/

ui/
  componentes/
  navegacion/
  pantallas/
  tema/
  viewmodel/

util/
```

### datos

Contiene la capa de datos:

- Entidades de Room.
- DAOs.
- Base de datos local.
- DTOs remotos.
- Repositorios.
- Cliente remoto.

### ui

Contiene la capa visual:

- Pantallas Compose.
- Componentes reutilizables.
- Navegación.
- Tema.
- ViewModels.

### util

Contiene utilidades:

- Normalización de entradas numéricas.
- Gestión de imágenes.
- Normalización de matrículas.
- Observación de conectividad.
- Planificación de sincronización.
- Worker de sincronización.

## Entrega Del Proyecto

Para entregar el código fuente, incluir:

- `app/`
- `gradle/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `README.md`

No incluir:

- `.gradle/`
- `.idea/`
- `.kotlin/`
- `build/`
- `app/build/`
- `local.properties`
- APKs si la entrega pide solo código fuente.
- Capturas con datos reales.
- Ficheros con claves.
- Scripts SQL sensibles.

Si también se quiere entregar una versión instalable, se puede adjuntar el APK por separado.

## Limitaciones Actuales

Aunque GestCar ya tiene muchas funcionalidades, todavía hay partes ampliables:

- Estadísticas completas con gráficos avanzados.
- Exportación CSV definitiva.
- Notificaciones del sistema.
- Preferencias avanzadas de cuenta.
- Tests automatizados.
- Firma release final.
- Mejoras de accesibilidad.
- Mejoras de internacionalización.

## Buenas Prácticas De Seguridad

Para mantener el proyecto seguro:

- No publicar claves privadas.
- No publicar scripts SQL sensibles.
- No publicar credenciales SMTP.
- No incluir claves privilegiadas en el APK.
- No mostrar identificadores internos al usuario final.
- No convertir almacenamiento privado en público.
- Mantener políticas de seguridad activas en backend.
- Revisar cualquier dato antes de subirlo a GitHub.

## Resumen

GestCar es una app Android de gestión de vehículos diseñada con arquitectura MVVM, almacenamiento local, sincronización remota y enfoque offline-first.

El proyecto prioriza:

- Usabilidad.
- Persistencia local.
- Sincronización entre dispositivos.
- Seguridad de datos.
- Claridad arquitectónica.
- Separación de responsabilidades.
- Facilidad de revisión académica.

La documentación pública evita exponer detalles sensibles, pero mantiene suficiente información para entender el alcance, la arquitectura, el uso y el proceso de instalación de la aplicación.
