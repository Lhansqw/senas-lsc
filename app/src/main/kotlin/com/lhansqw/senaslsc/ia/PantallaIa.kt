package com.lhansqw.senaslsc.ia

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.descargas.Progreso
import com.lhansqw.senaslsc.core.descargas.formatearDuracion
import com.lhansqw.senaslsc.core.descargas.formatearTamano
import com.lhansqw.senaslsc.core.descargas.formatearVelocidad
import com.lhansqw.senaslsc.interpreter.litertlm.DescriptorModelo
import com.lhansqw.senaslsc.interpreter.litertlm.EstadoModeloLenguaje

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaIa(onVolver: () -> Unit, viewModel: IaViewModel = viewModel()) {
    BackHandler(onBack = onVolver)
    val estado by viewModel.estadoModelo.collectAsStateWithLifecycle()
    val usar by viewModel.usarIaLocal.collectAsStateWithLifecycle()
    val consentimiento by viewModel.consentimiento.collectAsStateWithLifecycle()
    val hayClave by viewModel.hayClave.collectAsStateWithLifecycle()
    val modeloGemini by viewModel.modeloGemini.collectAsStateWithLifecycle()
    val preferencia by viewModel.preferencia.collectAsStateWithLifecycle()
    val prueba by viewModel.prueba.collectAsStateWithLifecycle()
    var confirmarDescarga by rememberSaveable { mutableStateOf(false) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    val importar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importar)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ia_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(painterResource(R.drawable.ic_volver), contentDescription = stringResource(R.string.accion_volver))
                    }
                },
            )
        },
    ) { relleno ->
        Column(
            modifier = Modifier
                .padding(relleno)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.ia_explicacion), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.ia_privacidad),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!viewModel.capacidad.interpreteLocal) {
                TarjetaAviso(
                    titulo = stringResource(R.string.ia_no_compatible_titulo),
                    texto = viewModel.capacidad.motivos.joinToString("\n"),
                )
            }

            TarjetaModelo(
                descriptor = viewModel.descriptor,
                estado = estado,
                usar = usar,
                compatible = viewModel.capacidad.interpreteLocal,
                onDescargar = { confirmarDescarga = true },
                onImportar = { importar.launch(arrayOf("*/*")) },
                onCancelar = viewModel::cancelar,
                onEliminar = { confirmarEliminar = true },
                onUsar = viewModel::establecerUsarIaLocal,
            )

            TarjetaInternet(
                consentimiento = consentimiento,
                hayClave = hayClave,
                modelo = modeloGemini,
                preferencia = preferencia,
                prueba = prueba,
                onConsentimiento = viewModel::establecerConsentimiento,
                onGuardarClave = viewModel::guardarClave,
                onBorrarClave = viewModel::borrarClave,
                onModelo = viewModel::establecerModelo,
                onPreferencia = viewModel::establecerPreferencia,
                onProbar = viewModel::probar,
            )
        }
    }

    if (confirmarDescarga) {
        val uri = LocalUriHandler.current
        AlertDialog(
            onDismissRequest = { confirmarDescarga = false },
            title = { Text(stringResource(R.string.ia_descarga_titulo)) },
            text = {
                Text(
                    stringResource(
                        R.string.ia_descarga_texto,
                        viewModel.descriptor.nombre,
                        formatearTamano(viewModel.descriptor.tamanoAproximado),
                        viewModel.descriptor.licencia,
                    ),
                )
            },
            confirmButton = {
                Button(onClick = {
                    confirmarDescarga = false
                    viewModel.descargar()
                }) { Text(stringResource(R.string.ia_descargar)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { uri.openUri(viewModel.descriptor.paginaModelo) }) {
                        Text(stringResource(R.string.ia_ver_licencia))
                    }
                    TextButton(onClick = { confirmarDescarga = false }) { Text(stringResource(R.string.accion_cancelar)) }
                }
            },
        )
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(stringResource(R.string.ia_eliminar_titulo)) },
            text = { Text(stringResource(R.string.ia_eliminar_texto)) },
            confirmButton = {
                Button(onClick = {
                    confirmarEliminar = false
                    viewModel.eliminar()
                }) { Text(stringResource(R.string.ia_eliminar)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text(stringResource(R.string.accion_cancelar)) }
            },
        )
    }
}

