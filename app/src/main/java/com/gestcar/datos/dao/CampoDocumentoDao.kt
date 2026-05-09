package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gestcar.datos.entidades.CampoDocumento
import kotlinx.coroutines.flow.Flow

@Dao
interface CampoDocumentoDao {

    @Query("SELECT * FROM campos_documento WHERE documentoId = :documentoId ORDER BY orden ASC")
    fun obtenerPorDocumento(documentoId: String): Flow<List<CampoDocumento>>

    @Query("SELECT * FROM campos_documento WHERE documentoId = :documentoId ORDER BY orden ASC")
    suspend fun obtenerPorDocumentoLista(documentoId: String): List<CampoDocumento>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarTodos(campos: List<CampoDocumento>)

    @Query("DELETE FROM campos_documento WHERE documentoId = :documentoId")
    suspend fun eliminarPorDocumento(documentoId: String)
}
