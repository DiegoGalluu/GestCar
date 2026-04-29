# GestCar

Aplicacion Android para la gestion de vehiculos, mantenimientos y costes asociados.

## Funcionalidades

- Autenticacion con Supabase mediante email y contrasena
- Recuperacion de contrasena con deep links
- Persistencia de sesion
- CRUD completo de vehiculos
- Fotos de vehiculos desde camara o galeria
- Gestion de repostajes
- Gestion de mantenimientos y reparaciones
- Gestion de gastos periodicos con control de pagos y vencimientos
- Recordatorios por fecha o kilometraje
- Base de datos local con Room para uso offline
- Sincronizacion con Supabase
- Storage privado para imagenes
- Navegacion con barra inferior
- Tema Material Design 3 con paleta personalizada

## Como configurar

### 1. Crear proyecto en Supabase

1. Ve a [supabase.com](https://supabase.com) y crea una cuenta gratuita
2. Crea un nuevo proyecto (pon el nombre que quieras y una contrasena para la bd)
3. Espera a que se cree el proyecto (tarda unos segundos)

### 2. Crear la base de datos remota

1. En el dashboard de supabase ve a **SQL Editor**
2. Crea una nueva query
3. Copia y pega el contenido del archivo `supabase_setup.sql`
4. Dale a **Run** para crear la estructura inicial
5. Copia y pega el contenido del archivo `supabase_setup_funcionalidades.sql`
6. Dale a **Run** para crear las tablas de repostajes, mantenimientos, gastos y recordatorios

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
    pantallas/     -> pantallas de la app
    componentes/   -> componentes reutilizables (tarjeta vehiculo, etc)
    viewmodel/     -> viewmodels con la logica de presentacion
```
