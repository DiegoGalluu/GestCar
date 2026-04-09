package com.gestcar.ui.pantallas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.ImagenVehiculo
import com.gestcar.ui.viewmodel.VehiculoViewModel
import com.gestcar.util.GestorImagenesVehiculo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// pantalla que muestra toda la info detallada de un vehiculo
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleVehiculo(
    vehiculoId: String,
    alEditar: () -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    viewModel: VehiculoViewModel = viewModel()
) {
    val vehiculo by viewModel.vehiculoDetalle.collectAsState()
    val estadoLista by viewModel.estadoLista.collectAsState()
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var uriTemporalCamara by remember { mutableStateOf<Uri?>(null) }
    val contexto = LocalContext.current

    val selectorGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.actualizarImagenVehiculo(uri)
        }
    }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { imagenGuardada ->
        val uriCaptura = uriTemporalCamara
        if (imagenGuardada && uriCaptura != null) {
            viewModel.actualizarImagenVehiculo(uriCaptura)
        }
        uriTemporalCamara = null
    }

    // cargamos el vehiculo al entrar en la pantalla
    LaunchedEffect(vehiculoId) {
        viewModel.cargarDetalle(vehiculoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del vehiculo") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "volver")
                    }
                },
                actions = {
                    // boton de editar
                    IconButton(onClick = alEditar) {
                        Icon(Icons.Default.Edit, contentDescription = "editar vehiculo")
                    }
                    // boton de eliminar
                    IconButton(onClick = { mostrarDialogoEliminar = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "eliminar vehiculo")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        vehiculo?.let { v ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.BottomEnd
                ) {
                    ImagenVehiculo(
                        vehiculo = v,
                        modifier = Modifier.size(220.dp),
                        iconoPadding = 36
                    )

                    FloatingActionButton(
                        onClick = { mostrarMenuFoto = true },
                        modifier = Modifier.padding(12.dp),
                        containerColor = Color(0xFF7FC8FF)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "anadir foto",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = mostrarMenuFoto,
                        onDismissRequest = { mostrarMenuFoto = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Elegir de la galeria") },
                            leadingIcon = {
                                Icon(Icons.Default.Image, contentDescription = null)
                            },
                            onClick = {
                                mostrarMenuFoto = false
                                selectorGaleria.launch("image/*")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hacer una foto") },
                            leadingIcon = {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                            },
                            onClick = {
                                mostrarMenuFoto = false
                                val vehiculoActual = vehiculo ?: return@DropdownMenuItem
                                val nuevaUri = GestorImagenesVehiculo.crearUriTemporalCamara(
                                    context = contexto,
                                    vehiculoId = vehiculoActual.id
                                )
                                uriTemporalCamara = nuevaUri
                                lanzadorCamara.launch(nuevaUri)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // nombre del vehiculo
                Text(
                    text = "${v.marca} ${v.modelo}",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = v.matricula,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // tarjeta con los datos principales
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilaDato("Tipo", v.tipo)
                        FilaDato("Año de fabricación", formatearFechaFabricacion(v))
                        FilaDato("Kilometraje", "${String.format("%,.0f", v.kilometraje)} km")
                        v.tipoCombustible?.let { FilaDato("Combustible", it) }
                        FilaDato("Fecha de alta", formatearFecha(v.fechaAlta))
                        v.notas?.let { FilaDato("Notas", it) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // en futuras fases aqui iran las secciones de repostajes, mantenimientos, etc
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Proximamente",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Aqui se mostraran los repostajes, mantenimientos y estadisticas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                estadoLista.mensajeError?.let { mensaje ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Snackbar {
                        Text(mensaje)
                    }
                }
            }
        }
    }

    // dialogo de confirmacion para eliminar el vehiculo
    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar vehiculo") },
            text = { Text("Estas seguro de que quieres eliminar este vehiculo? Se borraran todos sus datos asociados") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vehiculo?.let { viewModel.eliminarVehiculo(it) }
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

// fila con etiqueta y valor para mostrar datos del vehiculo
@Composable
fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// funcion helper para formatear un timestamp a fecha legible
fun formatearFecha(timestamp: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
    return formato.format(Date(timestamp))
}

fun formatearFechaFabricacion(vehiculo: Vehiculo): String {
    val anio = vehiculo.anioFabricacion.toString()
    val mes = vehiculo.mesFabricacion?.let { numeroMes ->
        listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
        ).getOrNull(numeroMes - 1)
    }

    return when {
        vehiculo.diaFabricacion != null && mes != null -> "${vehiculo.diaFabricacion} de $mes de $anio"
        mes != null -> "$mes de $anio"
        else -> anio
    }
}
