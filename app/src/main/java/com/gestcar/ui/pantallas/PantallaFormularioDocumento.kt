package com.gestcar.ui.pantallas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.CampoDocumento
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.colorFondoTarjetaUsuario
import com.gestcar.ui.viewmodel.DocumentacionViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PantallaFormularioDocumento(
    vehiculoId: String,
    documentoId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: DocumentacionViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val documento = estado.documento
    val esNuevo = documentoId == "nuevo"
    val scrollFormulario = rememberScrollState()
    val tituloVacio = documento.titulo.isBlank()
    val medidasCampos = remember { mutableStateMapOf<String, MedidaCampoFormulario>() }
    val margenScrollAutomatico = with(LocalDensity.current) { 72.dp.toPx() }
    val pasoScrollAutomatico = with(LocalDensity.current) { 10.dp.toPx() }
    val intervaloScrollAutomaticoMs = 16L
    var intentoGuardar by remember { mutableStateOf(false) }
    var topContenedor by remember { mutableFloatStateOf(0f) }
    var altoContenedor by remember { mutableFloatStateOf(0f) }
    var campoArrastradoId by remember { mutableStateOf<String?>(null) }
    var topInicialArrastre by remember { mutableFloatStateOf(0f) }
    var altoCampoArrastrado by remember { mutableFloatStateOf(0f) }
    var desplazamientoArrastre by remember { mutableFloatStateOf(0f) }
    var indiceDestinoArrastre by remember { mutableStateOf<Int?>(null) }
    var direccionScrollAutomatico by remember { mutableStateOf(0) }

    fun actualizarDestinoArrastre() {
        val campoActivoId = campoArrastradoId ?: return
        val centroTarjeta = topInicialArrastre + desplazamientoArrastre + altoCampoArrastrado / 2f
        val camposSinActivo = estado.campos.filterNot { it.id == campoActivoId }
        var destino = camposSinActivo.size

        for ((indiceCampo, campo) in camposSinActivo.withIndex()) {
            val medida = medidasCampos[campo.id] ?: continue
            val centroCampo = medida.top + medida.alto / 2f
            if (centroTarjeta < centroCampo) {
                destino = indiceCampo
                break
            }
        }

        indiceDestinoArrastre = destino
    }

    fun actualizarDireccionScrollAutomatico() {
        if (campoArrastradoId == null || altoContenedor == 0f) {
            direccionScrollAutomatico = 0
            return
        }

        val topTarjeta = topInicialArrastre + desplazamientoArrastre
        val bottomTarjeta = topTarjeta + altoCampoArrastrado
        direccionScrollAutomatico = when {
            topTarjeta < margenScrollAutomatico && scrollFormulario.value > 0 -> -1
            bottomTarjeta > altoContenedor - margenScrollAutomatico &&
                scrollFormulario.value < scrollFormulario.maxValue -> 1
            else -> 0
        }
    }

    fun iniciarArrastreCampo(campo: CampoDocumento, indice: Int) {
        val medida = medidasCampos[campo.id] ?: return
        campoArrastradoId = campo.id
        topInicialArrastre = medida.top
        altoCampoArrastrado = medida.alto
        desplazamientoArrastre = 0f
        indiceDestinoArrastre = indice
        direccionScrollAutomatico = 0
    }

    fun cancelarArrastreCampo() {
        campoArrastradoId = null
        desplazamientoArrastre = 0f
        indiceDestinoArrastre = null
        direccionScrollAutomatico = 0
    }

    fun terminarArrastreCampo() {
        val campoActivoId = campoArrastradoId
        val destino = indiceDestinoArrastre
        if (campoActivoId != null && destino != null) {
            viewModel.moverCampoAIndice(campoActivoId, destino)
        }
        cancelarArrastreCampo()
    }

    LaunchedEffect(vehiculoId, documentoId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(documentoId)
        }
    }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            alGuardar()
        }
    }

    LaunchedEffect(campoArrastradoId, direccionScrollAutomatico) {
        while (campoArrastradoId != null && direccionScrollAutomatico != 0) {
            val desplazamientoReal = scrollFormulario.scrollBy(
                pasoScrollAutomatico * direccionScrollAutomatico
            )
            if (desplazamientoReal == 0f) {
                direccionScrollAutomatico = 0
                break
            }

            actualizarDestinoArrastre()
            actualizarDireccionScrollAutomatico()
            delay(intervaloScrollAutomaticoMs)
        }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = if (esNuevo) "Nueva documentación" else "Editar documentación",
                alVolver = alVolver
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .onGloballyPositioned { coordenadas ->
                    val bounds = coordenadas.boundsInRoot()
                    topContenedor = bounds.top
                    altoContenedor = bounds.height
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollFormulario),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = documento.titulo,
                    onValueChange = { viewModel.actualizarDocumento(documento.copy(titulo = it)) },
                    label = { Text("Título *") },
                    isError = intentoGuardar && tituloVacio,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = documento.notas ?: "",
                    onValueChange = {
                        viewModel.actualizarDocumento(documento.copy(notas = it))
                    },
                    label = { Text("Notas") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Campos personalizados",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(onClick = { viewModel.anadirCampo() }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Añadir")
                    }
                }

                val campoActivoId = campoArrastradoId
                val destinoActual = indiceDestinoArrastre
                var indiceSinActivo = 0

                estado.campos.forEachIndexed { indice, campo ->
                    if (campo.id != campoActivoId && destinoActual == indiceSinActivo) {
                        IndicadorDestinoCampo(altoCampoArrastrado)
                    }

                    key(campo.id) {
                        TarjetaCampoDocumentoEditable(
                            campo = campo,
                            indice = indice,
                            puedeSubir = indice > 0,
                            puedeBajar = indice < estado.campos.lastIndex,
                            estaSiendoArrastrada = campo.id == campoActivoId,
                            modifier = Modifier.onGloballyPositioned { coordenadas ->
                                val bounds = coordenadas.boundsInRoot()
                                medidasCampos[campo.id] = MedidaCampoFormulario(
                                    top = bounds.top - topContenedor,
                                    alto = bounds.height
                                )
                            },
                            alIniciarArrastre = { iniciarArrastreCampo(campo, indice) },
                            alArrastrar = { movimientoVertical ->
                                desplazamientoArrastre += movimientoVertical
                                actualizarDestinoArrastre()
                                actualizarDireccionScrollAutomatico()
                            },
                            alTerminarArrastre = { terminarArrastreCampo() },
                            alCancelarArrastre = { cancelarArrastreCampo() },
                            alEliminar = { viewModel.eliminarCampo(campo.id) },
                            alCambiarNombre = { viewModel.actualizarCampo(campo.id, nombre = it) },
                            alCambiarValor = { viewModel.actualizarCampo(campo.id, valor = it) }
                        )
                    }

                    if (campo.id != campoActivoId) {
                        indiceSinActivo++
                    }
                }

                if (campoActivoId != null && destinoActual == indiceSinActivo) {
                    IndicadorDestinoCampo(altoCampoArrastrado)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        intentoGuardar = true
                        if (!tituloVacio) {
                            viewModel.guardarDocumento()
                        }
                    },
                    enabled = !estado.estaCargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (estado.estaCargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar documentación")
                    }
                }

                if (intentoGuardar && tituloVacio) {
                    Snackbar {
                        Text("Faltan campos obligatorios por rellenar")
                    }
                }

                estado.mensajeError?.let { error ->
                    Snackbar {
                        Text(error)
                    }
                }
            }

            campoArrastradoId?.let { idActivo ->
                val campoActivo = estado.campos.firstOrNull { it.id == idActivo }
                val indiceActivo = estado.campos.indexOfFirst { it.id == idActivo }
                if (campoActivo != null && indiceActivo != -1) {
                    TarjetaCampoDocumentoEditable(
                        campo = campoActivo,
                        indice = indiceActivo,
                        puedeSubir = indiceActivo > 0,
                        puedeBajar = indiceActivo < estado.campos.lastIndex,
                        estaSiendoArrastrada = true,
                        arrastreHabilitado = false,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (topInicialArrastre + desplazamientoArrastre).roundToInt()
                                )
                            }
                            .zIndex(4f),
                        alIniciarArrastre = {},
                        alArrastrar = {},
                        alTerminarArrastre = {},
                        alCancelarArrastre = {},
                        alEliminar = {},
                        alCambiarNombre = {},
                        alCambiarValor = {}
                    )
                }
            }
        }
    }
}

