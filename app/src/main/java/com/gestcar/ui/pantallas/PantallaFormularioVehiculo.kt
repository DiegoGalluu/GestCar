package com.gestcar.ui.pantallas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.gestcar.datos.entidades.Vehiculo
import com.gestcar.ui.componentes.BarraSuperiorCompacta
import com.gestcar.ui.viewmodel.VehiculoViewModel
import com.gestcar.util.normalizarMatricula

private val tiposVehiculo = listOf("COCHE", "MOTO", "FURGONETA")
private val tiposCombustible = listOf("Gasolina", "Diesel", "Electrico", "Hibrido", "GLP")

private val mesesFabricacion = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

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

    LaunchedEffect(vehiculoId) {
        if (esNuevo) {
            viewModel.resetearFormulario(usuarioId)
        } else {
            viewModel.cargarParaEditar(vehiculoId)
        }
    }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            alGuardar()
        }
    }

    val vehiculo = estado.vehiculo
    var intentoGuardar by remember { mutableStateOf(false) }
    var vehiculoPendienteConfirmacion by remember { mutableStateOf<Vehiculo?>(null) }
    val marcaVacia = vehiculo.marca.isBlank()
    val modeloVacio = vehiculo.modelo.isBlank()
    val anioFabricacionVacio = vehiculo.anioFabricacion <= 0
    val tipoVacio = vehiculo.tipo.isBlank()
    val matriculaVacia = vehiculo.matricula.isBlank()
    val faltaMarca = intentoGuardar && marcaVacia
    val faltaModelo = intentoGuardar && modeloVacio
    val faltaAnioFabricacion = intentoGuardar && anioFabricacionVacio
    val faltaTipo = intentoGuardar && tipoVacio
    val faltaMatricula = intentoGuardar && matriculaVacia
    val faltanCamposObligatorios = marcaVacia ||
        modeloVacio ||
        anioFabricacionVacio ||
        tipoVacio ||
        matriculaVacia

    Scaffold(
        topBar = {
            BarraSuperiorCompacta(
                titulo = if (esNuevo) "Nuevo vehículo" else "Editar vehículo",
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
                value = vehiculo.marca,
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(marca = it)) },
                label = { Text("Marca *") },
                isError = faltaMarca,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = vehiculo.modelo,
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(modelo = it)) },
                label = { Text("Modelo *") },
                isError = faltaModelo,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = if (vehiculo.anioFabricacion > 0) vehiculo.anioFabricacion.toString() else "",
                onValueChange = {
                    val anio = it.toIntOrNull() ?: 0
                    viewModel.actualizarFormulario(vehiculo.copy(anioFabricacion = anio))
                },
                label = { Text("Año de fabricación *") },
                isError = faltaAnioFabricacion,
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

            SelectorDesplegable(
                etiqueta = "Tipo de vehículo *",
                valorSeleccionado = vehiculo.tipo,
                opciones = tiposVehiculo,
                alSeleccionar = { viewModel.actualizarFormulario(vehiculo.copy(tipo = it)) },
                esError = faltaTipo
            )

            OutlinedTextField(
                value = vehiculo.matricula,
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(matricula = it)) },
                label = { Text("Matrícula *") },
                isError = faltaMatricula,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

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

            SelectorDesplegable(
                etiqueta = "Tipo de combustible",
                valorSeleccionado = vehiculo.tipoCombustible ?: "",
                opciones = tiposCombustible,
                alSeleccionar = { viewModel.actualizarFormulario(vehiculo.copy(tipoCombustible = it)) }
            )

            OutlinedTextField(
                value = vehiculo.notas ?: "",
                onValueChange = { viewModel.actualizarFormulario(vehiculo.copy(notas = it.ifBlank { null })) },
                label = { Text("Notas") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    intentoGuardar = true
                    if (!faltanCamposObligatorios) {
                        val resultadoMatricula = normalizarMatricula(vehiculo.matricula)
                        val vehiculoNormalizado = vehiculo.copy(matricula = resultadoMatricula.matriculaParaGuardar)

                        if (resultadoMatricula.reconocida) {
                            viewModel.guardarVehiculo(vehiculoNormalizado)
                        } else {
                            vehiculoPendienteConfirmacion = vehiculoNormalizado
                        }
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
                    Text("Guardar vehículo")
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

    vehiculoPendienteConfirmacion?.let { vehiculoConfirmado ->
        AlertDialog(
            onDismissRequest = { vehiculoPendienteConfirmacion = null },
            title = { Text("Matrícula no reconocida") },
            text = {
                Text("No reconocemos el formato de esta matrícula. ¿Deseas introducirla de todas maneras?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vehiculoPendienteConfirmacion = null
                        viewModel.guardarVehiculo(vehiculoConfirmado)
                    }
                ) {
                    Text("Sí, guardar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { vehiculoPendienteConfirmacion = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("No, revisar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectorDesplegable(
    etiqueta: String,
    valorSeleccionado: String,
    opciones: List<String>,
    alSeleccionar: (String) -> Unit,
    esError: Boolean = false
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
            isError = esError,
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
