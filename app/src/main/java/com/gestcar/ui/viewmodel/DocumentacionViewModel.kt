package com.gestcar.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gestcar.datos.basedatos.GestCarBaseDatos
import com.gestcar.datos.entidades.AdjuntoDocumento
import com.gestcar.datos.entidades.CampoDocumento
import com.gestcar.datos.entidades.DocumentoVehiculo
import com.gestcar.datos.repositorio.DocumentacionRepositorio
import com.gestcar.util.GestorArchivosDocumento
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class EstadoListaDocumentacion(
    val documentos: List<DocumentoVehiculo> = emptyList(),
    val cantidadCamposPorDocumento: Map<String, Int> = emptyMap(),
    val estaCargando: Boolean = false,
    val mensajeError: String? = null
)

data class EstadoFormularioDocumentacion(
    val documento: DocumentoVehiculo = DocumentoVehiculo(),
    val campos: List<CampoDocumento> = emptyList(),
    val adjuntos: List<AdjuntoDocumento> = emptyList(),
    val urlsFirmadasAdjuntos: Map<String, String> = emptyMap(),
    val estaCargando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val mensajeError: String? = null
)

class DocumentacionViewModel(aplicacion: Application) : AndroidViewModel(aplicacion) {

    private val baseDatos = GestCarBaseDatos.obtenerInstancia(aplicacion)
    private val repositorio = DocumentacionRepositorio(
        documentoDao = baseDatos.documentoVehiculoDao(),
        campoDao = baseDatos.campoDocumentoDao(),
        adjuntoDao = baseDatos.adjuntoDocumentoDao(),
        vehiculoDao = baseDatos.vehiculoDao()
    )

    private val _estadoLista = MutableStateFlow(EstadoListaDocumentacion())
    val estadoLista: StateFlow<EstadoListaDocumentacion> = _estadoLista.asStateFlow()

    private val _estadoFormulario = MutableStateFlow(EstadoFormularioDocumentacion())
    val estadoFormulario: StateFlow<EstadoFormularioDocumentacion> = _estadoFormulario.asStateFlow()

    fun cargarDocumentos(vehiculoId: String) {
        viewModelScope.launch {
            _estadoLista.value = _estadoLista.value.copy(estaCargando = true, mensajeError = null)
            launch {
                repositorio.sincronizar(vehiculoId)
            }
            repositorio.obtenerDocumentos(vehiculoId).collect { documentos ->
                val cantidades = documentos.associate { documento ->
                    documento.id to repositorio.obtenerCamposLista(documento.id)
                        .count { campo -> campo.nombre.isNotBlank() || campo.valor.isNotBlank() }
                }
                _estadoLista.value = EstadoListaDocumentacion(
                    documentos = documentos,
                    cantidadCamposPorDocumento = cantidades,
                    estaCargando = false
                )
            }
        }
    }

    fun resetearFormulario(vehiculoId: String) {
        val documentoId = UUID.randomUUID().toString()
        _estadoFormulario.value = EstadoFormularioDocumentacion(
            documento = DocumentoVehiculo(
                id = documentoId,
                vehiculoId = vehiculoId
            ),
            campos = listOf(campoVacio(documentoId))
        )
    }

    fun cargarParaEditar(documentoId: String) {
        viewModelScope.launch {
            val documento = repositorio.obtenerDocumentoPorId(documentoId) ?: return@launch
            val campos = repositorio.obtenerCamposLista(documentoId)
            _estadoFormulario.value = EstadoFormularioDocumentacion(
                documento = documento,
                campos = campos.ifEmpty { listOf(campoVacio(documentoId)) }
            )
        }
    }

