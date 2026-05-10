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
            runCatching { sincronizarDocumento(documentoLimpio, camposLimpios) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(documento: DocumentoVehiculo): Result<Unit> {
        return try {
            documentoDao.eliminar(documento)

            runCatching {
                ClienteSupabase.cliente.postgrest[tablaDocumentosRemota]
                    .delete { filter { eq("id", documento.id) } }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizar(vehiculoId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()

            // subimos primero para proteger los campos creados sin cobertura
            // despues descargamos para traer cambios de otros dispositivos
            documentoDao.obtenerPorVehiculoLista(vehiculoId).forEach { documento ->
                val campos = campoDao.obtenerPorDocumentoLista(documento.id)
                runCatching { sincronizarDocumento(documento, campos) }
                    .onFailure { errores.add(it) }
            }

            if (errores.isNotEmpty()) {
                return Result.failure(errores.first())
            }

            val documentosRemotos = ClienteSupabase.cliente.postgrest[tablaDocumentosRemota]
                .select { filter { eq("vehiculo_id", vehiculoId) } }
                .decodeList<DocumentoVehiculoDto>()

            documentosRemotos.forEach { documentoDto ->
                val documento = documentoDto.aEntidad()
                documentoDao.insertar(documento)

                // los campos se sustituyen por la copia remota del documento
                // como antes hemos subido lo local evitamos borrar cambios pendientes
                val camposRemotos = ClienteSupabase.cliente.postgrest[tablaCamposRemota]
                    .select { filter { eq("documento_id", documento.id) } }
                    .decodeList<CampoDocumentoDto>()
                    .map { it.aEntidad() }
                    .sortedBy { it.orden }

                campoDao.eliminarPorDocumento(documento.id)
                campoDao.insertarTodos(camposRemotos)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizarPendientesDelUsuario(usuarioId: String): Result<Unit> {
        return try {
            val errores = mutableListOf<Throwable>()
            vehiculoDao.obtenerVehiculosPorUsuarioLista(usuarioId).forEach { vehiculo ->
                documentoDao.obtenerPorVehiculoLista(vehiculo.id).forEach { documento ->
                    val campos = campoDao.obtenerPorDocumentoLista(documento.id)
                    runCatching { sincronizarDocumento(documento, campos) }
                        .onFailure { errores.add(it) }
                }
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

        ClienteSupabase.cliente.postgrest[tablaCamposRemota]
            .delete { filter { eq("documento_id", documento.id) } }

        campos.forEach { campo ->
            ClienteSupabase.cliente.postgrest[tablaCamposRemota]
                .upsert(
                    value = campo.aDto(),
                    request = {
                        select(Columns.list("id"))
                    }
                )
        }
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
}
