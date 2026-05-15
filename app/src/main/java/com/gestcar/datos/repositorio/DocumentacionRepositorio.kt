package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.AdjuntoDocumentoDao
import com.gestcar.datos.dao.CampoDocumentoDao
import com.gestcar.datos.dao.DocumentoVehiculoDao
import com.gestcar.datos.dao.VehiculoDao
import com.gestcar.datos.entidades.AdjuntoDocumento
import com.gestcar.datos.entidades.CampoDocumento
import com.gestcar.datos.entidades.DocumentoVehiculo
import com.gestcar.datos.remoto.AdjuntoDocumentoDto
import com.gestcar.datos.remoto.CampoDocumentoDto
import com.gestcar.datos.remoto.ClienteSupabase
import com.gestcar.datos.remoto.DocumentoVehiculoDto
import com.gestcar.datos.remoto.aDto
import com.gestcar.datos.remoto.aEntidad
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.time.Duration.Companion.hours

class DocumentacionRepositorio(
    private val documentoDao: DocumentoVehiculoDao,
    private val campoDao: CampoDocumentoDao,
    private val adjuntoDao: AdjuntoDocumentoDao,
    private val vehiculoDao: VehiculoDao
) {
    private val tablaDocumentosRemota = "documentos_vehiculo"
    private val tablaCamposRemota = "campos_documento"
    private val tablaAdjuntosRemota = "adjuntos_documento"
    private val tablaVehiculosRemota = "vehiculos"

    fun obtenerDocumentos(vehiculoId: String): Flow<List<DocumentoVehiculo>> {
        return documentoDao.obtenerPorVehiculo(vehiculoId)
    }

    fun obtenerCampos(documentoId: String): Flow<List<CampoDocumento>> {
        return campoDao.obtenerPorDocumento(documentoId)
    }

    fun obtenerAdjuntos(documentoId: String): Flow<List<AdjuntoDocumento>> {
        return adjuntoDao.obtenerPorDocumento(documentoId)
    }

    suspend fun obtenerDocumentoPorId(documentoId: String): DocumentoVehiculo? {
        return documentoDao.obtenerPorId(documentoId)
    }

    suspend fun obtenerCamposLista(documentoId: String): List<CampoDocumento> {
        return campoDao.obtenerPorDocumentoLista(documentoId)
    }

    suspend fun obtenerAdjuntosLista(documentoId: String): List<AdjuntoDocumento> {
        return adjuntoDao.obtenerPorDocumentoLista(documentoId)
    }

    suspend fun guardarAdjunto(adjunto: AdjuntoDocumento): Result<Unit> {
        return try {
            val adjuntoActualizado = adjunto.copy(actualizadoEn = System.currentTimeMillis())
            adjuntoDao.insertar(adjuntoActualizado)
            runCatching {
                withTimeoutOrNull(TIEMPO_MAXIMO_SYNC_RAPIDA_MS) {
                    val adjuntoSincronizado = sincronizarAdjunto(adjuntoActualizado)
                    adjuntoDao.insertar(adjuntoSincronizado)
                    tocarDocumentoPadre(adjuntoSincronizado.documentoId, adjuntoSincronizado.actualizadoEn)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAdjunto(adjunto: AdjuntoDocumento): Result<Unit> {
        return try {
            adjuntoDao.eliminar(adjunto)
            runCatching {
                withTimeoutOrNull(TIEMPO_MAXIMO_SYNC_RAPIDA_MS) {
                    eliminarAdjuntoRemoto(adjunto)
                    tocarDocumentoPadre(adjunto.documentoId, System.currentTimeMillis())
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guardar(
        documento: DocumentoVehiculo,
        campos: List<CampoDocumento>
    ): Result<Unit> {
        return try {
            val ahora = System.currentTimeMillis()
            val documentoLimpio = documento.copy(
                titulo = documento.titulo.trim(),
                notas = documento.notas?.trim().orEmpty(),
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
                    documento.actualizadoEn > documentoLocal.actualizadoEn

                if (remotoEsMasNuevo) {
                    documentoDao.insertar(documento)
                    descargarCamposDocumento(documento.id)
                }

                runCatching {
                    descargarAdjuntosDocumento(documento.id)
                }
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

        runCatching {
            sincronizarAdjuntosDocumento(documento.id)
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

    private suspend fun obtenerAdjuntosRemotos(documentoId: String): List<AdjuntoDocumentoDto> {
        return ClienteSupabase.cliente.postgrest[tablaAdjuntosRemota]
            .select { filter { eq("documento_id", documentoId) } }
            .decodeList<AdjuntoDocumentoDto>()
    }

    private suspend fun descargarCamposDocumento(documentoId: String) {
        val camposRemotos = obtenerCamposRemotos(documentoId)
            .map { it.aEntidad() }
            .sortedBy { it.orden }

        campoDao.eliminarPorDocumento(documentoId)
        campoDao.insertarTodos(camposRemotos)
    }

    private suspend fun descargarAdjuntosDocumento(documentoId: String) {
        val adjuntosRemotos = obtenerAdjuntosRemotos(documentoId)
            .map { dto ->
                val adjuntoLocal = adjuntoDao.obtenerPorId(dto.id)
                dto.aEntidad(uriLocal = adjuntoLocal?.uriLocal.orEmpty())
            }
            .sortedBy { it.orden }
        val idsRemotos = adjuntosRemotos.map { it.id }.toSet()
        val ahora = System.currentTimeMillis()

        adjuntoDao.obtenerPorDocumentoLista(documentoId)
            .filter { adjunto ->
                adjunto.id !in idsRemotos &&
                    !adjunto.rutaStorage.isNullOrBlank() &&
                    ahora - adjunto.actualizadoEn > MARGEN_ADJUNTO_RECIENTE_MS
            }
            .forEach { adjunto -> adjuntoDao.eliminarPorId(adjunto.id) }

        adjuntosRemotos.forEach { adjunto ->
            val adjuntoLocal = adjuntoDao.obtenerPorId(adjunto.id)
            val remotoEsMasNuevo = adjuntoLocal == null ||
                adjunto.actualizadoEn >= adjuntoLocal.actualizadoEn ||
                adjunto.rutaStorage != adjuntoLocal.rutaStorage

            if (remotoEsMasNuevo) {
                adjuntoDao.insertar(adjunto)
            }
        }
    }

    private suspend fun sincronizarAdjuntosDocumento(documentoId: String) {
        val adjuntosLocales = adjuntoDao.obtenerPorDocumentoLista(documentoId)
        adjuntosLocales.forEach { adjunto ->
            runCatching {
                val adjuntoSincronizado = sincronizarAdjunto(adjunto)
                adjuntoDao.insertar(adjuntoSincronizado)
            }
        }

        descargarAdjuntosDocumento(documentoId)
    }

    private suspend fun sincronizarAdjunto(adjunto: AdjuntoDocumento): AdjuntoDocumento {
        val rutaStorage = adjunto.rutaStorage ?: construirRutaStorage(adjunto)
        val adjuntoConRuta = adjunto.copy(
            rutaStorage = rutaStorage,
            actualizadoEn = if (adjunto.actualizadoEn == 0L) System.currentTimeMillis() else adjunto.actualizadoEn
        )

        subirArchivoAdjuntoSiExiste(adjuntoConRuta)

        ClienteSupabase.cliente.postgrest[tablaAdjuntosRemota]
            .upsert(
                value = adjuntoConRuta.aDto(),
                request = {
                    select(Columns.list("id"))
                }
            )

        return adjuntoConRuta
    }

    private suspend fun subirArchivoAdjuntoSiExiste(adjunto: AdjuntoDocumento) {
        val rutaLocal = android.net.Uri.parse(adjunto.uriLocal).path ?: return
        val archivo = File(rutaLocal)
        if (!archivo.exists()) {
            return
        }

        val rutaStorage = adjunto.rutaStorage ?: return
        ClienteSupabase.cliente.storage
            .from(ClienteSupabase.BUCKET_DOCUMENTOS_VEHICULO)
            .upload(rutaStorage, archivo.readBytes()) {
                upsert = true
            }
    }

    private suspend fun eliminarAdjuntoRemoto(adjunto: AdjuntoDocumento) {
        val rutaStorage = adjunto.rutaStorage
        if (!rutaStorage.isNullOrBlank()) {
            ClienteSupabase.cliente.storage
                .from(ClienteSupabase.BUCKET_DOCUMENTOS_VEHICULO)
                .delete(listOf(rutaStorage))
        }

        ClienteSupabase.cliente.postgrest[tablaAdjuntosRemota]
            .delete { filter { eq("id", adjunto.id) } }
    }

    private suspend fun tocarDocumentoPadre(documentoId: String, actualizadoEn: Long) {
        val documento = documentoDao.obtenerPorId(documentoId) ?: return
        documentoDao.insertar(documento.copy(actualizadoEn = actualizadoEn))

        ClienteSupabase.cliente.postgrest[tablaDocumentosRemota]
            .update(
                update = {
                    set("actualizado_en", actualizadoEn)
                },
                request = {
                    filter { eq("id", documentoId) }
                    select(Columns.list("id"))
                }
            )
    }

    suspend fun obtenerUrlFirmadaAdjunto(adjunto: AdjuntoDocumento): String? {
        val rutaStorage = adjunto.rutaStorage ?: return null
        return runCatching {
            ClienteSupabase.cliente.storage
                .from(ClienteSupabase.BUCKET_DOCUMENTOS_VEHICULO)
                .createSignedUrl(rutaStorage, 6.hours)
        }.getOrNull()
    }

    private fun construirRutaStorage(adjunto: AdjuntoDocumento): String {
        val extension = adjunto.nombreArchivo.substringAfterLast('.', missingDelimiterValue = "")
            .ifBlank {
                when {
                    adjunto.mimeType == "application/pdf" -> "pdf"
                    adjunto.mimeType.startsWith("image/") -> "jpg"
                    else -> "bin"
                }
            }

        return "${adjunto.documentoId}/${adjunto.id}.$extension"
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
        private const val MARGEN_ADJUNTO_RECIENTE_MS = 30_000L
    }
}
