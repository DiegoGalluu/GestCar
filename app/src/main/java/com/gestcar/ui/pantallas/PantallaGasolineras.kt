package com.gestcar.ui.pantallas

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.remoto.Gasolinera
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.EstadoGasolineras
import com.gestcar.ui.viewmodel.GasolineraViewModel
import com.gestcar.ui.viewmodel.OrdenGasolineras
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun PantallaGasolineras(
    alVolver: () -> Unit,
    viewModel: GasolineraViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    var gasolineraSeleccionada by remember { mutableStateOf<Gasolinera?>(null) }
    var mensajeUbicacion by remember { mutableStateOf<String?>(null) }
    var permisoUbicacionSolicitado by remember { mutableStateOf(false) }
    var mostrarFiltros by remember { mutableStateOf(false) }
    val radios = remember { listOf(5, 10, 30, 50, 100, 200) }
    val indiceRadio = radios.indexOf(estado.radioKm).takeIf { it >= 0 } ?: 0
    val colorBarraSistema = MaterialTheme.colorScheme.primary.toArgb()

    DisposableEffect(colorBarraSistema) {
        val ventana = contexto.encontrarActividad()?.window
        val colorAnterior = ventana?.statusBarColor
        ventana?.statusBarColor = colorBarraSistema

        onDispose {
            if (colorAnterior != null) {
                ventana.statusBarColor = colorAnterior
            }
        }
    }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (concedido) {
            solicitarUbicacionActual(
                contexto = contexto,
                alObtenerUbicacion = { ubicacion ->
                    viewModel.usarUbicacion(ubicacion.latitude, ubicacion.longitude)
                    mensajeUbicacion = null
                },
                alError = { mensaje -> mensajeUbicacion = mensaje }
            )
        } else {
            mensajeUbicacion = "Permiso de ubicación denegado"
        }
    }

    LaunchedEffect(Unit) {
        if (!permisoUbicacionSolicitado) {
            permisoUbicacionSolicitado = true
            if (contexto.tienePermisoUbicacion()) {
                solicitarUbicacionActual(
                    contexto = contexto,
                    alObtenerUbicacion = { ubicacion ->
                        viewModel.usarUbicacion(ubicacion.latitude, ubicacion.longitude)
                        mensajeUbicacion = null
                    },
                    alError = { mensaje -> mensajeUbicacion = mensaje }
                )
            } else {
                lanzadorPermisos.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    LaunchedEffect(estado.gasolinerasFiltradas, gasolineraSeleccionada?.id) {
        val seleccion = gasolineraSeleccionada ?: return@LaunchedEffect
        if (estado.gasolinerasFiltradas.none { it.id == seleccion.id }) {
            gasolineraSeleccionada = null
        }
    }

    if (mostrarFiltros) {
        DialogoFiltrosGasolineras(
            combustibles = estado.combustiblesDisponibles,
            combustibleSeleccionado = estado.combustibleSeleccionado,
            ordenGasolineras = estado.ordenGasolineras,
            alSeleccionarCombustible = viewModel::cambiarFiltroCombustible,
            alSeleccionarOrden = viewModel::cambiarOrdenGasolineras,
            alLimpiar = viewModel::limpiarFiltrosAvanzados,
            alCerrar = { mostrarFiltros = false }
        )
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary)) {
                BarraSuperiorCompacta(
                    titulo = "Gasolineras",
                    alVolver = alVolver
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val mapView = remember { crearMapViewNativo(contexto) }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView },
                    update = { mapa ->
                        actualizarMapaNativo(
                            mapa = mapa,
                            estado = estado,
                            alSeleccionarGasolinera = { gasolineraSeleccionada = it }
                        )
                    }
                )

                DisposableEffect(mapView) {
                    mapView.onResume()
                    onDispose {
                        mapView.onPause()
                        mapView.onDetach()
                    }
                }

                gasolineraSeleccionada?.let { gasolinera ->
                    TarjetaGasolineraSeleccionada(
                        gasolinera = gasolinera,
                        combustibleSeleccionado = estado.combustibleSeleccionado,
                        alIr = { contexto.abrirRutaEnMaps(gasolinera) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }

                FloatingActionButton(
                    onClick = { mostrarFiltros = true },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtrar gasolineras",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                FloatingActionButton(
                    onClick = {
                        if (contexto.tienePermisoUbicacion()) {
                            solicitarUbicacionActual(
                                contexto = contexto,
                                alObtenerUbicacion = { ubicacion ->
                                    viewModel.usarUbicacion(ubicacion.latitude, ubicacion.longitude)
                                    centrarMapaEnUbicacion(
                                        mapa = mapView,
                                        latitud = ubicacion.latitude,
                                        longitud = ubicacion.longitude,
                                        radioKm = estado.radioKm
                                    )
                                    mensajeUbicacion = null
                                },
                                alError = { mensaje -> mensajeUbicacion = mensaje }
                            )
                        } else {
                            lanzadorPermisos.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Usar mi ubicación",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (estado.estaCargando) {
                    Card(
                        modifier = Modifier.align(Alignment.Center),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            Text("Cargando gasolineras")
                        }
                    }
                }
            }

            PanelRadioGasolineras(
                radioKm = estado.radioKm,
                indiceRadio = indiceRadio,
                radios = radios,
                totalGasolineras = estado.totalGasolinerasFiltradas,
                marcadoresMostrados = estado.gasolinerasFiltradas.size,
                combustibleSeleccionado = estado.combustibleSeleccionado,
                ordenGasolineras = estado.ordenGasolineras,
                alCambiarIndice = { indice -> viewModel.cambiarRadio(radios[indice]) }
            )

            estado.mensajeError?.let { mensaje ->
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(mensaje)
                }
            }

            mensajeUbicacion?.let { mensaje ->
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(mensaje)
                }
            }
        }
    }
}

