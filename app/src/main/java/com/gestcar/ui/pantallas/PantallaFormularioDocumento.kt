package com.gestcar.ui.pantallas

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.CampoDocumento
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.colorFondoTarjetaUsuario
import com.gestcar.ui.viewmodel.DocumentacionViewModel
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
    var intentoGuardar by remember { mutableStateOf(false) }
    val tituloVacio = documento.titulo.isBlank()

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

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = if (esNuevo) "Nueva documentación" else "Editar documentación",
                alVolver = alVolver
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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
                    viewModel.actualizarDocumento(documento.copy(notas = it.ifBlank { null }))
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

            estado.campos.forEachIndexed { indice, campo ->
                TarjetaCampoDocumentoEditable(
                    campo = campo,
                    indice = indice,
                    puedeSubir = indice > 0,
                    puedeBajar = indice < estado.campos.lastIndex,
                    alMover = { direccion -> viewModel.moverCampo(campo.id, direccion) },
                    alEliminar = { viewModel.eliminarCampo(campo.id) },
                    alCambiarNombre = { viewModel.actualizarCampo(campo.id, nombre = it) },
                    alCambiarValor = { viewModel.actualizarCampo(campo.id, valor = it) }
                )
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
    }
}

@Composable
private fun TarjetaCampoDocumentoEditable(
    campo: CampoDocumento,
    indice: Int,
    puedeSubir: Boolean,
    puedeBajar: Boolean,
    alMover: (Int) -> Unit,
    alEliminar: () -> Unit,
    alCambiarNombre: (String) -> Unit,
    alCambiarValor: (String) -> Unit
) {
    val umbralArrastre = with(LocalDensity.current) { 72.dp.toPx() }
    val limiteArrastre = with(LocalDensity.current) { 112.dp.toPx() }
    var acumuladoArrastre by remember(campo.id) { mutableFloatStateOf(0f) }
    var desplazamientoArrastre by remember(campo.id) { mutableFloatStateOf(0f) }
    val desplazamientoAnimado by animateFloatAsState(
        targetValue = desplazamientoArrastre,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "animacionOrdenCampoDocumento"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, desplazamientoAnimado.roundToInt()) }
            .zIndex(if (desplazamientoAnimado != 0f) 1f else 0f),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(36.dp)
                    .pointerInput(campo.id, puedeSubir, puedeBajar) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                acumuladoArrastre = 0f
                                desplazamientoArrastre = 0f
                            },
                            onDragCancel = {
                                acumuladoArrastre = 0f
                                desplazamientoArrastre = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                acumuladoArrastre += dragAmount
                                desplazamientoArrastre = (desplazamientoArrastre + dragAmount)
                                    .coerceIn(-limiteArrastre, limiteArrastre)

                                when {
                                    acumuladoArrastre <= -umbralArrastre && puedeSubir -> {
                                        alMover(-1)
                                        acumuladoArrastre = 0f
                                        desplazamientoArrastre = 0f
                                    }

                                    acumuladoArrastre >= umbralArrastre && puedeBajar -> {
                                        alMover(1)
                                        acumuladoArrastre = 0f
                                        desplazamientoArrastre = 0f
                                    }
                                }
                            }
                        )
                    }
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
