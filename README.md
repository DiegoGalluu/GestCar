# GestCar

GestCar es una aplicación Android nativa para la gestión integral de vehículos particulares. Permite registrar vehículos, repostajes, mantenimientos, reparaciones, gastos periódicos, recordatorios y fotografías, manteniendo una copia local para funcionar sin conexión y sincronizando los datos con Supabase cuando hay red.

El proyecto está desarrollado en Kotlin con Jetpack Compose, Room, MVVM, WorkManager y Supabase. La idea principal de la app es que el usuario pueda consultar y registrar información importante de sus vehículos incluso en situaciones sin cobertura, evitando perder datos y sincronizándolos posteriormente con el backend remoto.

## Estado actual del proyecto

La app se encuentra en un estado funcional avanzado. Actualmente ya incluye:

- Autenticación con Supabase mediante correo electrónico y contraseña.
- Registro de usuarios con confirmación de correo.
- Recuperación y cambio de contraseña mediante deep links.
- Persistencia de sesión para no obligar al usuario a iniciar sesión cada vez.
- Splash inicial para evitar que se vea el login mientras se recupera una sesión guardada.
- Navegación con barra inferior de cinco secciones principales.
- CRUD completo de vehículos.
- Organización manual de vehículos habituales y secundarios.
- Selector de vehículo activo reutilizable en las secciones de datos.
- Registro, edición, detalle y eliminación de repostajes.
- Cálculo de consumo medio en L/100 km.
- Cálculo de coste medio de combustible cada 100 km.
- Filtros temporales para repostajes.
- Registro, edición, detalle y eliminación de mantenimientos y reparaciones.
- Separación de operaciones pendientes y realizadas en mantenimientos.
- Registro, edición, detalle y eliminación de gastos periódicos.
- Control de gastos pendientes y pagados.
- Gestión de vencimientos en gastos, con diferenciación visual de vencidos, próximos y al día.
- Registro, edición, detalle y eliminación de recordatorios.
- Recordatorios por fecha, kilometraje o ambos.
- Resumen dentro del detalle del vehículo con últimos repostajes, mantenimientos, gastos y recordatorios.
- Métricas rápidas del vehículo: consumo, coste de combustible por 100 km, gasto registrado y gasto por kilómetro.
- Fotos de vehículos desde cámara o galería.
- Compresión de imágenes antes de guardarlas o subirlas.
- Copia local privada de imágenes para que se vean sin conexión.
- Storage privado en Supabase con signed URLs temporales.
- Banner de conectividad para avisar cuando no hay conexión y cuando vuelve la conexión.
- Sincronización offline-first entre Room y Supabase.
- Sincronización puntual al recuperar conexión.
- Sincronización periódica en segundo plano con WorkManager.
- Pantalla de cuenta con correo del usuario y opción de eliminar cuenta.
- Eliminación de cuenta mediante función segura en Supabase, sin incluir claves privilegiadas en la app.
- Tema Material Design 3 con paleta personalizada.
- Icono personalizado de la aplicación.
- Comentarios explicativos en las zonas principales del código.

## Objetivo de la aplicación

GestCar está pensada para centralizar en una sola app toda la información importante asociada a un vehículo:

- Datos básicos del vehículo.
- Historial de repostajes.
- Historial de mantenimiento y reparaciones.
- Gastos fijos o periódicos como seguro, ITV, impuestos, parking o peajes.
- Recordatorios de tareas, revisiones o vencimientos.
- Fotografías identificativas del vehículo.
- Resumen de costes y consumo.

Un caso de uso importante es el funcionamiento sin conexión. Por ejemplo, si un usuario registra un repostaje en una zona sin cobertura, la app guarda ese dato en Room y lo sube a Supabase cuando vuelva a tener conexión, incluso si la app se sincroniza más tarde mediante el worker en segundo plano.

## Tecnologías utilizadas

### Android

