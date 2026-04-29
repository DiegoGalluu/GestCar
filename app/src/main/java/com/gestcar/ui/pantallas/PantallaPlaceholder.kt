package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gestcar.ui.componentes.BarraSuperiorCompacta

// pantalla de apoyo para secciones que todavia no tienen vista definitiva
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPlaceholder(
    nombreSeccion: String,
    alVolver: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = nombreSeccion,
                alVolver = alVolver
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
