package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gestcar.ui.componentes.BarraSuperiorCompacta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMasOpciones(
    alIrARecordatorios: () -> Unit,
    alIrAEstadisticas: () -> Unit,
    alCerrarSesion: () -> Unit
) {
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
                descripcion = "Disponible en una próxima iteración",
                icono = Icons.Default.FileDownload,
                alPulsar = {}
            )

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
