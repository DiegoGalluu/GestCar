package com.gestcar.ui.pantallas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gestcar.datos.entidades.AdjuntoDocumento
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.DocumentacionViewModel
import com.gestcar.util.GestorArchivosDocumento
import java.util.Locale

@Composable
fun PantallaDetalleDocumento(
    documentoId: String,
    alEditar: (String, String) -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    viewModel: DocumentacionViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val documento = estado.documento
    val campos = estado.campos.filter { it.nombre.isNotBlank() || it.valor.isNotBlank() }
    val contexto = LocalContext.current
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarModalAdjunto by remember { mutableStateOf(false) }
    var uriTemporalCamara by remember { mutableStateOf<Uri?>(null) }

    val selectorImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.anadirAdjunto(uri)
        }
    }

    val selectorDocumento = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.anadirAdjunto(uri)
        }
    }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { imagenGuardada ->
        val uriCaptura = uriTemporalCamara
        if (imagenGuardada && uriCaptura != null) {
            viewModel.anadirAdjunto(uriCaptura)
        }
        uriTemporalCamara = null
    }

    LaunchedEffect(documentoId) {
        viewModel.cargarDetalle(documentoId)
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Detalle de documentación",
                alVolver = alVolver,
                acciones = {
                    if (documento.id.isNotBlank()) {
                        IconButton(onClick = { alEditar(documento.vehiculoId, documento.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar documentación")
                        }
                        IconButton(onClick = { mostrarDialogoEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar documentación")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = documento.titulo.ifBlank { "Documentación" },
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (campos.isEmpty()) {
                        Text(
                            text = "Sin campos guardados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        campos.forEach { campo ->
                            FilaCampoDocumento(
                                nombre = campo.nombre,
                                valor = campo.valor
                            )
                        }
                    }

                    documento.notas?.takeIf { it.isNotBlank() }?.let { notas ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = notas,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            EnlaceAnadirArchivo(
                alPulsar = { mostrarModalAdjunto = true }
            )

            if (estado.adjuntos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            estado.adjuntos.forEach { adjunto ->
                TarjetaAdjuntoDocumento(
                    adjunto = adjunto,
                    alEliminar = { viewModel.eliminarAdjunto(adjunto) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            estado.mensajeError?.let { error ->
                SnackbarDocumento(error)
            }
        }
    }

    if (mostrarModalAdjunto) {
        AlertDialog(
            onDismissRequest = { mostrarModalAdjunto = false },
            title = { Text("Añadir archivo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OpcionAdjunto(
                        icono = Icons.Default.PhotoCamera,
                        texto = "Hacer foto",
                        alPulsar = {
                            mostrarModalAdjunto = false
                            val uriCamara = GestorArchivosDocumento.crearUriTemporalCamara(
                                contexto = contexto,
                                documentoId = documento.id
                            )
                            uriTemporalCamara = uriCamara
                            lanzadorCamara.launch(uriCamara)
                        }
                    )
                    OpcionAdjunto(
                        icono = Icons.Default.PhotoLibrary,
                        texto = "Elegir imagen",
                        alPulsar = {
                            mostrarModalAdjunto = false
                            selectorImagen.launch("image/*")
                        }
                    )
                    OpcionAdjunto(
                        icono = Icons.AutoMirrored.Filled.InsertDriveFile,
                        texto = "Seleccionar PDF",
                        alPulsar = {
                            mostrarModalAdjunto = false
                            selectorDocumento.launch(arrayOf("application/pdf"))
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { mostrarModalAdjunto = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar documentación") },
            text = { Text("¿Seguro que quieres eliminar esta tarjeta de documentación?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarDocumento(documento)
                        mostrarDialogoEliminar = false
                        alEliminar()
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoEliminar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun EnlaceAnadirArchivo(alPulsar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = alPulsar
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Añadir archivo",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TarjetaAdjuntoDocumento(
    adjunto: AdjuntoDocumento,
    alEliminar: () -> Unit
) {
    val esImagen = adjunto.mimeType.startsWith("image/")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (esImagen) {
            Box {
                AsyncImage(
                    model = adjunto.uriLocal,
                    contentDescription = adjunto.nombreArchivo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                IconButton(
                    onClick = alEliminar,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar archivo",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = adjunto.nombreArchivo,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${tipoLegible(adjunto.mimeType)} · ${tamanoLegible(adjunto.tamanoBytes)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                    }
                    IconButton(onClick = alEliminar) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar archivo",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpcionAdjunto(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    alPulsar: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = alPulsar)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = texto, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SnackbarDocumento(error: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun FilaCampoDocumento(
    nombre: String,
    valor: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = nombre.ifBlank { "Campo sin nombre" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor.ifBlank { "Sin información" },
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun tipoLegible(mimeType: String): String {
    return when (mimeType.lowercase(Locale.ROOT)) {
        "application/pdf" -> "PDF"
        else -> mimeType.substringAfterLast('/').uppercase(Locale.ROOT)
    }
}

private fun tamanoLegible(bytes: Long): String {
    val kb = bytes / 1024.0
    if (kb < 1024.0) {
        return String.format(Locale("es", "ES"), "%.0f KB", kb)
    }
    return String.format(Locale("es", "ES"), "%.1f MB", kb / 1024.0)
}
