package com.gestcar.datos.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gestcar.datos.entidades.AdjuntoDocumento
import kotlinx.coroutines.flow.Flow

@Dao
interface AdjuntoDocumentoDao {

    @Query("SELECT * FROM adjuntos_documento WHERE documentoId = :documentoId ORDER BY orden ASC, fechaAlta ASC")
    fun obtenerPorDocumento(documentoId: String): Flow<List<AdjuntoDocumento>>

    @Query("SELECT * FROM adjuntos_documento WHERE documentoId = :documentoId ORDER BY orden ASC, fechaAlta ASC")
    suspend fun obtenerPorDocumentoLista(documentoId: String): List<AdjuntoDocumento>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(adjunto: AdjuntoDocumento)

    @Delete
    suspend fun eliminar(adjunto: AdjuntoDocumento)
}
