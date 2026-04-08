package com.gestcar

import android.app.Application

// clase de aplicacion principal, se usa para inicializar cosas globales
// por ahora no hace nada especial pero la necesitamos para que room
// y supabase tengan acceso al contexto de la app
class GestCarAplicacion : Application()