- Kotlin.
- Jetpack Compose.
- Material Design 3.
- Navigation Compose.
- ViewModel.
- StateFlow.
- Room.
- WorkManager.
- FileProvider.
- Coil para carga de imágenes.

### Backend

- Supabase Auth.
- Supabase PostgREST.
- Supabase Storage.
- Row Level Security.
- PostgreSQL.
- Funciones SQL para operaciones seguras como eliminación de cuenta.

### Herramientas de build

- Gradle.
- Android Gradle Plugin.
- Kotlin Serialization.
- KSP para Room.

## Requisitos técnicos

El proyecto está configurado con:

- `minSdk`: 26.
- `targetSdk`: 35.
- `compileSdk`: 35.
- Java 17.
- Kotlin 2.0.21.
- Android Gradle Plugin 8.7.3.
- Room 2.6.1.
- Navigation Compose 2.8.4.
- Supabase Kotlin 3.0.2.
- WorkManager 2.9.1.

## Estructura del proyecto

La estructura principal del código está en:

```text
app/src/main/java/com/gestcar/
```

Organización de paquetes:

```text
com.gestcar/
  ActividadPrincipal.kt
  GestCarAplicacion.kt

  datos/
    basedatos/
      GestCarBaseDatos.kt

    dao/
      VehiculoDao.kt
      RepostajeDao.kt
      MantenimientoDao.kt
      GastoPeriodicoDao.kt
      RecordatorioDao.kt

    entidades/
      Vehiculo.kt
      Repostaje.kt
      Mantenimiento.kt
      GastoPeriodico.kt
      Recordatorio.kt

    remoto/
      ClienteSupabase.kt
      VehiculoDto.kt
      RepostajeDto.kt
      MantenimientoDto.kt
      GastoPeriodicoDto.kt
      RecordatorioDto.kt

    repositorio/
      VehiculoRepositorio.kt
      RepostajeRepositorio.kt
      MantenimientoRepositorio.kt
      GastoPeriodicoRepositorio.kt
      RecordatorioRepositorio.kt

  ui/
    componentes/
      BarraSuperiorCompacta.kt
      CampoFecha.kt
      FiltroPeriodo.kt
      ImagenVehiculo.kt
      SelectorVehiculoActivo.kt
      TarjetaVehiculo.kt
      TarjetaRepostaje.kt
      TarjetaMantenimiento.kt
      TarjetaGasto.kt
      TarjetaRecordatorio.kt

    navegacion/
      Rutas.kt
      GrafoNavegacion.kt

    pantallas/
      PantallaInicioSesion.kt
      PantallaRegistro.kt
      PantallaRestablecerContrasena.kt
      PantallaSplash.kt
      PantallaListaVehiculos.kt
      PantallaFormularioVehiculo.kt
      PantallaDetalleVehiculo.kt
      PantallaListaRepostajes.kt
      PantallaFormularioRepostaje.kt
      PantallaDetalleRepostaje.kt
      PantallaListaMantenimientos.kt
      PantallaFormularioMantenimiento.kt
      PantallaDetalleMantenimiento.kt
      PantallaListaGastos.kt
      PantallaFormularioGasto.kt
      PantallaDetalleGasto.kt
      PantallaListaRecordatorios.kt
      PantallaFormularioRecordatorio.kt
      PantallaDetalleRecordatorio.kt
      PantallaMasOpciones.kt
      PantallaCuenta.kt

    tema/
      Color.kt
      Tema.kt

    viewmodel/
      AutenticacionViewModel.kt
      CuentaViewModel.kt
      VehiculoViewModel.kt
      VehiculoActivoViewModel.kt
      RepostajeViewModel.kt
      MantenimientoViewModel.kt
      GastoPeriodicoViewModel.kt
      RecordatorioViewModel.kt
      DetalleVehiculoResumenViewModel.kt

  util/
    EntradaNumerica.kt
    GestorImagenesVehiculo.kt
    NormalizadorMatriculas.kt
    ObservadorConectividad.kt
    PlanificadorSincronizacion.kt
    RutasImagenVehiculo.kt
    SincronizacionWorker.kt
```

