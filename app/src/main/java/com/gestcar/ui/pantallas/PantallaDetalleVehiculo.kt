package com.gestcar.ui.pantallas

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Build
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.entidades.GastoPeriodico
import com.gestcar.datos.entidades.Mantenimiento
import com.gestcar.datos.entidades.Recordatorio
import com.gestcar.datos.entidades.Repostaje
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.componentes.EstadoVisualRecordatorio
import com.gestcar.ui.componentes.ImagenVehiculo
import com.gestcar.ui.componentes.calcularEstadoVisualGasto
import com.gestcar.ui.componentes.formatearFechaCorta
import com.gestcar.ui.componentes.textoEstadoGasto
import com.gestcar.ui.viewmodel.DetalleVehiculoResumenViewModel
import com.gestcar.ui.viewmodel.VehiculoViewModel
import com.gestcar.util.GestorImagenesVehiculo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleVehiculo(
    vehiculoId: String,
    alEditar: () -> Unit,
    alVolver: () -> Unit,
    alEliminar: () -> Unit,
    alVerRepostajes: () -> Unit,
    alVerMantenimientos: () -> Unit,
    alVerGastos: () -> Unit,
    alVerRecordatorios: () -> Unit,
    viewModel: VehiculoViewModel = viewModel(),
    resumenViewModel: DetalleVehiculoResumenViewModel = viewModel()
) {
    val vehiculo by viewModel.vehiculoDetalle.collectAsState()
    val estadoLista by viewModel.estadoLista.collectAsState()
    val resumen by resumenViewModel.estado.collectAsState()
    var mostrarDialogoEliminar by remember { mutableStateOf(false) }
    var mostrarMenuFoto by remember { mutableStateOf(false) }
    var uriTemporalCamara by remember { mutableStateOf<Uri?>(null) }
    val contexto = LocalContext.current

    val selectorGaleria = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // la galeria devuelve una uri temporal del sistema
        // el viewmodel la copia y comprime dentro de almacenamiento privado
        if (uri != null) {
            viewModel.actualizarImagenVehiculo(uri)
        }
    }

    val lanzadorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { imagenGuardada ->
        val uriCaptura = uriTemporalCamara
        // takepicture escribe directamente en la uri que le damos
        // si el resultado es correcto procesamos esa misma uri como cualquier imagen
        if (imagenGuardada && uriCaptura != null) {
            viewModel.actualizarImagenVehiculo(uriCaptura)
        }
        uriTemporalCamara = null
    }

    LaunchedEffect(vehiculoId) {
        viewModel.cargarDetalle(vehiculoId)
    }

    LaunchedEffect(vehiculo?.id, vehiculo?.kilometraje) {
        // el resumen depende del kilometraje porque algunas metricas son coste por km
        // si el vehiculo cambia o se actualiza el odometro recalculamos
        vehiculo?.let { resumenViewModel.cargarResumen(it.id, it.kilometraje) }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Detalle del vehículo",
                alVolver = alVolver,
                acciones = {
                    IconButton(onClick = alEditar) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar vehículo")
                    }
                    IconButton(onClick = { mostrarDialogoEliminar = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar vehículo")
                    }
                }
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
                CabeceraVehiculoDetalle(
                    vehiculo = v,
                    mostrarMenuFoto = mostrarMenuFoto,
                    alCambiarMenuFoto = { mostrarMenuFoto = it },
                    alElegirGaleria = { selectorGaleria.launch("image/*") },
                    alHacerFoto = {
                        val nuevaUri = GestorImagenesVehiculo.crearUriTemporalCamara(
                            context = contexto,
                            vehiculoId = v.id
                        )
                        uriTemporalCamara = nuevaUri
                        lanzadorCamara.launch(nuevaUri)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                TarjetaDatosVehiculo(v)

                Spacer(modifier = Modifier.height(16.dp))

                // resumen rapido para evitar que el detalle sea solo una ficha estatica
                // aqui se ve de un vistazo consumo gastos y coste por kilometro
                ResumenRapidoVehiculo(
                    consumoMedio = resumen.consumoMedio,
                    costeCombustibleCada100Km = resumen.costeCombustibleCada100Km,
                    costePorKilometro = resumen.costePorKilometro,
                    costeTotal = resumen.costeTotal
                )

                Spacer(modifier = Modifier.height(16.dp))

                SeccionResumen(
                    titulo = "Últimos repostajes",
                    textoVacio = "No hay repostajes registrados",
                    estaVacia = resumen.ultimosRepostajes.isEmpty(),
                    alVerTodos = alVerRepostajes
                ) {
                    resumen.ultimosRepostajes.forEach { repostaje ->
                        FilaRepostajeResumen(repostaje)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SeccionResumen(
                    titulo = "Últimos mantenimientos",
                    textoVacio = "No hay operaciones registradas",
                    estaVacia = resumen.ultimosMantenimientos.isEmpty(),
                    alVerTodos = alVerMantenimientos
                ) {
                    resumen.ultimosMantenimientos.forEach { mantenimiento ->
                        FilaMantenimientoResumen(mantenimiento)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SeccionResumen(
                    titulo = "Gastos pendientes",
                    textoVacio = "No hay gastos pendientes",
                    estaVacia = resumen.gastosActivos.isEmpty(),
                    alVerTodos = alVerGastos
                ) {
                    resumen.gastosActivos.forEach { gasto ->
                        FilaGastoResumen(gasto)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                SeccionResumen(
                    titulo = "Recordatorios pendientes",
                    textoVacio = "No hay recordatorios pendientes",
                    estaVacia = resumen.recordatoriosPendientes.isEmpty(),
                    alVerTodos = alVerRecordatorios
                ) {
                    resumen.recordatoriosPendientes.forEach { recordatorio ->
                        FilaRecordatorioResumen(recordatorio, v)
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

    if (mostrarDialogoEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoEliminar = false },
            title = { Text("Eliminar vehículo") },
            text = { Text("¿Estás seguro de que quieres eliminar este vehículo? Se borrarán todos sus datos asociados.") },
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

@Composable
private fun CabeceraVehiculoDetalle(
    vehiculo: Vehiculo,
    mostrarMenuFoto: Boolean,
    alCambiarMenuFoto: (Boolean) -> Unit,
    alElegirGaleria: () -> Unit,
    alHacerFoto: () -> Unit
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        // imagenvehiculo ya decide si usa foto local remota o icono por defecto
        // esta pantalla solo se preocupa de mostrarla y ofrecer cambiarla
        ImagenVehiculo(
            vehiculo = vehiculo,
            modifier = Modifier.size(220.dp),
            iconoPadding = 36
        )

        FloatingActionButton(
            onClick = { alCambiarMenuFoto(true) },
            modifier = Modifier.padding(12.dp),
            containerColor = Color(0xFF7FC8FF)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Añadir foto",
                tint = Color.White
            )
        }

        DropdownMenu(
            expanded = mostrarMenuFoto,
            onDismissRequest = { alCambiarMenuFoto(false) }
        ) {
            DropdownMenuItem(
                text = { Text("Elegir de la galería") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                onClick = {
                    alCambiarMenuFoto(false)
                    alElegirGaleria()
                }
            )
            DropdownMenuItem(
                text = { Text("Hacer una foto") },
                leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                onClick = {
                    alCambiarMenuFoto(false)
                    alHacerFoto()
                }
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "${vehiculo.marca} ${vehiculo.modelo}",
        style = MaterialTheme.typography.headlineMedium
    )

    Text(
        text = vehiculo.matricula,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun TarjetaDatosVehiculo(vehiculo: Vehiculo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilaDato("Tipo", vehiculo.tipo)
            FilaDato("Año de fabricación", formatearFechaFabricacion(vehiculo))
            FilaDato("Kilometraje", "${formatearNumero(vehiculo.kilometraje)} km")
            vehiculo.tipoCombustible?.let { FilaDato("Combustible", it) }
            FilaDato("Fecha de alta", formatearFechaDetalle(vehiculo.fechaAlta))
            vehiculo.notas?.takeIf { it.isNotBlank() }?.let { FilaDato("Notas", it) }
        }
    }
}

@Composable
private fun ResumenRapidoVehiculo(
    consumoMedio: Double,
    costeCombustibleCada100Km: Double,
    costePorKilometro: Double,
    costeTotal: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TarjetaMetrica(
            titulo = "Consumo",
            valor = if (consumoMedio > 0) "${formatearDecimal(consumoMedio)} L/100 km" else "Sin datos",
            icono = Icons.Default.LocalGasStation,
            modifier = Modifier.weight(1f)
        )
        TarjetaMetrica(
            titulo = "Combustible /100 km",
            valor = if (costeCombustibleCada100Km > 0) "${formatearDecimal(costeCombustibleCada100Km)} €" else "Sin datos",
            icono = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TarjetaMetrica(
            titulo = "Gasto registrado",
            valor = "${formatearDecimal(costeTotal)} €",
            icono = Icons.Default.Payments,
            modifier = Modifier.weight(1f)
        )
        TarjetaMetrica(
            titulo = "Gasto total/km",
            valor = if (costePorKilometro > 0) "${formatearDecimal(costePorKilometro)} €/km" else "Sin datos",
            icono = Icons.Default.TrendingUp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TarjetaMetrica(
    titulo: String,
    valor: String,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valor,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun SeccionResumen(
    titulo: String,
    textoVacio: String,
    estaVacia: Boolean,
    alVerTodos: () -> Unit,
    contenido: @Composable ColumnScope.() -> Unit
) {
    // componente comun para bloques de resumen del detalle
    // mantiene la misma estructura visual para repostajes gastos mantenimiento y recordatorios
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = alVerTodos) {
                    Text("Ver todos")
                }
            }

            if (estaVacia) {
                Text(
                    text = textoVacio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    contenido()
                }
            }
        }
    }
}

@Composable
private fun FilaRepostajeResumen(repostaje: Repostaje) {
    FilaMovimientoResumen(
        icono = Icons.Default.LocalGasStation,
        titulo = formatearFechaCorta(repostaje.fecha),
        detalle = "${formatearNumero(repostaje.kilometros)} km · ${formatearDecimal(repostaje.litros)} L",
        importe = "${formatearDecimal(repostaje.importeTotal)} €"
    )
}

@Composable
private fun FilaMantenimientoResumen(mantenimiento: Mantenimiento) {
    val detalleKm = mantenimiento.kilometros?.let { " · ${formatearNumero(it)} km" }.orEmpty()
    FilaMovimientoResumen(
        icono = Icons.Default.Build,
        titulo = mantenimiento.tipo,
        detalle = "${formatearFechaCorta(mantenimiento.fecha)}$detalleKm",
        importe = "${formatearDecimal(mantenimiento.coste)} €"
    )
}

@Composable
private fun FilaGastoResumen(gasto: GastoPeriodico) {
    val estado = calcularEstadoVisualGasto(gasto)
    FilaMovimientoResumen(
        icono = Icons.Default.Payments,
        titulo = gasto.concepto,
        detalle = textoEstadoGasto(gasto, estado),
        importe = "${formatearDecimal(gasto.importe)} €"
    )
}

@Composable
private fun FilaRecordatorioResumen(
    recordatorio: Recordatorio,
    vehiculo: Vehiculo
) {
    val estado = calcularEstadoVisual(recordatorio, vehiculo)
    FilaMovimientoResumen(
        icono = Icons.Default.Notifications,
        titulo = recordatorio.concepto,
        detalle = textoDetalleRecordatorioResumen(recordatorio, estado),
        importe = null
    )
}

@Composable
private fun FilaMovimientoResumen(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    importe: String?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        importe?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

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

private fun textoDetalleRecordatorioResumen(
    recordatorio: Recordatorio,
    estadoVisual: EstadoVisualRecordatorio
): String {
    val partes = mutableListOf<String>()

    // construimos el texto solo con los limites que existan
    // un recordatorio puede depender de fecha kilometraje o ambas cosas
    recordatorio.fechaLimite?.let { partes.add("Fecha: ${formatearFechaCorta(it)}") }
    recordatorio.kilometrajeLimite?.let { partes.add("Km: ${formatearNumero(it)}") }

    if (partes.isEmpty()) {
        partes.add("Sin límite definido")
    }

    partes.add(
        when (estadoVisual) {
            EstadoVisualRecordatorio.VENCIDO -> "Vencido"
            EstadoVisualRecordatorio.PROXIMO -> "Próximo"
            EstadoVisualRecordatorio.PENDIENTE -> "Pendiente"
            EstadoVisualRecordatorio.COMPLETADO -> "Completado"
        }
    )

    return partes.joinToString(" · ")
}

private fun formatearFechaDetalle(timestamp: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))
    return formato.format(Date(timestamp))
}

fun formatearFecha(timestamp: Long): String {
    return formatearFechaDetalle(timestamp)
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

private fun formatearNumero(valor: Double): String {
    return String.format(Locale("es", "ES"), "%,.0f", valor)
}

private fun formatearDecimal(valor: Double): String {
    return String.format(Locale("es", "ES"), "%.2f", valor)
}