@Composable
private fun DialogoFiltrosGasolineras(
    combustibles: List<String>,
    combustibleSeleccionado: String?,
    ordenGasolineras: OrdenGasolineras,
    alSeleccionarCombustible: (String?) -> Unit,
    alSeleccionarOrden: (OrdenGasolineras) -> Unit,
    alLimpiar: () -> Unit,
    alCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = alCerrar,
        title = { Text("Filtrar gasolineras") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Combustible",
                    style = MaterialTheme.typography.titleSmall
                )

                FilaFiltroGasolinera(
                    texto = "Todos",
                    seleccionado = combustibleSeleccionado == null,
                    habilitado = true,
                    alPulsar = { alSeleccionarCombustible(null) }
                )

                combustibles.forEach { combustible ->
                    FilaFiltroGasolinera(
                        texto = combustible,
                        seleccionado = combustibleSeleccionado == combustible,
                        habilitado = true,
                        alPulsar = { alSeleccionarCombustible(combustible) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Orden",
                    style = MaterialTheme.typography.titleSmall
                )

                FilaFiltroGasolinera(
                    texto = "Más cercanas",
                    seleccionado = ordenGasolineras == OrdenGasolineras.DISTANCIA,
                    habilitado = true,
                    alPulsar = { alSeleccionarOrden(OrdenGasolineras.DISTANCIA) }
                )

                FilaFiltroGasolinera(
                    texto = "Precio más bajo",
                    seleccionado = ordenGasolineras == OrdenGasolineras.PRECIO,
                    habilitado = combustibleSeleccionado != null,
                    alPulsar = { alSeleccionarOrden(OrdenGasolineras.PRECIO) }
                )

                if (combustibleSeleccionado == null) {
                    Text(
                        text = "El orden por precio se activa al elegir un combustible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = alCerrar) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            TextButton(onClick = alLimpiar) {
                Text("Limpiar")
            }
        }
    )
}

@Composable
private fun FilaFiltroGasolinera(
    texto: String,
    seleccionado: Boolean,
    habilitado: Boolean,
    alPulsar: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = seleccionado,
            onClick = if (habilitado) alPulsar else null,
            enabled = habilitado
        )
        Text(
            text = texto,
            color = if (habilitado) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun PanelRadioGasolineras(
    radioKm: Int,
    indiceRadio: Int,
    radios: List<Int>,
    totalGasolineras: Int,
    marcadoresMostrados: Int,
    combustibleSeleccionado: String?,
    ordenGasolineras: OrdenGasolineras,
    alCambiarIndice: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Rango de búsqueda - $radioKm km",
                style = MaterialTheme.typography.titleLarge
            )

            SelectorRadioGasolineras(
                indiceRadio = indiceRadio,
                totalRadios = radios.size,
                alCambiarIndice = alCambiarIndice
            )

            Text(
                text = textoContadorGasolineras(
                    totalGasolineras = totalGasolineras,
                    marcadoresMostrados = marcadoresMostrados,
                    combustibleSeleccionado = combustibleSeleccionado,
                    ordenGasolineras = ordenGasolineras
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            combustibleSeleccionado?.let { combustible ->
                val ordenTexto = if (ordenGasolineras == OrdenGasolineras.PRECIO) {
                    "precio más bajo"
                } else {
                    "más cercanas"
                }
                Text(
                    text = "Filtro: $combustible · $ordenTexto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SelectorRadioGasolineras(
    indiceRadio: Int,
    totalRadios: Int,
    alCambiarIndice: (Int) -> Unit
) {
    var anchoSelectorPx by remember { mutableIntStateOf(0) }
    val densidad = LocalDensity.current
    val tamanoPulgarPx = with(densidad) { 28.dp.roundToPx() }
    val fraccion = if (totalRadios <= 1) {
        0f
    } else {
        indiceRadio.toFloat() / (totalRadios - 1).toFloat()
    }.coerceIn(0f, 1f)

    fun actualizarIndice(posicion: Offset) {
        if (anchoSelectorPx <= 0 || totalRadios <= 1) return
        val proporcion = (posicion.x / anchoSelectorPx).coerceIn(0f, 1f)
        val indice = (proporcion * (totalRadios - 1)).roundToInt().coerceIn(0, totalRadios - 1)
        alCambiarIndice(indice)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .onGloballyPositioned { coordenadas ->
                anchoSelectorPx = coordenadas.size.width
            }
            .pointerInput(totalRadios) {
                awaitEachGesture {
                    val pulsacion = awaitFirstDown()
                    actualizarIndice(pulsacion.position)
                    drag(pulsacion.id) { cambio ->
                        actualizarIndice(cambio.position)
                        cambio.consume()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(fraccion.coerceAtLeast(0.02f))
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(
                        x = (fraccion * anchoSelectorPx - tamanoPulgarPx / 2f).roundToInt(),
                        y = 0
                    )
                }
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun TarjetaGasolineraSeleccionada(
    gasolinera: Gasolinera,
    combustibleSeleccionado: String?,
    alIr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preciosVisibles = remember(gasolinera, combustibleSeleccionado) {
        if (combustibleSeleccionado == null) {
            gasolinera.precios
        } else {
            gasolinera.precios.sortedBy { precio ->
                if (precio.nombre == combustibleSeleccionado) 0 else 1
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = gasolinera.rotulo,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${gasolinera.direccion}, ${gasolinera.municipio}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            preciosVisibles.take(4).forEach { precio ->
                val esCombustibleSeleccionado = precio.nombre == combustibleSeleccionado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = precio.nombre,
                        color = if (esCombustibleSeleccionado) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = String.format(Locale("es", "ES"), "%.2f €", precio.precio),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (preciosVisibles.size > 4) {
                Text(
                    text = "+ ${preciosVisibles.size - 4} combustibles más",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = alIr,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ir")
            }
        }
    }
}

private fun crearMapViewNativo(contexto: Context): MapView {
    Configuration.getInstance().userAgentValue = contexto.packageName
    return MapView(contexto).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        minZoomLevel = 5.0
        maxZoomLevel = 19.0
        controller.setZoom(12.0)
        controller.setCenter(GeoPoint(40.4168, -3.7038))
    }
}

private fun actualizarMapaNativo(
    mapa: MapView,
    estado: EstadoGasolineras,
    alSeleccionarGasolinera: (Gasolinera) -> Unit
) {
    val centro = GeoPoint(estado.centroLatitud, estado.centroLongitud)

    mapa.controller.setZoom(zoomParaRadio(estado.radioKm))
    mapa.controller.setCenter(centro)
    mapa.overlays.clear()
    mapa.overlays.add(crearCirculoBusqueda(centro, estado.radioKm))
    mapa.overlays.add(crearMarcadorCentro(mapa, centro, estado.nombreCentro))
    val iconoGasolinera = crearIconoGasolinera(mapa.context)

    estado.gasolinerasFiltradas.forEach { gasolinera ->
        mapa.overlays.add(
            crearMarcadorGasolinera(
                mapa = mapa,
                gasolinera = gasolinera,
                icono = iconoGasolinera,
                alSeleccionarGasolinera = alSeleccionarGasolinera
            )
        )
    }

    mapa.invalidate()
}

private fun crearCirculoBusqueda(centro: GeoPoint, radioKm: Int): Polygon =
    Polygon().apply {
        points = puntosCirculo(centro, radioKm * 1000.0)
        fillPaint.color = android.graphics.Color.argb(55, 74, 144, 217)
        outlinePaint.color = android.graphics.Color.argb(180, 31, 78, 121)
        outlinePaint.strokeWidth = 3f
    }

private fun crearMarcadorCentro(mapa: MapView, centro: GeoPoint, titulo: String): Marker =
    Marker(mapa).apply {
        position = centro
        title = titulo
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        icon = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(android.graphics.Color.rgb(74, 144, 217))
            setStroke(8, android.graphics.Color.WHITE)
            setSize(34, 34)
        }
    }

private fun crearMarcadorGasolinera(
    mapa: MapView,
    gasolinera: Gasolinera,
    icono: Drawable,
    alSeleccionarGasolinera: (Gasolinera) -> Unit
): Marker =
    Marker(mapa).apply {
        position = GeoPoint(gasolinera.latitud, gasolinera.longitud)
        title = gasolinera.rotulo
        snippet = gasolinera.direccion
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        this.icon = icono.constantState?.newDrawable()?.mutate() ?: icono
        setOnMarkerClickListener { _, _ ->
            alSeleccionarGasolinera(gasolinera)
            true
        }
    }

private fun crearIconoGasolinera(contexto: Context): Drawable {
    val escala = contexto.resources.displayMetrics.density
    val ancho = (42 * escala).roundToInt()
    val alto = (54 * escala).roundToInt()
    val mapaBits = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    val lienzo = Canvas(mapaBits)
    val pintura = Paint(Paint.ANTI_ALIAS_FLAG)

    val azul = android.graphics.Color.rgb(31, 78, 121)
    val azulClaro = android.graphics.Color.rgb(74, 144, 217)
    val blanco = android.graphics.Color.WHITE

    val centroX = ancho / 2f
    val radio = ancho * 0.36f
    val centroY = radio + 3f * escala

    val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(70, 0, 0, 0)
    }
    lienzo.drawCircle(centroX + escala, centroY + escala, radio, sombra)

    pintura.color = azul
    lienzo.drawCircle(centroX, centroY, radio, pintura)

    val punta = Path().apply {
        moveTo(centroX - radio * 0.55f, centroY + radio * 0.45f)
        lineTo(centroX, alto - 4f * escala)
        lineTo(centroX + radio * 0.55f, centroY + radio * 0.45f)
        close()
    }
    lienzo.drawPath(punta, pintura)

    pintura.color = azulClaro
    pintura.style = Paint.Style.STROKE
    pintura.strokeWidth = 2.2f * escala
    lienzo.drawCircle(centroX, centroY, radio - 1.5f * escala, pintura)

    pintura.style = Paint.Style.FILL
    pintura.color = blanco
    val cuerpo = RectF(
        centroX - 7f * escala,
        centroY - 9f * escala,
        centroX + 5f * escala,
        centroY + 9f * escala
    )
    lienzo.drawRoundRect(cuerpo, 2f * escala, 2f * escala, pintura)

    pintura.color = azul
    lienzo.drawRect(
        centroX - 5f * escala,
        centroY - 7f * escala,
        centroX + 3f * escala,
        centroY - 2f * escala,
        pintura
    )

    pintura.color = blanco
    pintura.strokeWidth = 2f * escala
    pintura.style = Paint.Style.STROKE
    val manguera = Path().apply {
        moveTo(centroX + 5f * escala, centroY - 4f * escala)
        cubicTo(
            centroX + 14f * escala,
            centroY - 2f * escala,
            centroX + 13f * escala,
            centroY + 10f * escala,
            centroX + 8f * escala,
            centroY + 8f * escala
        )
    }
    lienzo.drawPath(manguera, pintura)

    return BitmapDrawable(contexto.resources, mapaBits)
}

private fun centrarMapaEnUbicacion(
    mapa: MapView,
    latitud: Double,
    longitud: Double,
    radioKm: Int
) {
    mapa.controller.setZoom(zoomParaRadio(radioKm))
    mapa.controller.animateTo(GeoPoint(latitud, longitud))
}

private fun puntosCirculo(centro: GeoPoint, radioMetros: Double): List<GeoPoint> {
    val radioTierraMetros = 6371000.0
    val latitudCentro = Math.toRadians(centro.latitude)
    val longitudCentro = Math.toRadians(centro.longitude)
    val distanciaAngular = radioMetros / radioTierraMetros

    return (0..96).map { indice ->
        val rumbo = 2.0 * Math.PI * indice / 96.0
        val latitud = asin(
            sin(latitudCentro) * cos(distanciaAngular) +
                cos(latitudCentro) * sin(distanciaAngular) * cos(rumbo)
        )
        val longitud = longitudCentro + atan2(
            sin(rumbo) * sin(distanciaAngular) * cos(latitudCentro),
            cos(distanciaAngular) - sin(latitudCentro) * sin(latitud)
        )

        GeoPoint(Math.toDegrees(latitud), Math.toDegrees(longitud))
    }
}

private fun zoomParaRadio(radioKm: Int): Double =
    when {
        radioKm <= 5 -> 13.0
        radioKm <= 10 -> 12.0
        radioKm <= 30 -> 10.0
        radioKm <= 50 -> 9.0
        radioKm <= 100 -> 8.0
        else -> 7.0
    }

private fun textoContadorGasolineras(
    totalGasolineras: Int,
    marcadoresMostrados: Int,
    combustibleSeleccionado: String?,
    ordenGasolineras: OrdenGasolineras
): String =
    when {
        totalGasolineras == 0 && combustibleSeleccionado != null ->
            "No hay gasolineras con $combustibleSeleccionado en este rango"

        totalGasolineras == 0 -> "No hay gasolineras en este rango"

        totalGasolineras > marcadoresMostrados && ordenGasolineras == OrdenGasolineras.PRECIO ->
            "$totalGasolineras gasolineras encontradas, mostrando las $marcadoresMostrados más baratas"

        totalGasolineras > marcadoresMostrados ->
            "$totalGasolineras gasolineras encontradas, mostrando las $marcadoresMostrados más cercanas"

        else -> "$totalGasolineras gasolineras encontradas"
    }

private fun Context.tienePermisoUbicacion(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun solicitarUbicacionActual(
    contexto: Context,
    alObtenerUbicacion: (Location) -> Unit,
    alError: (String) -> Unit
) {
    if (!contexto.tienePermisoUbicacion()) {
        alError("Permiso de ubicación no concedido")
        return
    }

    val gestorUbicacion = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val ultimaUbicacion = gestorUbicacion.getProviders(true)
        .mapNotNull { proveedor -> gestorUbicacion.getLastKnownLocation(proveedor) }
        .maxByOrNull { it.time }

    ultimaUbicacion?.let(alObtenerUbicacion)

    val proveedor = when {
        gestorUbicacion.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        gestorUbicacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        else -> null
    }

    if (proveedor == null) {
        alError("Activa la ubicación del dispositivo para centrar el mapa")
        return
    }

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            alObtenerUbicacion(location)
            gestorUbicacion.removeUpdates(this)
        }
    }

    gestorUbicacion.requestSingleUpdate(proveedor, listener, Looper.getMainLooper())
}

private fun Context.abrirRutaEnMaps(gasolinera: Gasolinera) {
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=${gasolinera.latitud},${gasolinera.longitud}"
    )
    startActivity(Intent(Intent.ACTION_VIEW, uri))
}

private tailrec fun Context.encontrarActividad(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.encontrarActividad()
        else -> null
    }