## Arquitectura

La aplicación sigue una arquitectura MVVM con repositorios:

```text
Pantallas Compose
    |
ViewModels
    |
Repositorios
    |
Room DAOs + Supabase
```

### Pantallas Compose

Las pantallas se encargan de:

- Mostrar el estado recibido desde los ViewModels.
- Capturar acciones del usuario.
- Navegar entre pantallas.
- Mostrar formularios, tarjetas, diálogos y estados vacíos.

No deberían contener lógica de persistencia ni consultas directas a Supabase.

### ViewModels

Los ViewModels se encargan de:

- Mantener el estado de UI mediante `StateFlow`.
- Validar formularios.
- Lanzar operaciones en `viewModelScope`.
- Pedir datos al repositorio.
- Transformar datos para la pantalla.

Ejemplos:

- `VehiculoViewModel`: lista, formulario, detalle e imágenes de vehículos.
- `RepostajeViewModel`: repostajes, filtros temporales, consumo y coste medio.
- `VehiculoActivoViewModel`: selector global de vehículo activo para secciones de la bottom bar.
- `DetalleVehiculoResumenViewModel`: resumen del detalle del vehículo combinando varias tablas.
- `AutenticacionViewModel`: login, registro, recuperación de contraseña y sesión.
- `CuentaViewModel`: eliminación segura de cuenta.

### Repositorios

Los repositorios son el puente entre Room y Supabase. La regla general del proyecto es:

1. Guardar primero en Room.
2. Intentar sincronizar con Supabase.
3. Si Supabase falla, mantener el dato local.
4. Reintentar la sincronización más adelante.

Este patrón permite que la app sea offline-first.

### Room

Room es la base de datos local de la app. Se utiliza para:

- Guardar vehículos.
- Guardar repostajes.
- Guardar mantenimientos.
- Guardar gastos periódicos.
- Guardar recordatorios.
- Mantener datos disponibles sin conexión.
- Repintar la UI mediante `Flow`.

La base de datos actual está en versión 6.

### Supabase

Supabase se utiliza para:

- Autenticación.
- Base de datos remota.
- Sincronización entre dispositivos.
- Storage privado de fotos.
- Seguridad mediante Row Level Security.

## Modelo de datos

### Vehículos

Tabla local y remota principal. Cada vehículo pertenece a un usuario.

Campos principales:

- `id`
- `usuario_id`
- `marca`
- `modelo`
- `anio_fabricacion`
- `mes_fabricacion`
- `dia_fabricacion`
- `tipo`
- `matricula`
- `kilometraje`
- `tipo_combustible`
- `imagen_uri`
- `fecha_alta`
- `notas`
- `actualizado_en`

En local existen además campos de organización:

- `habitual`
- `ordenLista`

Estos campos permiten ordenar la pantalla principal y decidir qué vehículo se carga primero en los selectores.

### Repostajes

Cada repostaje pertenece a un vehículo.

Campos principales:

- `id`
- `vehiculo_id`
- `fecha`
- `kilometros`
- `litros`
- `precio_por_litro`
- `importe_total`
- `lleno_completo`
- `gasolinera`
- `notas`
- `actualizado_en`

La app calcula:

- Consumo medio en L/100 km.
- Coste medio de combustible por 100 km.
- Totales por periodo.

El cálculo de consumo usa solo repostajes marcados como depósito lleno.

### Mantenimientos y reparaciones

Cada mantenimiento o reparación pertenece a un vehículo.

Campos principales:

- `id`
- `vehiculo_id`
- `tipo`
- `categoria`
- `fecha`
- `kilometros`
- `coste`
- `taller`
- `descripcion`
- `realizado`
- `fecha_realizado`
- `actualizado_en`

