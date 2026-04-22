package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.CampoFecha
import com.gestcar.ui.viewmodel.CATEGORIA_MANTENIMIENTO
import com.gestcar.ui.viewmodel.CATEGORIA_REPARACION
import com.gestcar.ui.viewmodel.MantenimientoViewModel
import com.gestcar.ui.viewmodel.tiposMantenimientoPredefinidos

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioMantenimiento(
    vehiculoId: String,
    mantenimientoId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: MantenimientoViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val esNuevo = mantenimientoId == "nuevo"
    val mantenimiento = estado.mantenimiento

    LaunchedEffect(vehiculoId, mantenimientoId) {
        if (esNuevo) {
            viewModel.resetearFormulario(vehiculoId)
        } else {
            viewModel.cargarParaEditar(mantenimientoId)
        }
    }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            alGuardar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esNuevo) "Nueva operación" else "Editar operación") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
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
                value = mantenimiento.tipo,
                onValueChange = { viewModel.actualizarFormulario(mantenimiento.copy(tipo = it)) },
                label = { Text("Tipo o componente (obligatorio)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Sugerencias",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tiposMantenimientoPredefinidos.forEach { sugerencia ->
                    FilterChip(
                        selected = mantenimiento.tipo == sugerencia,
                        onClick = { viewModel.actualizarFormulario(mantenimiento.copy(tipo = sugerencia)) },
                        label = { Text(sugerencia) }
                    )
                }
            }

            SelectorCategoriaMantenimiento(
                categoria = mantenimiento.categoria,
                alSeleccionar = { viewModel.actualizarFormulario(mantenimiento.copy(categoria = it)) }
            )

            CampoFecha(
                etiqueta = "Fecha (obligatorio)",
                fecha = mantenimiento.fecha,
                alSeleccionarFecha = {
                    viewModel.actualizarFormulario(mantenimiento.copy(fecha = it))
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mantenimiento.kilometros?.takeIf { it > 0 }?.toLong()?.toString().orEmpty(),
                onValueChange = {
                    viewModel.actualizarFormulario(
                        mantenimiento.copy(kilometros = it.toDoubleOrNull())
                    )
                },
                label = { Text("Kilómetros (opcional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = if (mantenimiento.coste > 0) mantenimiento.coste.toString() else "",
                onValueChange = {
                    viewModel.actualizarFormulario(
                        mantenimiento.copy(coste = it.replace(",", ".").toDoubleOrNull() ?: 0.0)
                    )
                },
                label = { Text("Coste (opcional, 0 € si no tuvo coste)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mantenimiento.taller ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(mantenimiento.copy(taller = it.ifBlank { null }))
                },
                label = { Text("Taller (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mantenimiento.descripcion ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(mantenimiento.copy(descripcion = it.ifBlank { null }))
                },
                label = { Text("Descripción (opcional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.guardarMantenimiento() },
                enabled = !estado.estaCargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.estaCargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar operación")
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
private fun SelectorCategoriaMantenimiento(
    categoria: String,
    alSeleccionar: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = categoria == CATEGORIA_MANTENIMIENTO,
            onClick = { alSeleccionar(CATEGORIA_MANTENIMIENTO) },
            label = { Text("Mantenimiento") }
        )
        FilterChip(
            selected = categoria == CATEGORIA_REPARACION,
            onClick = { alSeleccionar(CATEGORIA_REPARACION) },
            label = { Text("Reparación") }
        )
    }
}