private data class MedidaCampoFormulario(
    val top: Float,
    val alto: Float
)

@Composable
private fun IndicadorDestinoCampo(altoCampoArrastrado: Float) {
    val altoIndicador = with(LocalDensity.current) {
        altoCampoArrastrado.coerceAtLeast(72.dp.toPx()).toDp()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(altoIndicador)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp)
            )
    )
}

@Composable
private fun TarjetaCampoDocumentoEditable(
    campo: CampoDocumento,
    indice: Int,
    puedeSubir: Boolean,
    puedeBajar: Boolean,
    modifier: Modifier = Modifier,
    estaSiendoArrastrada: Boolean = false,
    arrastreHabilitado: Boolean = true,
    alIniciarArrastre: () -> Unit,
    alArrastrar: (Float) -> Unit,
    alTerminarArrastre: () -> Unit,
    alCancelarArrastre: () -> Unit,
    alEliminar: () -> Unit,
    alCambiarNombre: (String) -> Unit,
    alCambiarValor: (String) -> Unit
) {
    val escalaAnimada by animateFloatAsState(
        targetValue = if (estaSiendoArrastrada) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "escalaOrdenCampoDocumento"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = if (estaSiendoArrastrada && arrastreHabilitado) 0.16f else 1f
                scaleX = escalaAnimada
                scaleY = escalaAnimada
            }
            .zIndex(if (estaSiendoArrastrada) 2f else 0f),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (estaSiendoArrastrada) 10.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = colorFondoTarjetaUsuario(indice)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Campo ${indice + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = alEliminar,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar campo"
                        )
                    }
                }

                OutlinedTextField(
                    value = campo.nombre,
                    onValueChange = alCambiarNombre,
                    label = { Text("Nombre del campo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = campo.valor,
                    onValueChange = alCambiarValor,
                    label = { Text("Valor o descripción") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val modificadorArrastre = if (arrastreHabilitado) {
                Modifier.pointerInput(campo.id) {
                    detectDragGestures(
                        onDragStart = { alIniciarArrastre() },
                        onDragEnd = { alTerminarArrastre() },
                        onDragCancel = { alCancelarArrastre() },
                        onDrag = { cambio, dragAmount ->
                            cambio.consume()
                            alArrastrar(dragAmount.y)
                        }
                    )
                }
            } else {
                Modifier
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(36.dp)
                    .then(modificadorArrastre)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (puedeSubir) 1f else 0.25f
                    )
                )
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Arrastrar campo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = if (puedeBajar) 1f else 0.25f
                    )
                )
            }
        }
    }
}