    fun cargarDetalle(documentoId: String) {
        viewModelScope.launch {
            val documentoInicial = repositorio.obtenerDocumentoPorId(documentoId) ?: return@launch
            val camposIniciales = repositorio.obtenerCamposLista(documentoId)
            val adjuntosIniciales = repositorio.obtenerAdjuntosLista(documentoId)
            _estadoFormulario.value = EstadoFormularioDocumentacion(
                documento = documentoInicial,
                campos = camposIniciales.ifEmpty { listOf(campoVacio(documentoId)) },
                adjuntos = adjuntosIniciales
            )
            cargarUrlsFirmadas(adjuntosIniciales)

            repositorio.sincronizar(documentoInicial.vehiculoId)

            val documentoActualizado = repositorio.obtenerDocumentoPorId(documentoId) ?: return@launch
            val camposActualizados = repositorio.obtenerCamposLista(documentoId)
            val adjuntosActualizados = repositorio.obtenerAdjuntosLista(documentoId)
            _estadoFormulario.value = EstadoFormularioDocumentacion(
                documento = documentoActualizado,
                campos = camposActualizados.ifEmpty { listOf(campoVacio(documentoId)) },
                adjuntos = adjuntosActualizados
            )
            cargarUrlsFirmadas(adjuntosActualizados)
        }
    }

    fun actualizarDocumento(documento: DocumentoVehiculo) {
        _estadoFormulario.value = _estadoFormulario.value.copy(
            documento = documento,
            guardadoExitoso = false,
            mensajeError = null
        )
    }

    fun actualizarCampo(
        campoId: String,
        nombre: String? = null,
        valor: String? = null
    ) {
        val estadoActual = _estadoFormulario.value
        _estadoFormulario.value = estadoActual.copy(
            campos = estadoActual.campos.map { campo ->
                if (campo.id == campoId) {
                    campo.copy(
                        nombre = nombre ?: campo.nombre,
                        valor = valor ?: campo.valor
                    )
                } else {
                    campo
                }
            },
            mensajeError = null
        )
    }

    fun anadirCampo() {
        val estadoActual = _estadoFormulario.value
        val documentoId = estadoActual.documento.id
        _estadoFormulario.value = estadoActual.copy(
            campos = estadoActual.campos + campoVacio(documentoId, estadoActual.campos.size)
        )
    }

    fun eliminarCampo(campoId: String) {
        val estadoActual = _estadoFormulario.value
        val camposRestantes = estadoActual.campos.filterNot { it.id == campoId }
        _estadoFormulario.value = estadoActual.copy(
            campos = camposRestantes
                .ifEmpty { listOf(campoVacio(estadoActual.documento.id)) }
                .mapIndexed { indice, campo -> campo.copy(orden = indice) }
        )
    }

    fun moverCampo(campoId: String, direccion: Int) {
        val estadoActual = _estadoFormulario.value
        val campos = estadoActual.campos.toMutableList()
        val indiceActual = campos.indexOfFirst { it.id == campoId }
        if (indiceActual == -1) {
            return
        }

        val indiceDestino = (indiceActual + direccion).coerceIn(0, campos.lastIndex)
        if (indiceActual == indiceDestino) {
            return
        }

        val campoMovido = campos.removeAt(indiceActual)
        campos.add(indiceDestino, campoMovido)

        _estadoFormulario.value = estadoActual.copy(
            campos = campos.mapIndexed { indice, campo -> campo.copy(orden = indice) },
            mensajeError = null
        )
    }

    fun moverCampoAIndice(campoId: String, indiceDestino: Int) {
        val estadoActual = _estadoFormulario.value
        val campos = estadoActual.campos.toMutableList()
        val indiceActual = campos.indexOfFirst { it.id == campoId }
        if (indiceActual == -1) {
            return
        }

        val destinoSeguro = indiceDestino.coerceIn(0, campos.lastIndex)
        if (indiceActual == destinoSeguro) {
            return
        }

        val campoMovido = campos.removeAt(indiceActual)
        campos.add(destinoSeguro, campoMovido)

        _estadoFormulario.value = estadoActual.copy(
            campos = campos.mapIndexed { indice, campo -> campo.copy(orden = indice) },
            mensajeError = null
        )
    }

    fun guardarDocumento() {
        viewModelScope.launch {
            val estadoActual = _estadoFormulario.value
            val documento = estadoActual.documento

            if (documento.vehiculoId.isBlank() || documento.titulo.isBlank()) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Faltan campos obligatorios por rellenar"
                )
                return@launch
            }

