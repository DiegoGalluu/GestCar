package com.gestcar.datos.repositorio

import com.gestcar.datos.remoto.Gasolinera
import com.gestcar.datos.remoto.PrecioCombustibleGasolinera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

class GasolineraRepositorio {

    private val json = Json { ignoreUnknownKeys = true }

    private val combustiblesVisibles = listOf(
        "SP95" to listOf("Precio Gasolina 95 E5", "Precio Gasolina 95 E10"),
        "SP98" to listOf("Precio Gasolina 98 E5", "Precio Gasolina 98 E10"),
        "Diésel" to listOf("Precio Gasoleo A"),
        "Diésel+" to listOf("Precio Gasoleo Premium"),
        "GLP" to listOf("Precio Gases licuados del petróleo"),
        "GNC" to listOf("Precio Gas Natural Comprimido"),
        "GNL" to listOf("Precio Gas Natural Licuado"),
        "Biodiésel" to listOf("Precio Biodiesel"),
        "Bioetanol" to listOf("Precio Bioetanol")
    )

    suspend fun obtenerGasolineras(): Result<List<Gasolinera>> = withContext(Dispatchers.IO) {
        runCatching {
            val conexion = URL(ENDPOINT_GASOLINERAS).openConnection() as HttpURLConnection
            conexion.connectTimeout = 15000
            conexion.readTimeout = 20000
            conexion.requestMethod = "GET"

            try {
                val respuesta = conexion.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val raiz = json.parseToJsonElement(respuesta).jsonObject
                raiz["ListaEESSPrecio"]
                    ?.jsonArray
                    ?.mapNotNull { elemento -> elemento.jsonObject.aGasolinera() }
                    ?.distinctBy { gasolinera -> gasolinera.id }
                    .orEmpty()
            } finally {
                conexion.disconnect()
            }
        }
    }

    private fun JsonObject.aGasolinera(): Gasolinera? {
        val tipoVenta = texto("Tipo Venta")
        if (tipoVenta.isNotBlank() && !tipoVenta.equals("P", ignoreCase = true)) return null

        val latitud = texto("Latitud").aNumeroDecimal() ?: return null
        val longitud = texto("Longitud (WGS84)").aNumeroDecimal() ?: return null
        val id = texto("IDEESS").ifBlank {
            "${texto("Rótulo")}-${texto("Dirección")}-$latitud-$longitud"
        }

        val precios = combustiblesVisibles.mapNotNull { (nombre, claves) ->
            claves.firstNotNullOfOrNull { clave -> texto(clave).aNumeroDecimal() }?.let { precio ->
                PrecioCombustibleGasolinera(nombre = nombre, precio = precio)
            }
        }
        if (precios.isEmpty()) return null

        return Gasolinera(
            id = id,
            rotulo = texto("Rótulo").ifBlank { "Gasolinera" },
            direccion = texto("Dirección"),
            municipio = texto("Municipio"),
            provincia = texto("Provincia"),
            horario = texto("Horario"),
            latitud = latitud,
            longitud = longitud,
            precios = precios
        )
    }

    private fun JsonObject.texto(clave: String): String =
        this[clave]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()

    private fun String.aNumeroDecimal(): Double? =
        replace(",", ".")
            .replace(" ", "")
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()

    companion object {
        private const val ENDPOINT_GASOLINERAS =
            "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/"
    }
}
