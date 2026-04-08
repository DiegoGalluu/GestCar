# GestCar - Fase 1

Aplicacion Android para la gestion de mantenimiento y costes vehiculares.

## Que incluye esta fase

- Autenticacion con Supabase (login y registro con email)
- CRUD completo de vehiculos (crear, ver, editar, eliminar)
- Base de datos local con Room (funciona sin internet)
- Sincronizacion con Supabase (los datos se guardan en la nube)
- Navegacion con barra inferior (solo vehiculos funcional, el resto proximamente)
- Tema Material Design 3 con la paleta de colores del documento de diseno

## Como configurar

### 1. Crear proyecto en Supabase

1. Ve a [supabase.com](https://supabase.com) y crea una cuenta gratuita
2. Crea un nuevo proyecto (pon el nombre que quieras y una contrasena para la bd)
3. Espera a que se cree el proyecto (tarda unos segundos)

### 2. Crear la tabla de vehiculos

1. En el dashboard de supabase ve a **SQL Editor**
2. Crea una nueva query
3. Copia y pega el contenido del archivo `supabase_setup.sql`
4. Dale a **Run** para ejecutar el script

### 3. Configurar las credenciales en la app

1. En supabase ve a **Settings > API**
2. Copia la **Project URL** y la **anon public key**
3. Abre el archivo `app/src/main/java/com/gestcar/datos/remoto/ClienteSupabase.kt`
4. Reemplaza `TU_PROYECTO` y `TU_CLAVE_ANONIMA_AQUI` con tus valores

### 4. Abrir en Android Studio

1. Abre Android Studio
2. File > Open > selecciona la carpeta GestCar
3. Espera a que gradle sincronice las dependencias
4. Ejecuta la app en un emulador o dispositivo fisico

## Estructura del proyecto

```
com.gestcar/
  datos/
    entidades/     -> clases de datos (vehiculo, etc)
    dao/           -> interfaces de acceso a la base de datos local
    basedatos/     -> configuracion de room
    remoto/        -> cliente de supabase y dtos
    repositorio/   -> logica de datos (local + remoto)
  ui/
    tema/          -> colores y tema material 3
    navegacion/    -> rutas y grafo de navegacion
    pantallas/     -> pantallas de la app (login, lista, formulario, detalle)
    componentes/   -> componentes reutilizables (tarjeta vehiculo, etc)
    viewmodel/     -> viewmodels con la logica de presentacion
```

## Proximas fases

- Fase 2: Repostajes y mantenimientos
- Fase 3: Gastos periodicos y recordatorios
- Fase 4: Estadisticas, graficos y exportacion CSV
