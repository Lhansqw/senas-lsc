package com.lhansqw.senaslsc.historial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.data.history.EntradaHistorial
import com.lhansqw.senaslsc.data.history.FormatoFecha
import com.lhansqw.senaslsc.data.history.OrigenTexto
import com.lhansqw.senaslsc.ui.AccionesTexto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(onVolver: () -> Unit, viewModel: HistorialViewModel = viewModel()) {
    BackHandler(onBack = onVolver)
    val context = LocalContext.current
    val guardar by viewModel.guardar.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val visibles by viewModel.visibles.collectAsStateWithLifecycle()
    val consulta by viewModel.consulta.collectAsStateWithLifecycle()
    var confirmarVaciar by rememberSaveable { mutableStateOf(false) }
    val ahora = System.currentTimeMillis()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.historial_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(painterResource(R.drawable.ic_volver), contentDescription = stringResource(R.string.accion_volver))
                    }
                },
            )
        },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .padding(relleno)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.historial_guardar), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(R.string.historial_guardar_ayuda),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = guardar, onCheckedChange = viewModel::establecerGuardar)
                }
            }
            if (estado.recuperadoDeError) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            stringResource(R.string.historial_recuperado),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = consulta,
                    onValueChange = viewModel::buscar,
                    label = { Text(stringResource(R.string.historial_buscar)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (estado.cargado && visibles.isEmpty()) {
                item {
                    Text(
                        stringResource(if (consulta.isBlank()) R.string.historial_vacio else R.string.historial_sin_resultados),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(visibles, key = { it.id }) { entrada ->
                TarjetaEntrada(
                    entrada = entrada,
                    fecha = FormatoFecha.relativa(entrada.creadaMs, ahora),
                    onCopiar = { AccionesTexto.copiar(context, entrada.texto) },
                    onEliminar = { viewModel.eliminar(entrada.id) },
                )
            }
            if (estado.entradas.isNotEmpty()) {
                item {
                    TextButton(onClick = { confirmarVaciar = true }) { Text(stringResource(R.string.historial_vaciar)) }
                }
            }
        }
    }

    if (confirmarVaciar) {
        AlertDialog(
            onDismissRequest = { confirmarVaciar = false },
            title = { Text(stringResource(R.string.historial_vaciar_titulo)) },
            text = { Text(stringResource(R.string.historial_vaciar_texto)) },
            confirmButton = {
                Button(onClick = {
                    confirmarVaciar = false
                    viewModel.vaciar()
                }) { Text(stringResource(R.string.historial_vaciar)) }
            },
            dismissButton = { TextButton(onClick = { confirmarVaciar = false }) { Text(stringResource(R.string.accion_cancelar)) } },
        )
    }
}

@Composable
private fun TarjetaEntrada(entrada: EntradaHistorial, fecha: String, onCopiar: () -> Unit, onEliminar: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "$fecha · ${stringResource(etiquetaOrigen(entrada.origen))}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() },
            )
            Text(entrada.texto, style = MaterialTheme.typography.bodyLarge)
            if (entrada.glosas.isNotEmpty()) {
                Text(
                    entrada.glosas.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row {
                TextButton(onClick = onCopiar) { Text(stringResource(R.string.accion_copiar)) }
                TextButton(onClick = onEliminar) { Text(stringResource(R.string.historial_eliminar)) }
            }
        }
    }
}

private fun etiquetaOrigen(origen: OrigenTexto): Int = when (origen) {
    OrigenTexto.REGLAS -> R.string.historial_origen_reglas
    OrigenTexto.IA_LOCAL -> R.string.historial_origen_ia_local
    OrigenTexto.IA_REMOTA -> R.string.historial_origen_ia_remota
    OrigenTexto.USUARIO -> R.string.historial_origen_usuario
}
