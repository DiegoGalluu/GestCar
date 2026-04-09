package com.gestcar.datos.remoto

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

// cliente de supabase, es el punto de conexion con el servidor remoto
// aqui se configura la url del proyecto y la clave anonima
// IMPORTANTE, tienes que poner tu propia url y clave de tu proyecto de supabase
object ClienteSupabase {

    // pon aqui la url de tu proyecto de supabase
    // la encuentras en supabase dashboard > settings > api > project url
    private const val SUPABASE_URL = "https://fghrbkgktlssvpbznfjk.supabase.co"

    // pon aqui la clave anonima (anon key) de tu proyecto
    // la encuentras en supabase dashboard > settings > api > anon public
    private const val SUPABASE_KEY = "sb_publishable_fy1CfFqjPeO0-SlkQxijcg_1_63QOH5"

    // creamos el cliente con los modulos de autenticacion y base de datos
    val cliente = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        // modulo de autenticacion para login y registro
        install(Auth)

        // modulo de postgrest para hacer consultas a la base de datos remota
        install(Postgrest)
    }
}