La pantalla permite tratar mantenimientos como una lista de operaciones pendientes o realizadas.

### Gastos periódicos

Cada gasto pertenece a un vehículo.

Campos principales:

- `id`
- `vehiculo_id`
- `concepto`
- `importe`
- `fecha`
- `periodicidad`
- `fecha_vencimiento`
- `pagado`
- `fecha_pago`
- `notas`
- `actualizado_en`

La app diferencia visualmente entre:

- Gastos vencidos.
- Gastos próximos.
- Gastos al día.
- Gastos pagados.

Los gastos únicos pueden quedar directamente como histórico si no requieren seguimiento futuro.

### Recordatorios

Cada recordatorio pertenece a un vehículo.

Campos principales:

- `id`
- `vehiculo_id`
- `concepto`
- `fecha_limite`
- `kilometraje_limite`
- `completado`
- `fecha_completado`
- `notas`
- `actualizado_en`

La app puede mostrar recordatorios vencidos, próximos, pendientes o completados.

## Sincronización offline-first

La sincronización es una parte central de GestCar.

### Guardado local primero

Cuando el usuario guarda un vehículo, repostaje, gasto, mantenimiento o recordatorio:

1. Se guarda primero en Room.
2. La UI se actualiza inmediatamente.
3. Se intenta subir a Supabase.
4. Si falla la red o Supabase no responde, el dato queda local.
5. La sincronización se reintenta más adelante.

Esto evita que el usuario pierda datos por no tener conexión.

### Sincronización al abrir secciones

Al cargar pantallas como vehículos, repostajes, gastos o mantenimientos, la app intenta:

1. Subir datos locales pendientes.
2. Descargar datos remotos.
3. Insertarlos en Room.
4. Mostrar la versión local actualizada.

### Sincronización al recuperar conexión

La barra superior observa cambios de conectividad. Si vuelve la conexión, se encola una sincronización puntual.

### Sincronización en segundo plano

`GestCarAplicacion` programa una sincronización periódica con WorkManager.

Actualmente se ejecuta cada 6 horas, siempre que haya conexión.

Esto cubre el caso de uso en el que el usuario:

1. Registra datos sin conexión.
2. Cierra la app.
3. Recupera conexión más tarde.
4. No vuelve a abrir GestCar inmediatamente.

WorkManager puede sincronizar esos datos aunque la app no esté en primer plano.

## Gestión de imágenes

Las fotos de vehículos tienen un tratamiento especial por privacidad y rendimiento.

### Captura o selección

El usuario puede:

- Hacer una foto con la cámara.
- Elegir una foto desde la galería.

### Compresión

Antes de guardar una imagen:

- Se reduce su tamaño máximo.
- Se comprime en JPEG.
- Se guarda una copia privada dentro del almacenamiento interno de la app.

Esto evita subir imágenes demasiado pesadas y permite mostrar fotos sin conexión.

### Storage privado

Las imágenes se suben a un bucket privado de Supabase llamado `vehiculos`.

La ruta remota sigue este patrón:

```text
usuario_id/vehiculo_id.jpg
```

La app no usa un bucket público. Cuando necesita mostrar una imagen remota, genera una signed URL temporal y guarda una copia local.

## Seguridad

GestCar trata datos sensibles:

- Matrículas.
- Imágenes de vehículos.
- Kilometraje.
- Historial de gastos.
- Historial de mantenimiento.
- Datos personales asociados a una cuenta.

Por ese motivo se han aplicado varias medidas.

### Row Level Security

Todas las tablas remotas tienen RLS activado. Cada usuario solo puede consultar, crear, editar o eliminar datos asociados a sus propios vehículos.

Las tablas hijas validan propiedad mediante la tabla `vehiculos`.

Por ejemplo, un repostaje solo es accesible si pertenece a un vehículo cuyo `usuario_id` coincide con `auth.uid()`.

### Storage privado

El bucket de fotos no es público. Las políticas de Storage limitan el acceso a la carpeta del usuario autenticado.

