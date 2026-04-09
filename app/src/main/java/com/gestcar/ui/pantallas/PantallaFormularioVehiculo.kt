package com.gestcar.ui.pantallas

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gestcar.ui.viewmodel.VehiculoViewModel

// tipos de vehiculo disponibles
private val tiposVehiculo = listOf("COCHE", "MOTO", "FURGONETA")

// tipos de combustible disponibles
private val tiposCombustible = listOf("Gasolina", "Diesel", "Electrico", "Hibrido", "GLP")

private val mesesFabricacion = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

// pantalla con el formulario para crear o editar un vehiculo
// si vehiculoId es "nuevo" se crea uno nuevo, si no se edita el existente
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormularioVehiculo(
    vehiculoId: String,
    usuarioId: String,
    alGuardar: () -> Unit,
    alVolver: () -> Unit,
    viewModel: VehiculoViewModel = viewModel()
) {
    val estado by viewModel.estadoFormulario.collectAsState()
    val esNuevo = vehiculoId == "nuevo"

    // cargamos el vehiculo si estamos editando, o reseteamos si es nuevo
    LaunchedEffect(vehiculoId) {
        if (esNuevo) {
            viewModel.resetearFormulario(usuarioId)
        } else {
            viewModel.cargarParaEditar(vehiculoId)
        }
    }

    // si se guardo correctamente volvemos a la pantalla anterior
    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            alGuardar()
        }
    }

    val vehiculo = estado.vehiculo

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esNuevo) "Nuevo vehiculo" else "Editar vehiculo") },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "volver")
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
            // campo de marca
            OutlinedTextField(
                value = vehiculo.marca,
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(marca = it)) },
                label = { Text("Marca *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // campo de modelo
            OutlinedTextField(
                value = vehiculo.modelo,
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(modelo = it)) },
                label = { Text("Modelo *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // año obligatorio y fecha parcial/completa opcional
            OutlinedTextField(
                value = if (vehiculo.anioFabricacion > 0) vehiculo.anioFabricacion.toString() else "",
                onValueChange = {
                    val anio = it.toIntOrNull() ?: 0
                    viewModel.actualizarFormulario(vehiculo.copy(anioFabricacion = anio))
                },
                label = { Text("Año de fabricación *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            SelectorDesplegable(
                etiqueta = "Mes de fabricación",
                valorSeleccionado = vehiculo.mesFabricacion?.let { mesesFabricacion[it - 1] } ?: "",
                opciones = mesesFabricacion,
                alSeleccionar = {
                    viewModel.actualizarFormulario(
                        vehiculo.copy(mesFabricacion = mesesFabricacion.indexOf(it) + 1)
                    )
                }
            )

            OutlinedTextField(
                value = vehiculo.diaFabricacion?.toString() ?: "",
                onValueChange = {
                    val dia = it.toIntOrNull()
                    viewModel.actualizarFormulario(
                        vehiculo.copy(
                            diaFabricacion = dia?.takeIf { numero -> numero in 1..31 }
                        )
                    )
                },
                label = { Text("Día de fabricación") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // selector de tipo de vehiculo
            SelectorDesplegable(
                etiqueta = "Tipo de vehiculo *",
                valorSeleccionado = vehiculo.tipo,
                opciones = tiposVehiculo,
                alSeleccionar = { viewModel.actualizarFormulario(vehiculo.copy(tipo = it)) }
            )

            // campo de matricula
            OutlinedTextField(
                value = vehiculo.matricula,
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(matricula = it)) },
                label = { Text("Matricula *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // campo de kilometraje
            OutlinedTextField(
                value = if (vehiculo.kilometraje > 0) vehiculo.kilometraje.toLong().toString() else "",
                onValueChange = {
                    val km = it.toDoubleOrNull() ?: 0.0
                    viewModel.actualizarFormulario(vehiculo.copy(kilometraje = km))
                },
                label = { Text("Kilometraje actual") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // selector de tipo de combustible
            SelectorDesplegable(
                etiqueta = "Tipo de combustible",
                valorSeleccionado = vehiculo.tipoCombustible ?: "",
                opciones = tiposCombustible,
                alSeleccionar = { viewModel.actualizarFormulario(vehiculo.copy(tipoCombustible = it)) }
            )

            // campo de notas
            OutlinedTextField(
                value = vehiculo.notas ?: "",
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(notas = it.ifBlank { null })) },
                label = { Text("Notas (opcional)") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // boton de guardar
            Button(
                onClick = { viewModel.guardarVehiculo() },
                enabled = !estado.estaCargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.estaCargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar vehiculo")
                }
            }

            // mensaje de error si la validacion falla
            estado.mensajeError?.let { error ->
                Snackbar {
                    Text(error)
                }
            }
        }
    }
}

// componente reutilizable para un menu desplegable con opciones
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDesplegable(
    etiqueta: String,
    valorSeleccionado: String,
    opciones: List<String>,
    alSeleccionar: (String) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = !expandido }
    ) {
        OutlinedTextField(
            value = valorSeleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )

        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        alSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}
