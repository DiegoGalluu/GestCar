package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.VehiculoDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import com.gestcar.util.normalizarRutaImagenVehiculo
import java.io.File

// repositorio de vehiculos, es el intermediario entre la ui y los datos
// se encarga de guardar en local (room) y sincronizar con supabase
// la estrategia es guardar primero en local y luego intentar subir a supabase
// asi la app funciona siempre aunque no haya internet
class VehiculoRepositorio(
    private val vehiculoDao: VehiculoDao
) {
    // nombre de la tabla en supabase
    private val tablaRemota = "vehiculos"

    // obtener todos los vehiculos del usuario como flow reactivo
    // esto devuelve los datos locales de room
    fun obtenerVehiculos(usuarioId: String): Flow<List<Vehiculo>> {
        return vehiculoDao.obtenerVehiculosPorUsuario(usuarioId)
    }

    // obtener todos los vehiculos locales de una sola vez, util para sincronizar pendientes
    private suspend fun obtenerVehiculosLocales(usuarioId: String): List<Vehiculo> {
        return vehiculoDao.obtenerVehiculosPorUsuarioLista(usuarioId)
    }

    // obtener un vehiculo por su id desde la base de datos local
    suspend fun obtenerPorId(id: String): Vehiculo? {
        return vehiculoDao.obtenerPorId(id)
    }

    suspend fun prepararOrganizacionSiEsNuevo(vehiculo: Vehiculo): Vehiculo {
        if (vehiculoDao.obtenerPorId(vehiculo.id) != null) {
            return vehiculo
        }

        // el primer vehiculo pasa a ser habitual automaticamente
        // los siguientes quedan como secundarios hasta que el usuario los organice
        val vehiculos = obtenerVehiculosLocales(vehiculo.usuarioId)
        val hayHabituales = vehiculos.any { it.habitual }
        val siguienteOrden = (vehiculos.maxOfOrNull { it.ordenLista } ?: -1) + 1

        return vehiculo.copy(
            habitual = !hayHabituales,
            ordenLista = siguienteOrden
        )
    }

    suspend fun guardarOrganizacion(vehiculos: List<Vehiculo>) {
        // la organizacion es una preferencia de uso de la app
        // se guarda localmente para no depender de red al ordenar la lista
        vehiculoDao.actualizarVehiculos(vehiculos)
    }

    // guardar un vehiculo nuevo, primero en local y luego intenta subirlo a supabase
    suspend fun guardar(vehiculo: Vehiculo): Result<Unit> {
        return try {
            // guardamos en room primero para que la app sea responsive
            vehiculoDao.insertar(vehiculo)

            sincronizarVehiculo(vehiculo)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // actualizar un vehiculo existente en local y en remoto
    suspend fun actualizar(vehiculo: Vehiculo): Result<Unit> {
        return try {
            val vehiculoActualizado = vehiculo.copy(
                actualizadoEn = System.currentTimeMillis()
            )
            vehiculoDao.actualizar(vehiculoActualizado)

            sincronizarVehiculo(vehiculoActualizado)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // eliminar un vehiculo de local y de supabase
    suspend fun eliminar(vehiculo: Vehiculo): Result<Unit> {
        return try {
            vehiculoDao.eliminar(vehiculo)

            ClienteSupabase.cliente.postgrest[tablaRemota]
                .delete { filter { eq("id", vehiculo.id) } }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // sincronizar los vehiculos remotos con los locales
    // primero leemos supabase para evitar que un dispositivo con una copia vieja
    // vuelva a subir un vehiculo que ya fue borrado desde otro movil
    suspend fun sincronizar(usuarioId: String): Result<Unit> {
        try {
            val vehiculosLocales = obtenerVehiculosLocales(usuarioId)
            val vehiculosRemotos = ClienteSupabase.cliente.postgrest[tablaRemota]
                .select { filter { eq("usuario_id", usuarioId) } }
                .decodeList<VehiculoDto>()
            val vehiculosRemotosPorId = vehiculosRemotos.associateBy { it.id }

            vehiculosLocales.forEach { vehiculo ->
                val vehiculoRemoto = vehiculosRemotosPorId[vehiculo.id]
                when {
                    vehiculoRemoto == null -> {
                        vehiculoDao.eliminarPorId(vehiculo.id)
                    }

                    vehiculo.actualizadoEn > vehiculoRemoto.actualizadoEn -> {
                        sincronizarVehiculo(vehiculo)
                    }
                }
            }

            val vehiculosRemotosActualizados = ClienteSupabase.cliente.postgrest[tablaRemota]
                .select { filter { eq("usuario_id", usuarioId) } }
                .decodeList<VehiculoDto>()

            // guardamos cada vehiculo remoto en local, replace si ya existe
            vehiculosRemotosActualizados.forEach { dto ->
                val vehiculoLocal = vehiculoDao.obtenerPorId(dto.id)
                val vehiculoRemoto = dto.aEntidad()
                vehiculoDao.insertar(
                    if (vehiculoLocal != null) {
                        // respetamos el orden local elegido por el usuario
                        // supabase guarda los datos del vehiculo pero la prioridad visual es local
                        vehiculoRemoto.copy(
                            habitual = vehiculoLocal.habitual,
                            ordenLista = vehiculoLocal.ordenLista
                        )
                    } else {
                        vehiculoRemoto
                    }
                )
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun actualizarImagenVehiculo(vehiculo: Vehiculo, imagenUriLocal: String): Result<Vehiculo> {
        // primero enlazamos la foto local para que se vea al instante
        // luego intentamos subirla y sustituir la ruta por la ruta remota
        val vehiculoActualizado = vehiculo.copy(
            imagenUri = imagenUriLocal,
            actualizadoEn = System.currentTimeMillis()
        )

        return try {
            vehiculoDao.actualizar(vehiculoActualizado)
            val vehiculoSincronizado = sincronizarVehiculo(vehiculoActualizado)
            Result.success(vehiculoSincronizado)
        } catch (e: Exception) {
            Result.success(vehiculoActualizado)
        }
    }

    private suspend fun sincronizarVehiculo(vehiculo: Vehiculo): Vehiculo {
        // antes de subir el vehiculo comprobamos si su imagen todavia es un archivo local
        // si lo es, la subimos al bucket privado y guardamos la ruta remota
        val vehiculoPreparado = subirImagenSiHaceFalta(vehiculo)
        ClienteSupabase.cliente.postgrest[tablaRemota]
            .upsert(
                value = vehiculoPreparado.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
        return vehiculoPreparado
    }

    private suspend fun subirImagenSiHaceFalta(vehiculo: Vehiculo): Vehiculo {
        val imagenUri = normalizarRutaImagenVehiculo(vehiculo.imagenUri) ?: return vehiculo
        if (!imagenUri.startsWith("file://")) {
            // si no empieza por file ya es una ruta remota o una ruta normalizada
            // no hay nada pesado que subir en este momento
            return vehiculo.copy(imagenUri = imagenUri)
        }

        val archivoImagen = File(requireNotNull(android.net.Uri.parse(imagenUri).path))
        if (!archivoImagen.exists()) {
            // si la foto local ya no existe evitamos dejar una ruta rota
            // es mejor volver al icono por defecto que mostrar una imagen inexistente
            return vehiculo.copy(imagenUri = null)
        }

        // ruta por usuario y vehiculo
        // asi no mezclamos fotos de usuarios diferentes dentro del mismo bucket
        val rutaRemota = "${vehiculo.usuarioId}/${vehiculo.id}.jpg"
        val bucket = ClienteSupabase.cliente.storage.from(ClienteSupabase.BUCKET_FOTOS_VEHICULOS)
        bucket.upload(rutaRemota, archivoImagen.readBytes()) {
            upsert = true
        }

        val vehiculoConImagenRemota = vehiculo.copy(
            imagenUri = rutaRemota,
            actualizadoEn = System.currentTimeMillis()
        )
        vehiculoDao.actualizar(vehiculoConImagenRemota)
        return vehiculoConImagenRemota
    }
}