### Sin service_role en la app

La app no contiene claves privilegiadas de Supabase.

Esto es importante porque cualquier APK puede ser inspeccionado. Incluir una clave `service_role` en una app móvil sería una vulnerabilidad grave.

### Eliminación de cuenta

La pantalla Cuenta permite eliminar la cuenta del usuario con doble confirmación.

La eliminación real se hace mediante una función SQL segura en Supabase:

```sql
public.eliminar_cuenta_actual()
```

La función borra:

- Objetos del bucket del usuario.
- Vehículos del usuario.
- Datos asociados por borrado en cascada.
- Usuario de `auth.users`.

La función solo puede ejecutarse por usuarios autenticados y usa `auth.uid()` para borrar únicamente la cuenta actual.

## Autenticación

La autenticación usa Supabase Auth.

Funcionalidades implementadas:

- Registro con correo y contraseña.
- Confirmación de correo.
- Inicio de sesión.
- Persistencia de sesión.
- Cierre de sesión.
- Recuperación de contraseña.
- Cambio de contraseña desde deep link.

El deep link configurado es:

```text
gestcar://login-callback
```

En Supabase debe estar configurado en:

- Authentication > URL Configuration > Site URL.
- Authentication > URL Configuration > Redirect URLs.

## Configuración de Supabase

El cliente de Supabase está en:

```text
app/src/main/java/com/gestcar/datos/remoto/ClienteSupabase.kt
```

Actualmente contiene:

```kotlin
private const val SUPABASE_URL = "https://fghrbkgktlssvpbznfjk.supabase.co"
private const val SUPABASE_KEY = "sb_publishable_fy1CfFqjPeO0-SlkQxijcg_1_63QOH5"
```

La clave usada es una publishable/anon key. No debe confundirse con una service role key.

Si se clona el proyecto para otro entorno, hay que crear un proyecto de Supabase nuevo y reemplazar esos valores.

## Scripts SQL incluidos

El repositorio contiene scripts SQL de soporte:

```text
supabase_setup.sql
supabase_setup_funcionalidades.sql
supabase_actualizar_mantenimientos_estado.sql
supabase_eliminar_cuenta.sql
```

Como el proyecto ha evolucionado, el estado esperado actual de Supabase incluye también:

- `vehiculos` sin la columna antigua `anio`.
- `vehiculos.anio_fabricacion` como columna obligatoria.
- `mantenimientos.realizado`.
- `mantenimientos.fecha_realizado`.
- Función `public.eliminar_cuenta_actual()`.

### Script final de puesta al día

Si el proyecto de Supabase viene de una versión anterior, ejecutar este script una vez en SQL Editor:

```sql
begin;

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = 'public'
          and table_name = 'vehiculos'
          and column_name = 'anio'
    ) then
        execute '
            update public.vehiculos
            set anio_fabricacion = coalesce(anio_fabricacion, anio)
            where anio_fabricacion is null
        ';
    end if;
end $$;

update public.vehiculos
set anio_fabricacion = extract(year from now())::integer
where anio_fabricacion is null;

alter table public.vehiculos
alter column anio_fabricacion set not null;

alter table public.vehiculos
drop column if exists anio;

alter table public.mantenimientos
add column if not exists realizado boolean not null default true;

alter table public.mantenimientos
add column if not exists fecha_realizado bigint;

create index if not exists idx_repostajes_vehiculo_id
on public.repostajes(vehiculo_id);

create index if not exists idx_mantenimientos_vehiculo_id
on public.mantenimientos(vehiculo_id);

create index if not exists idx_gastos_periodicos_vehiculo_id
on public.gastos_periodicos(vehiculo_id);

create index if not exists idx_recordatorios_vehiculo_id
on public.recordatorios(vehiculo_id);

create or replace function public.eliminar_cuenta_actual()
returns void
language plpgsql
security definer
set search_path = public, auth, storage
as $$
declare
    usuario_actual uuid := auth.uid();
begin
    if usuario_actual is null then
        raise exception 'no hay usuario autenticado';
    end if;

    delete from storage.objects
    where bucket_id = 'vehiculos'
      and (
          owner::text = usuario_actual::text
          or name like usuario_actual::text || '/%'
      );

    delete from public.vehiculos
    where usuario_id = usuario_actual;

    delete from auth.users
    where id = usuario_actual;
end;
$$;

revoke all on function public.eliminar_cuenta_actual() from public;
grant execute on function public.eliminar_cuenta_actual() to authenticated;

commit;
```