            val hayCampoIncompleto = estadoActual.campos.any { campo ->
                campo.nombre.isBlank() && campo.valor.isNotBlank()
            }
            if (hayCampoIncompleto) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Revisa los campos sin nombre"
                )
                return@launch
            }

            _estadoFormulario.value = estadoActual.copy(estaCargando = true, mensajeError = null)
            val resultado = repositorio.guardar(documento, estadoActual.campos)
            _estadoFormulario.value = if (resultado.isSuccess) {
                estadoActual.copy(
                    estaCargando = false,
                    guardadoExitoso = true,
                    mensajeError = null
                )
            } else {
                estadoActual.copy(
                    estaCargando = false,
                    guardadoExitoso = false,
                    mensajeError = "No se ha podido guardar la documentación"
                )
            }
        }
    }

    fun eliminarDocumento(documento: DocumentoVehiculo) {
        viewModelScope.launch {
            repositorio.eliminar(documento)
        }
    }

    fun anadirAdjunto(origenUri: Uri) {
        viewModelScope.launch {
            val estadoActual = _estadoFormulario.value
            val documentoId = estadoActual.documento.id
            if (documentoId.isBlank()) {
                _estadoFormulario.value = estadoActual.copy(
                    mensajeError = "Guarda la documentación antes de añadir archivos"
                )
                return@launch
            }

            val resultado = runCatching {
                GestorArchivosDocumento.guardarAdjunto(
                    contexto = getApplication(),
                    documentoId = documentoId,
                    origenUri = origenUri,
                    orden = estadoActual.adjuntos.size
                )
            }.mapCatching { adjunto ->
                repositorio.guardarAdjunto(adjunto).getOrThrow()
                adjunto
            }

            resultado
                .onSuccess {
                    val adjuntosActualizados = repositorio.obtenerAdjuntosLista(documentoId)
                    _estadoFormulario.value = estadoActual.copy(
                        adjuntos = adjuntosActualizados,
                        mensajeError = null
                    )
                    cargarUrlsFirmadas(adjuntosActualizados)
                }
                .onFailure {
                    _estadoFormulario.value = estadoActual.copy(
                        mensajeError = it.message ?: "No se ha podido añadir el archivo"
                    )
                }
        }
    }

    fun eliminarAdjunto(adjunto: AdjuntoDocumento) {
        viewModelScope.launch {
            GestorArchivosDocumento.eliminarArchivo(adjunto)
            repositorio.eliminarAdjunto(adjunto)
            _estadoFormulario.value = _estadoFormulario.value.copy(
                adjuntos = repositorio.obtenerAdjuntosLista(adjunto.documentoId),
                urlsFirmadasAdjuntos = _estadoFormulario.value.urlsFirmadasAdjuntos - adjunto.id,
                mensajeError = null
            )
        }
    }

    fun obtenerUriLocalCompartible(adjunto: AdjuntoDocumento): Uri? {
        return GestorArchivosDocumento.obtenerUriCompartible(getApplication(), adjunto)
    }

    fun cargarUrlFirmadaAdjunto(adjunto: AdjuntoDocumento) {
        cargarUrlsFirmadas(listOf(adjunto))
    }

    private fun cargarUrlsFirmadas(adjuntos: List<AdjuntoDocumento>) {
        val pendientes = adjuntos.filter { adjunto ->
            !adjunto.rutaStorage.isNullOrBlank() &&
                _estadoFormulario.value.urlsFirmadasAdjuntos[adjunto.id].isNullOrBlank()
        }
        if (pendientes.isEmpty()) {
            return
        }

        viewModelScope.launch {
            pendientes.forEach { adjunto ->
                val url = repositorio.obtenerUrlFirmadaAdjunto(adjunto) ?: return@forEach
                val estadoActual = _estadoFormulario.value
                _estadoFormulario.value = estadoActual.copy(
                    urlsFirmadasAdjuntos = estadoActual.urlsFirmadasAdjuntos + (adjunto.id to url)
                )
            }
        }
    }

    private fun campoVacio(
        documentoId: String,
        orden: Int = 0
    ): CampoDocumento {
        return CampoDocumento(
            id = UUID.randomUUID().toString(),
            documentoId = documentoId,
            orden = orden
        )
    }
}
