package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.ExportacionViewModel
import com.gestcar.ui.viewmodel.VehiculoActivoViewModel
import com.gestcar.util.FormatoExportacionVehiculo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMasOpciones(
    usuarioId: String,
    alIrARecordatorios: () -> Unit,
    alIrAEstadisticas: () -> Unit,
    alIrACuenta: () -> Unit,
    alCerrarSesion: () -> Unit,
    vehiculoActivoViewModel: VehiculoActivoViewModel = viewModel(),
    exportacionViewModel: ExportacionViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val estadoVehiculo by vehiculoActivoViewModel.estado.collectAsState()
    val estadoExportacion by exportacionViewModel.estado.collectAsState()
    val vehiculoActivo = estadoVehiculo.vehiculoActivo
    var mostrarDialogoExportacion by remember { mutableStateOf(false) }

    LaunchedEffect(usuarioId) {
        vehiculoActivoViewModel.cargarVehiculos(usuarioId)
    }

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(titulo = "Más opciones")
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OpcionMas(
                titulo = "Cuenta",
                descripcion = "Datos de usuario y opciones de privacidad",
                icono = Icons.Default.Person,
                alPulsar = alIrACuenta
            )

            OpcionMas(
                titulo = "Recordatorios",
                descripcion = "Alertas por fecha o kilometraje",
                icono = Icons.Default.Notifications,
                alPulsar = alIrARecordatorios
            )

            OpcionMas(
                titulo = "Estadísticas",
                descripcion = "Consumos, costes y gráficos",
                icono = Icons.Default.BarChart,
                alPulsar = alIrAEstadisticas
            )

            OpcionMas(
                titulo = "Exportar datos",
                descripcion = "CSV, PDF, Excel o resumen compartible",
                icono = Icons.Default.FileDownload,
                alPulsar = { mostrarDialogoExportacion = true }
            )

            estadoExportacion.mensajeError?.let { mensaje ->
                Snackbar(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(mensaje)
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider()
            Text(
                text = "Cerrar sesión",
                color = Color(0xFFB3261E),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { alCerrarSesion() }
                    .padding(vertical = 20.dp)
            )
        }
    }

    if (mostrarDialogoExportacion) {
        DialogoExportarDatos(
            nombreVehiculo = vehiculoActivo?.let { "${it.marca} ${it.modelo}" } ?: "vehículo activo",
            estaExportando = estadoExportacion.estaExportando,
            alCancelar = { mostrarDialogoExportacion = false },
            alExportar = { formato ->
                mostrarDialogoExportacion = false
                exportacionViewModel.exportarVehiculo(
                    contexto = contexto,
                    vehiculo = vehiculoActivo,
                    formato = formato,
                    alCrearIntent = { contexto.startActivity(it) }
                )
            }
        )
    }
}

@Composable
private fun OpcionMas(
    titulo: String,
    descripcion: String,
    icono: ImageVector,
    alPulsar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { alPulsar() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        ListItem(
            headlineContent = { Text(titulo) },
            supportingContent = { Text(descripcion) },
            leadingContent = {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        )
    }
}

@Composable
private fun DialogoExportarDatos(
    nombreVehiculo: String,
    estaExportando: Boolean,
    alCancelar: () -> Unit,
    alExportar: (FormatoExportacionVehiculo) -> Unit
) {
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("Exportar datos") },
        text = {
            Column {
                Text(
                    text = "Elige cómo quieres exportar la información de $nombreVehiculo.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OpcionFormatoExportacion(
                    titulo = "CSV",
                    descripcion = "Tabla compatible con Excel y Google Sheets",
                    habilitada = !estaExportando,
                    alPulsar = { alExportar(FormatoExportacionVehiculo.CSV) }
                )
                OpcionFormatoExportacion(
                    titulo = "PDF",
                    descripcion = "Informe legible para compartir o guardar",
                    habilitada = !estaExportando,
                    alPulsar = { alExportar(FormatoExportacionVehiculo.PDF) }
                )
                OpcionFormatoExportacion(
                    titulo = "Excel",
                    descripcion = "Archivo compatible con Excel",
                    habilitada = !estaExportando,
                    alPulsar = { alExportar(FormatoExportacionVehiculo.EXCEL) }
                )
                OpcionFormatoExportacion(
                    titulo = "Resumen compartible",
                    descripcion = "Texto corto para WhatsApp, email o notas",
                    habilitada = !estaExportando,
                    alPulsar = { alExportar(FormatoExportacionVehiculo.RESUMEN) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = alCancelar) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun OpcionFormatoExportacion(
    titulo: String,
    descripcion: String,
    habilitada: Boolean,
    alPulsar: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = habilitada) { alPulsar() },
        headlineContent = { Text(titulo) },
        supportingContent = { Text(descripcion) },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    )
}