Después de ejecutar este script, se debe usar una APK generada con el código actual, porque las versiones antiguas de la app podían enviar la columna legacy `anio`.

## Configuración de email transaccional

Supabase Auth necesita enviar correos para:

- Confirmación de registro.
- Recuperación de contraseña.

En este proyecto se configuró SMTP externo con Brevo para evitar límites bajos del correo integrado de Supabase.

La configuración se realiza desde:

```text
Supabase Dashboard > Authentication > Email > SMTP Settings
```

Hay que configurar:

- Sender email.
- Sender name.
- Host SMTP.
- Puerto.
- Usuario SMTP.
- Contraseña SMTP.

La app no necesita cambios de código si se cambia de proveedor SMTP. Es una configuración del proyecto de Supabase.

## Navegación

La app usa Navigation Compose.

Pantallas principales con bottom bar:

- Vehículos.
- Gastos.
- Repostajes.
- Mantenimiento.
- Más.

Pantallas secundarias:

- Formularios.
- Detalles.
- Recordatorios.
- Estadísticas placeholder.
- Cuenta.
- Login.
- Registro.
- Restablecer contraseña.

La navegación está centralizada en:

```text
ui/navegacion/Rutas.kt
ui/navegacion/GrafoNavegacion.kt
```

Las transiciones entre pantallas usan un fade corto para que el cambio sea suave sin hacer lenta la app.

## Pantallas principales

### Vehículos

Permite:

- Crear vehículos.
- Editar vehículos.
- Eliminar vehículos.
- Ver detalle del vehículo.
- Añadir o cambiar foto.
- Organizar vehículos habituales y secundarios.

El primer vehículo habitual se usa como vehículo por defecto en selectores.

### Repostajes

Permite:

- Registrar repostajes.
- Editar repostajes.
- Eliminar repostajes.
- Ver detalle del repostaje.
- Filtrar por periodo.
- Calcular consumo medio.
- Calcular coste medio cada 100 km.

### Mantenimientos

Permite:

- Registrar mantenimientos.
- Registrar reparaciones.
- Marcar operaciones como pendientes o realizadas.
- Editar operaciones.
- Eliminar operaciones.
- Ver detalle.

No se fuerza una lista cerrada de componentes, porque cada vehículo puede tener averías muy distintas.

### Gastos

Permite:

- Registrar gastos periódicos.
- Registrar gastos únicos.
- Controlar vencimientos.
- Marcar gastos como pagados.
- Crear nuevos avisos según periodicidad.
- Consultar histórico de pagados.

Ejemplos de gastos:

- ITV.
- Seguro.
- Impuesto de circulación.
- Parking.
- Peajes.
- Lavados.

### Recordatorios

Permite:

- Crear recordatorios por fecha.
- Crear recordatorios por kilometraje.
- Crear recordatorios combinados.
- Marcar como completado.
- Editar.
- Eliminar.

### Más

Incluye:

- Cuenta.
- Recordatorios.
- Estadísticas.
- Exportar datos.
- Cerrar sesión.

Algunas funciones como estadísticas completas y exportación final todavía pueden ampliarse en futuras iteraciones.

## Validación y formato de datos

### Matrículas

La app incluye un normalizador de matrículas españolas.

Reconoce:

