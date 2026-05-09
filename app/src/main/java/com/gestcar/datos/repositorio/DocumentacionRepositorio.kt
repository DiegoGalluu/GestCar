package com.gestcar.datos.repositorio

import com.gestcar.datos.dao.CampoDocumentoDao
import com.gestcar.datos.dao.DocumentoVehiculoDao
import com.gestcar.datos.entidades.CampoDocumento
import com.gestcar.datos.entidades.DocumentoVehiculo
import kotlinx.coroutines.flow.Flow

class DocumentacionRepositorio(
    private val documentoDao: DocumentoVehiculoDao,
    private val campoDao: CampoDocumentoDao
) {

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

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(documento: DocumentoVehiculo): Result<Unit> {
        return try {
            documentoDao.eliminar(documento)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
