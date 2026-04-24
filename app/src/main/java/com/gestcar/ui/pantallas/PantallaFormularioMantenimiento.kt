package com.gestcar.ui.pantallas

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronLeft
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.componentes.BarraSuperiorCompacta
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
    var intentoGuardar by remember { mutableStateOf(false) }
    var mostrarSugerencias by remember { mutableStateOf(false) }
    val tipoVacio = mantenimiento.tipo.isBlank()
    val faltaTipo = intentoGuardar && tipoVacio
    val faltanCamposObligatorios = tipoVacio

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
            BarraSuperiorCompacta(
                titulo = if (esNuevo) "Nueva operación" else "Editar operación",
                alVolver = alVolver
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
                label = { Text("Tipo o componente *") },
                isError = faltaTipo,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.clickable { mostrarSugerencias = !mostrarSugerencias },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Sugerencias",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (mostrarSugerencias) "▼" else "▶",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (mostrarSugerencias) {
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
            }

            SelectorCategoriaMantenimiento(
                categoria = mantenimiento.categoria,
                alSeleccionar = { viewModel.actualizarFormulario(mantenimiento.copy(categoria = it)) }
            )

            CampoFecha(
                etiqueta = "Fecha *",
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
                label = { Text("Kilómetros") },
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
                label = { Text("Coste") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mantenimiento.taller ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(mantenimiento.copy(taller = it.ifBlank { null }))
                },
                label = { Text("Taller") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mantenimiento.descripcion ?: "",
                onValueChange = {
                    viewModel.actualizarFormulario(mantenimiento.copy(descripcion = it.ifBlank { null }))
                },
                label = { Text("Comentarios") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    intentoGuardar = true
                    if (!faltanCamposObligatorios) {
                        viewModel.guardarMantenimiento()
                    }
                },
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

            if (intentoGuardar && faltanCamposObligatorios) {
                Snackbar {
                    Text("Faltan campos obligatorios por rellenar")
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
