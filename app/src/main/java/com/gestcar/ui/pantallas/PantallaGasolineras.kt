package com.gestcar.ui.pantallas

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.datos.remoto.Gasolinera
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.GasolineraViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

@Composable
fun PantallaGasolineras(
    alVolver: () -> Unit,
    viewModel: GasolineraViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val estado by viewModel.estado.collectAsState()
    var mapaCargado by remember { mutableStateOf(false) }
    val radios = remember { listOf(5, 10, 30, 50, 100, 200) }
    val indiceRadio = radios.indexOf(estado.radioKm).coerceAtLeast(0)

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (concedido) {
            obtenerUltimaUbicacion(contexto)?.let {
                viewModel.usarUbicacion(it.latitude, it.longitude)
            }
        }
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = "Gasolineras",
                alVolver = alVolver
            )
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
                val marcadoresJson = remember(estado.gasolinerasFiltradas) {
                    Json.encodeToString(estado.gasolinerasFiltradas.aMarcadoresMapa())
                }

                val webView = remember {
                    crearWebViewMapa(
                        contexto = contexto,
                        alMapaCargado = { mapaCargado = true }
                    )
                }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webView }
                )

                LaunchedEffect(
                    mapaCargado,
                    marcadoresJson,
                    estado.centroLatitud,
                    estado.centroLongitud,
                    estado.radioKm,
                    estado.nombreCentro
                ) {
                    if (mapaCargado) {
                        val nombreCentro = Json.encodeToString(estado.nombreCentro)
                        webView.evaluateJavascript(
                            """
                            window.actualizarMapa(
                                ${estado.centroLongitud},
                                ${estado.centroLatitud},
                                ${estado.radioKm},
                                $nombreCentro,
                                $marcadoresJson
                            );
                            """.trimIndent(),
                            null
                        )
                    }
                }

                DisposableEffect(webView) {
                    onDispose { webView.destroy() }
                }

                FloatingActionButton(
                    onClick = {
                        if (contexto.tienePermisoUbicacion()) {
                            obtenerUltimaUbicacion(contexto)?.let {
                                viewModel.usarUbicacion(it.latitude, it.longitude)
                            }
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
                            CircularProgressIndicator(modifier = Modifier.height(28.dp))
                            Text("Cargando gasolineras")
                        }
                    }
                }
            }

            PanelRadioGasolineras(
                radioKm = estado.radioKm,
                indiceRadio = indiceRadio,
                radios = radios,
                totalGasolineras = estado.gasolinerasFiltradas.size,
                alCambiarIndice = { indice -> viewModel.cambiarRadio(radios[indice]) },
                alAplicar = { viewModel.aplicarFiltro() }
            )

            estado.mensajeError?.let { mensaje ->
                Snackbar(modifier = Modifier.padding(16.dp)) {
                    Text(mensaje)
                }
            }
        }
    }
}

@Composable
private fun PanelRadioGasolineras(
    radioKm: Int,
    indiceRadio: Int,
    radios: List<Int>,
    totalGasolineras: Int,
    alCambiarIndice: (Int) -> Unit,
    alAplicar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

            Slider(
                value = indiceRadio.toFloat(),
                onValueChange = { valor ->
                    alCambiarIndice(valor.toInt().coerceIn(radios.indices))
                },
                valueRange = 0f..(radios.size - 1).toFloat(),
                steps = radios.size - 2
            )

            Text(
                text = "$totalGasolineras gasolineras encontradas",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = alAplicar,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun crearWebViewMapa(
    contexto: Context,
    alMapaCargado: () -> Unit
): WebView =
    WebView(contexto).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadsImagesAutomatically = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                alMapaCargado()
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url ?: return false
                if (url.host?.contains("google.com") == true || url.scheme == "geo") {
                    contexto.startActivity(Intent(Intent.ACTION_VIEW, url))
                    return true
                }
                return false
            }
        }
        loadDataWithBaseURL(
            "https://gestcar.local",
            htmlMapa(),
            "text/html",
            "UTF-8",
            null
        )
    }

