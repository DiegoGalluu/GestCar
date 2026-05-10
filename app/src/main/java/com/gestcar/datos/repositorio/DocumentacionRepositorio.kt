package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.CampoDocumentoDao
import com.gestcar.datos.dao.DocumentoVehiculoDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.CampoDocumento
import com.gestcar.datos.entidades.DocumentoVehiculo
import com.gestcar.datos.remoto.CampoDocumentoDto
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.DocumentoVehiculoDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull

class DocumentacionRepositorio(
    private val documentoDao: DocumentoVehiculoDao,
    private val campoDao: CampoDocumentoDao,
    private val vehiculoDao: VehiculoDao
) {
    private val tablaDocumentosRemota = "documentos_vehiculo"
    private val tablaCamposRemota = "campos_documento"
    private val tablaVehiculosRemota = "vehiculos"

    fun obtenerDocumentos(vehiculoId: String): Flow<List<DocumentoVehiculo>> {
        return documentoDao.obtenerPorVehiculo(vehiculoId)
    }

    fun obtenerCampos(documentoId: String): Flow<List<CampoDocumento>> {
        return campoDao.obtenerPorDocumento(documentoId)
    }

    suspend fun obtenerDocumentoPorId(documentoId: String): DocumentoVehiculo? {
        return documentoDao.obtenerPorId(documentoId)
    }

    suspend fun obtenerCamposLista(documentoId: String): List<CampoDocumento> {
        return campoDao.obtenerPorDocumentoLista(documentoId)
    }

    suspend fun guardar(
        documento: DocumentoVehiculo,
        campos: List<CampoDocumento>
    ): Result<Unit> {
        return try {
            val ahora = System.currentTimeMillis()
            val documentoLimpio = documento.copy(
                titulo = documento.titulo.trim(),
                notas = documento.notas?.trim()?.takeIf { it.isNotBlank() },
                actualizadoEn = ahora
            )

            val camposLimpios = campos
                .filter { it.nombre.isNotBlank() || it.valor.isNotBlank() }
                .mapIndexed { indice, campo ->
                    campo.copy(
                        documentoId = documento.id,
                        nombre = campo.nombre.trim(),
                        valor = campo.valor.trim(),
                        orden = indice,
                        actualizadoEn = ahora
                    )
                }

            documentoDao.insertar(documentoLimpio)
            campoDao.eliminarPorDocumento(documento.id)
            campoDao.insertarTodos(camposLimpios)

            // documentacion tambien sigue el modelo offline first
            // si supabase no responde el dato queda local y se subira despues
            runCatching {
                withTimeoutOrNull(TIEMPO_MAXIMO_SYNC_RAPIDA_MS) {
                    sincronizarDocumento(documentoLimpio, camposLimpios)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(documento: DocumentoVehiculo): Result<Unit> {
        return try {
            documentoDao.eliminar(documento)

            runCatching {
                withTimeoutOrNull(TIEMPO_MAXIMO_SYNC_RAPIDA_MS) {
                    ClienteSupabase.cliente.postgrest[tablaDocumentosRemota]
                        .delete { filter { eq("id", documento.id) } }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(vehiculoId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()
            val documentosLocales = documentoDao.obtenerPorVehiculoLista(vehiculoId)
            val documentosRemotosIniciales = runCatching {
                obtenerDocumentosRemotos(vehiculoId)
            }.getOrElse { emptyList() }
            val documentosRemotosPorId = documentosRemotosIniciales.associateBy { it.id }

            // no subimos a ciegas porque otro dispositivo puede tener una version mas nueva
            // si la copia local es antigua no debe borrar campos remotos recien creados
            documentosLocales.forEach { documento ->
                val documentoRemoto = documentosRemotosPorId[documento.id]
                val localEsMasNuevo = documentoRemoto == null ||
                    documento.actualizadoEn > documentoRemoto.actualizadoEn

                if (!localEsMasNuevo) {
                    return@forEach
                }

                val campos = campoDao.obtenerPorDocumentoLista(documento.id)
                runCatching { sincronizarDocumento(documento, campos) }
                    .onFailure { errores.add(it) }
            }

            val documentosRemotos = runCatching {
                obtenerDocumentosRemotos(vehiculoId)
            }.getOrElse { documentosRemotosIniciales }

            documentosRemotos.forEach { documentoDto ->
                val documento = documentoDto.aEntidad()
                val documentoLocal = documentoDao.obtenerPorId(documento.id)
                val remotoEsMasNuevo = documentoLocal == null ||
                    documento.actualizadoEn >= documentoLocal.actualizadoEn

                if (!remotoEsMasNuevo) {
                    return@forEach
                }

                documentoDao.insertar(documento)
                descargarCamposDocumento(documento.id)
            }

            if (errores.isEmpty()) {
                Result.success(Unit)
            } else {
                Result.failure(errores.first())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizarPendientesDelUsuario(usuarioId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()
            vehiculoDao.obtenerVehiculosPorUsuarioLista(usuarioId).forEach { vehiculo ->
                sincronizar(vehiculo.id).onFailure { errores.add(it) }
            }

            if (errores.isNotEmpty()) {
                return Result.failure(errores.first())
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sincronizarDocumento(
        documento: DocumentoVehiculo,
        campos: List<CampoDocumento>
    ) {
        sincronizarVehiculoPadreSiHaceFalta(documento.vehiculoId)

        ClienteSupabase.cliente.postgrest[tablaDocumentosRemota]
            .upsert(
                value = documento.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )

        campos.forEach { campo ->
            ClienteSupabase.cliente.postgrest[tablaCamposRemota]
                .upsert(
                    value = campo.aDto(),
                    request = {
                        select(Columns.list("id"))
                    }
                )
        }

        // borramos solo los campos remotos que ya no existen en local
        // asi evitamos el patron peligroso de borrar todo antes de volver a subir
        val idsLocales = campos.map { it.id }.toSet()
        obtenerCamposRemotos(documento.id)
            .filterNot { it.id in idsLocales }
            .forEach { campoRemoto ->
                ClienteSupabase.cliente.postgrest[tablaCamposRemota]
                    .delete { filter { eq("id", campoRemoto.id) } }
            }
    }

    private suspend fun obtenerDocumentosRemotos(vehiculoId: String): List<DocumentoVehiculoDto> {
        return ClienteSupabase.cliente.postgrest[tablaDocumentosRemota]
            .select { filter { eq("vehiculo_id", vehiculoId) } }
            .decodeList<DocumentoVehiculoDto>()
    }

    private suspend fun obtenerCamposRemotos(documentoId: String): List<CampoDocumentoDto> {
        return ClienteSupabase.cliente.postgrest[tablaCamposRemota]
            .select { filter { eq("documento_id", documentoId) } }
            .decodeList<CampoDocumentoDto>()
    }

    private suspend fun descargarCamposDocumento(documentoId: String) {
        val camposRemotos = obtenerCamposRemotos(documentoId)
            .map { it.aEntidad() }
            .sortedBy { it.orden }

        campoDao.eliminarPorDocumento(documentoId)
        campoDao.insertarTodos(camposRemotos)
    }

    private suspend fun sincronizarVehiculoPadreSiHaceFalta(vehiculoId: String) {
        val vehiculo = vehiculoDao.obtenerPorId(vehiculoId) ?: return

        // las reglas rls de documentacion cuelgan del vehiculo
        // por eso garantizamos que el padre exista antes de subir documentos
        ClienteSupabase.cliente.postgrest[tablaVehiculosRemota]
            .upsert(
                value = vehiculo.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )
    }

    companion object {
        private const val TIEMPO_MAXIMO_SYNC_RAPIDA_MS = 4_000L
    }
}
