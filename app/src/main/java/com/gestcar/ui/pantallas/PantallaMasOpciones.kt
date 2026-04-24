package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaMasOpciones(
    alIrARecordatorios: () -> Unit,
    alIrAEstadisticas: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Más opciones") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
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