private fun List<Gasolinera>.aMarcadoresMapa(): List<MarcadorGasolineraMapa> =
    map { gasolinera ->
        MarcadorGasolineraMapa(
            id = gasolinera.id,
            rotulo = gasolinera.rotulo,
            direccion = "${gasolinera.direccion}, ${gasolinera.municipio}",
            latitud = gasolinera.latitud,
            longitud = gasolinera.longitud,
            horario = gasolinera.horario,
            precios = gasolinera.precios.map {
                PrecioGasolineraMapa(
                    nombre = it.nombre,
                    precio = String.format(Locale("es", "ES"), "%.2f €", it.precio)
                )
            }
        )
    }

@Serializable
private data class MarcadorGasolineraMapa(
    val id: String,
    val rotulo: String,
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val horario: String,
    val precios: List<PrecioGasolineraMapa>
)

@Serializable
private data class PrecioGasolineraMapa(
    val nombre: String,
    val precio: String
)

private fun Context.tienePermisoUbicacion(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun obtenerUltimaUbicacion(contexto: Context): Location? {
    if (!contexto.tienePermisoUbicacion()) return null
    val gestorUbicacion = contexto.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return gestorUbicacion.getProviders(true)
        .mapNotNull { proveedor -> gestorUbicacion.getLastKnownLocation(proveedor) }
        .maxByOrNull { it.time }
}

private fun htmlMapa(): String =
    """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <link href="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.css" rel="stylesheet" />
        <script src="https://unpkg.com/maplibre-gl@4.7.1/dist/maplibre-gl.js"></script>
        <style>
            html, body, #mapa {
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                background: #f4f4f4;
                font-family: sans-serif;
            }
            .marcador {
                width: 18px;
                height: 18px;
                border-radius: 50%;
                background: #1F4E79;
                border: 3px solid white;
                box-shadow: 0 2px 8px rgba(0,0,0,.35);
            }
            .maplibregl-popup-content {
                border-radius: 14px;
                padding: 14px;
                min-width: 210px;
                box-shadow: 0 8px 24px rgba(0,0,0,.25);
            }
            .titulo {
                font-weight: 700;
                font-size: 15px;
                margin-bottom: 4px;
                color: #1C1B1F;
            }
            .direccion {
                font-size: 12px;
                color: #555;
                margin-bottom: 10px;
            }
            .precio {
                display: flex;
                justify-content: space-between;
                gap: 16px;
                padding: 3px 0;
                font-size: 13px;
            }
            .precio strong {
                color: #1F4E79;
            }
            .boton-ir {
                display: block;
                margin-top: 12px;
                padding: 9px 12px;
                border-radius: 999px;
                background: #1F4E79;
                color: white;
                text-decoration: none;
                text-align: center;
                font-weight: 700;
            }
        </style>
    </head>
    <body>
        <div id="mapa"></div>
        <script>
            const estilo = {
                version: 8,
                sources: {
                    osm: {
                        type: "raster",
                        tiles: ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
                        tileSize: 256,
                        attribution: "© OpenStreetMap contributors"
                    }
                },
                layers: [
                    { id: "osm", type: "raster", source: "osm" }
                ]
            };

            const mapa = new maplibregl.Map({
                container: "mapa",
                style: estilo,
                center: [-3.7038, 40.4168],
                zoom: 10
            });

            mapa.addControl(new maplibregl.NavigationControl({ showCompass: false }), "bottom-right");
            let marcadores = [];

            mapa.on("load", () => {
                mapa.addSource("radio", {
                    type: "geojson",
                    data: crearGeoJsonCirculo([-3.7038, 40.4168], 10)
                });

                mapa.addLayer({
                    id: "radio-relleno",
                    type: "fill",
                    source: "radio",
                    paint: {
                        "fill-color": "#4A90D9",
                        "fill-opacity": 0.28
                    }
                });

                mapa.addLayer({
                    id: "radio-borde",
                    type: "line",
                    source: "radio",
                    paint: {
                        "line-color": "#1F4E79",
                        "line-opacity": 0.45,
                        "line-width": 2
                    }
                });
            });

            window.actualizarMapa = function(longitud, latitud, radioKm, nombreCentro, estaciones) {
                const centro = [longitud, latitud];
                mapa.flyTo({ center: centro, zoom: zoomParaRadio(radioKm), essential: true });

                const fuenteRadio = mapa.getSource("radio");
                if (fuenteRadio) {
                    fuenteRadio.setData(crearGeoJsonCirculo(centro, radioKm));
                }

                marcadores.forEach((marcador) => marcador.remove());
                marcadores = [];

                estaciones.forEach((estacion) => {
                    const elemento = document.createElement("div");
                    elemento.className = "marcador";

                    const popup = new maplibregl.Popup({ offset: 18 }).setHTML(htmlPopup(estacion));
                    const marcador = new maplibregl.Marker({ element: elemento })
                        .setLngLat([estacion.longitud, estacion.latitud])
                        .setPopup(popup)
                        .addTo(mapa);
                    marcadores.push(marcador);
                });
            };

            function htmlPopup(estacion) {
                const precios = estacion.precios.length === 0
                    ? "<div class='direccion'>Sin precios disponibles</div>"
                    : estacion.precios.map((precio) =>
                        "<div class='precio'><span>" + escapar(precio.nombre) + "</span><strong>" + escapar(precio.precio) + "</strong></div>"
                    ).join("");

                const url = "https://www.google.com/maps/dir/?api=1&destination=" + estacion.latitud + "," + estacion.longitud;
                return "<div class='titulo'>" + escapar(estacion.rotulo) + "</div>" +
                    "<div class='direccion'>" + escapar(estacion.direccion) + "</div>" +
                    precios +
                    "<a class='boton-ir' href='" + url + "'>Ir</a>";
            }

            function escapar(valor) {
                return String(valor || "")
                    .replace(/&/g, "&amp;")
                    .replace(/</g, "&lt;")
                    .replace(/>/g, "&gt;")
                    .replace(/"/g, "&quot;")
                    .replace(/'/g, "&#039;");
            }

            function zoomParaRadio(radioKm) {
                if (radioKm <= 5) return 12;
                if (radioKm <= 10) return 11;
                if (radioKm <= 30) return 9.4;
                if (radioKm <= 50) return 8.6;
                if (radioKm <= 100) return 7.7;
                return 6.7;
            }

            function crearGeoJsonCirculo(centro, radioKm) {
                const puntos = 96;
                const coordenadas = [];
                const distancia = radioKm / 6371;
                const latitud = centro[1] * Math.PI / 180;
                const longitud = centro[0] * Math.PI / 180;

                for (let i = 0; i <= puntos; i++) {
                    const rumbo = 2 * Math.PI * i / puntos;
                    const latitudPunto = Math.asin(
                        Math.sin(latitud) * Math.cos(distancia) +
                        Math.cos(latitud) * Math.sin(distancia) * Math.cos(rumbo)
                    );
                    const longitudPunto = longitud + Math.atan2(
                        Math.sin(rumbo) * Math.sin(distancia) * Math.cos(latitud),
                        Math.cos(distancia) - Math.sin(latitud) * Math.sin(latitudPunto)
                    );
                    coordenadas.push([longitudPunto * 180 / Math.PI, latitudPunto * 180 / Math.PI]);
                }

                return {
                    type: "Feature",
                    geometry: {
                        type: "Polygon",
                        coordinates: [coordenadas]
                    }
                };
            }
        </script>
    </body>
    </html>
    """.trimIndent()
