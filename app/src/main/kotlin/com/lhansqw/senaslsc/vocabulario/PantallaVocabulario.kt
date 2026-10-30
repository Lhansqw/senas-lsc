package com.lhansqw.senaslsc.vocabulario

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.reconocimiento.resumenMetricas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVocabulario(onVolver: () -> Unit, viewModel: VocabularioViewModel = viewModel()) {
    BackHandler(onBack = onVolver)
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val consulta by viewModel.consulta.collectAsStateWithLifecycle()
    val tipo by viewModel.tipo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.vocabulario_titulo)) },
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
                Text(stringResource(R.string.vocabulario_explicacion), style = MaterialTheme.typography.bodyMedium)
            }
            when (val e = estado) {
                EstadoVocabulario.Cargando -> item { Text(stringResource(R.string.vocabulario_cargando)) }
                EstadoVocabulario.SinModelo -> item {
                    Text(stringResource(R.string.vocabulario_sin_modelo), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is EstadoVocabulario.Invalido -> items(e.errores) { Text("• $it", color = MaterialTheme.colorScheme.error) }
                is EstadoVocabulario.Listo -> {
                    item { FichaModelo(e) }
                    item {
                        OutlinedTextField(
                            value = consulta,
                            onValueChange = viewModel::buscar,
                            label = { Text(stringResource(R.string.vocabulario_buscar)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item { FiltroTipos(tipo, viewModel::filtrarTipo) }
                    if (grupos.isEmpty()) {
                        item { Text(stringResource(R.string.vocabulario_sin_resultados)) }
                    }
                    grupos.forEach { grupo ->
                        item(key = "titulo-${grupo.tipo}") {
                            Text(
                                stringResource(nombreTipo(grupo.tipo)) + " (${grupo.entradas.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        items(grupo.entradas, key = { it.id }) { FilaSena(it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FichaModelo(listo: EstadoVocabulario.Listo) {
    val c = listo.contrato
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.vocabulario_modelo, c.nombre, c.version, c.entradas.size), fontWeight = FontWeight.SemiBold)
            resumenMetricas(c.metricas)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            if (c.datosEntrenamiento.isNotEmpty()) {
                Text(
                    stringResource(R.string.vocabulario_datos, c.datosEntrenamiento.joinToString("; ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FiltroTipos(actual: TipoSena?, onCambiar: (TipoSena?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = actual == null, onClick = { onCambiar(null) }, label = { Text(stringResource(R.string.vocabulario_todas)) })
        listOf(TipoSena.PALABRA, TipoSena.NUMERO, TipoSena.LETRA).forEach { t ->
            FilterChip(selected = actual == t, onClick = { onCambiar(t) }, label = { Text(stringResource(nombreTipo(t))) })
        }
    }
}

@Composable
private fun FilaSena(e: EntradaVocabulario) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(e.glosa, fontWeight = FontWeight.Bold)
            Text("«${e.significado}»", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val detalles = buildList {
            if (e.interrogativa) add(stringResource(R.string.vocabulario_interrogativa))
            e.variante?.let { add(stringResource(R.string.vocabulario_variante, it)) }
        }
        if (detalles.isNotEmpty()) {
            Text(detalles.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider()
    }
}

private fun nombreTipo(tipo: TipoSena): Int = when (tipo) {
    TipoSena.PALABRA -> R.string.vocabulario_palabras
    TipoSena.NUMERO -> R.string.vocabulario_numeros
    TipoSena.LETRA -> R.string.vocabulario_letras
}