- Formato actual.
- Formatos provinciales antiguos.
- Históricas.
- Ciclomotores.
- Remolques.
- Especiales.
- Temporales civiles.

Por criterio de privacidad y seguridad, no se normalizan formatos diplomáticos ni de Fuerzas del Estado.

Si la matrícula no se reconoce, la app no bloquea al usuario automáticamente. Se puede pedir confirmación para introducirla igualmente.

### Fechas

Las fechas se almacenan como `Long`, usando timestamp en milisegundos.

Esto se aplica a:

- Fecha de alta.
- Fecha de repostaje.
- Fecha de mantenimiento.
- Fecha de gasto.
- Fecha de vencimiento.
- Fecha de recordatorio.
- Fecha de completado.

### Importes y decimales

Los campos numéricos permiten introducir coma o punto decimal. Internamente se normalizan para evitar errores con teclados móviles.

La UI muestra los importes formateados como euros.

## Tema visual

La app usa Material Design 3 con paleta personalizada.

Colores principales:

- Azul oscuro: `#1F4E79`.
- Azul claro: `#4A90D9`.
- Verde: `#2E8B57`.
- Ámbar: `#F9A825`.
- Rojo error: `#B3261E`.

La barra superior es personalizada para ahorrar espacio vertical y mostrar el banner de conectividad de forma consistente.

## Compilar y ejecutar

### Desde Android Studio

1. Abrir Android Studio.
2. Seleccionar `File > Open`.
3. Elegir la carpeta del proyecto.
4. Esperar a que Gradle sincronice.
5. Ejecutar en emulador o dispositivo físico.

### Desde terminal

En Windows:

```powershell
.\gradlew.bat assembleDebug
```

El APK debug queda en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para release sin firmar:

```powershell
.\gradlew.bat assembleRelease
```

El APK release sin firmar queda en:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Para instalar en un móvil normal, el APK debe estar firmado. El debug APK ya viene firmado con la clave debug de Android.

## Qué archivos subir al entregar el proyecto

Para entregar el proyecto como código fuente, se debe subir:

- `app/`
- `gradle/`
- `build.gradle.kts`
- `settings.gradle.kts`
- `gradle.properties`
- `gradlew`
- `gradlew.bat`
- `README.md`
- Scripts `.sql` necesarios.
- Documentación propia si se desea, como PDF de análisis o diseño.

No se deben subir:

- `.gradle/`
- `.idea/`
- `.kotlin/`
- `build/`
- `app/build/`
- `local.properties`
- Bases de datos debug locales.
- APKs generados si la entrega pide solo código fuente.

## Estado pendiente o ampliable

Aunque la app ya tiene una base funcional amplia, todavía hay partes que pueden ampliarse:

- Estadísticas completas con gráficos.
- Exportación CSV definitiva.
- Notificaciones del sistema para vencimientos y recordatorios.
- Preferencias de cuenta más avanzadas.
- Historial de sesiones o dispositivos.
- Ajustes de sincronización visibles para el usuario.
- Tests automatizados.
- Firma release final para distribución.

## Notas importantes de seguridad

- No introducir nunca `service_role` en el código Android.
- No convertir el bucket de fotos en público.
- Mantener RLS activado en todas las tablas.
- Mantener las políticas de Storage limitadas por carpeta de usuario.
- Mantener la eliminación de cuenta como función SQL segura, no como operación privilegiada desde la app.
- Evitar mostrar identificadores internos de Supabase al usuario final.

## Resumen técnico

GestCar combina una base local Room con Supabase como backend remoto. La app está diseñada para que el dato local sea la primera garantía de persistencia, y Supabase actúe como sincronización remota entre dispositivos.

El diseño prioriza:

- Funcionamiento offline.
- Seguridad de datos personales.
- Separación clara entre UI, ViewModel, repositorio y datos.
- Sincronización robusta.
- Interfaz sencilla para usuarios no técnicos.
- Código comentado para facilitar revisión académica.
