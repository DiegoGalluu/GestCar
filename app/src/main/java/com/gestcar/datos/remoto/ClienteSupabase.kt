package com.gestcar.datos.remoto

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

// cliente de supabase, es el punto de conexion con el servidor remoto
// aqui se configura la url del proyecto y la clave anonima
// IMPORTANTE, tienes que poner tu propia url y clave de tu proyecto de supabase
object ClienteSupabase {

    // bucket privado donde se guardan las fotos de los vehiculos
    // no se usa un bucket publico porque las matriculas y fotos pueden ser datos sensibles
    const val BUCKET_FOTOS_VEHICULOS = "vehiculos"

    // bucket privado para adjuntos de documentacion
    // aqui pueden vivir pdfs o fotos con datos sensibles del usuario
    const val BUCKET_DOCUMENTOS_VEHICULO = "documentos"

    // enlace profundo que permite volver a la app tras confirmar correo o resetear contrasena
    // debe coincidir con la configuracion de supabase auth y con el intent filter de android
    const val AUTH_DEEP_LINK = "gestcar://login-callback"

    // pon aqui la url de tu proyecto de supabase
    // la encuentras en supabase dashboard > settings > api > project url
    const val SUPABASE_URL = "https://fghrbkgktlssvpbznfjk.supabase.co"

    // pon aqui la clave anonima (anon key) de tu proyecto
    // la encuentras en supabase dashboard > settings > api > anon public
    // esta clave es publica y no permite saltarse rls ni acceder como administrador
    const val SUPABASE_KEY = "sb_publishable_fy1CfFqjPeO0-SlkQxijcg_1_63QOH5"

    // creamos un unico cliente compartido
    // asi todos los repositorios usan la misma sesion de supabase y la misma configuracion
    val cliente = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // modulo de autenticacion para login y registro
        install(Auth) {
            scheme = "gestcar"
            host = "login-callback"
        }

        // modulo de postgrest para hacer consultas a la base de datos remota
        install(Postgrest)

        // modulo de storage para subir fotos de vehiculos
        install(Storage)
    }
}