@Composable
private fun TarjetaModelo(
    descriptor: DescriptorModelo,
    estado: EstadoModeloLenguaje,
    usar: Boolean,
    compatible: Boolean,
    onDescargar: () -> Unit,
    onImportar: () -> Unit,
    onCancelar: () -> Unit,
    onEliminar: () -> Unit,
    onUsar: (Boolean) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                descriptor.nombre,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.ia_ficha, descriptor.licencia, formatearTamano(descriptor.tamanoAproximado)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (estado) {
                EstadoModeloLenguaje.NoInstalado -> {
                    Text(stringResource(R.string.ia_estado_no_instalado))
                    BotonesInstalar(compatible, stringResource(R.string.ia_descargar), onDescargar, onImportar)
                }
                is EstadoModeloLenguaje.Parcial -> {
                    Text(stringResource(R.string.ia_estado_parcial, formatearTamano(estado.bytes), formatearTamano(estado.total)))
                    BotonesInstalar(compatible, stringResource(R.string.ia_continuar_descarga), onDescargar, onImportar)
                }
                is EstadoModeloLenguaje.Descargando -> Avance(stringResource(R.string.ia_estado_descargando), estado.progreso, onCancelar)
                is EstadoModeloLenguaje.Importando -> Avance(stringResource(R.string.ia_estado_importando), estado.progreso, onCancelar)
                is EstadoModeloLenguaje.Instalado -> {
                    Text(stringResource(R.string.ia_estado_instalado, formatearTamano(estado.bytes)))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.ia_usar), modifier = Modifier.weight(1f))
                        Switch(checked = usar && compatible, onCheckedChange = onUsar, enabled = compatible)
                    }
                    OutlinedButton(onClick = onEliminar) { Text(stringResource(R.string.ia_eliminar)) }
                }
                is EstadoModeloLenguaje.EspacioInsuficiente -> {
                    Text(
                        stringResource(R.string.ia_estado_espacio, formatearTamano(estado.requerido), formatearTamano(estado.libre)),
                        color = MaterialTheme.colorScheme.error,
                    )
                    BotonesInstalar(compatible, stringResource(R.string.ia_reintentar), onDescargar, onImportar)
                }
                is EstadoModeloLenguaje.Error -> {
                    Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                    BotonesInstalar(
                        compatible,
                        stringResource(if (estado.reintentable) R.string.ia_reintentar else R.string.ia_descargar),
                        onDescargar,
                        onImportar,
                    )
                }
            }
        }
    }
}

@Composable
private fun BotonesInstalar(compatible: Boolean, textoDescarga: String, onDescargar: () -> Unit, onImportar: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onDescargar, enabled = compatible) { Text(textoDescarga) }
        OutlinedButton(onClick = onImportar, enabled = compatible) { Text(stringResource(R.string.ia_importar)) }
    }
    Text(
        stringResource(R.string.ia_importar_ayuda),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Avance(titulo: String, progreso: Progreso, onCancelar: () -> Unit) {
    val fraccion = progreso.fraccion
    val detalle = buildList {
        add(progreso.total?.let { "${formatearTamano(progreso.bytes)} / ${formatearTamano(it)}" } ?: formatearTamano(progreso.bytes))
        if (progreso.velocidad > 0) add(formatearVelocidad(progreso.velocidad))
        progreso.segundosRestantes?.let { add(stringResource(R.string.ia_restante, formatearDuracion(it))) }
    }.joinToString(" · ")

    Text(titulo)
    if (fraccion != null) {
        LinearProgressIndicator(
            progress = { fraccion },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { stateDescription = "${(fraccion * 100).toInt()} %" },
        )
    } else {
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    Text(detalle, style = MaterialTheme.typography.bodySmall)
    Text(
        stringResource(R.string.ia_no_cerrar),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = onCancelar) { Text(stringResource(R.string.accion_cancelar)) }
}

@Composable
private fun TarjetaAviso(titulo: String, texto: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.semantics { heading() },
            )
            Text(texto, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
