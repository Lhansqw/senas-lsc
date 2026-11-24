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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.reconocimiento.resumenMetricas
import com.lhansqw.senaslsc.ui.brutal.AvisoBrutal
import com.lhansqw.senaslsc.ui.brutal.BarraSuperiorBrutal
import com.lhansqw.senaslsc.ui.brutal.CampoBrutal
import com.lhansqw.senaslsc.ui.brutal.FichaFiltro
import com.lhansqw.senaslsc.ui.brutal.Insignia
import com.lhansqw.senaslsc.ui.brutal.Separador
import com.lhansqw.senaslsc.ui.brutal.TarjetaBrutal
import com.lhansqw.senaslsc.ui.brutal.TipoAviso
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal

@Composable
fun PantallaVocabulario(onVolver: () -> Unit, viewModel: VocabularioViewModel = viewModel()) {
    BackHandler(onBack = onVolver)
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val grupos by viewModel.grupos.collectAsStateWithLifecycle()
    val consulta by viewModel.consulta.collectAsStateWithLifecycle()
    val tipo by viewModel.tipo.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraSuperiorBrutal(stringResource(R.string.vocabulario_titulo), onVolver) },
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
                    AvisoBrutal(stringResource(R.string.vocabulario_sin_modelo), tipo = TipoAviso.Info)
                }
                is EstadoVocabulario.Invalido -> item {
                    AvisoBrutal(e.errores.joinToString("\n") { "• $it" }, tipo = TipoAviso.Error)
                }
                is EstadoVocabulario.Listo -> {
                    item { FichaModelo(e) }
                    item {
                        CampoBrutal(valor = consulta, onCambio = viewModel::buscar, etiqueta = stringResource(R.string.vocabulario_buscar))
                    }
                    item { FiltroTipos(tipo, viewModel::filtrarTipo) }
                    if (grupos.isEmpty()) {
                        item { Text(stringResource(R.string.vocabulario_sin_resultados)) }
                    }
                    grupos.forEach { grupo ->
                        item(key = "titulo-${grupo.tipo}") {
                            Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    stringResource(nombreTipo(grupo.tipo)) + " (${grupo.entradas.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.semantics { heading() },
                                )
                                Separador(grosor = LocalBrutal.current.grosor)
                            }
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
    TarjetaBrutal(color = ColoresBrutal.Amarillo, espacio = 4.dp) {
        Text(stringResource(R.string.vocabulario_modelo, c.nombre, c.version, c.visibles.size), style = MaterialTheme.typography.titleSmall)
        resumenMetricas(c.metricas)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        if (c.datosEntrenamiento.isNotEmpty()) {
            Text(
                stringResource(R.string.vocabulario_datos, c.datosEntrenamiento.joinToString("; ")),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FiltroTipos(actual: TipoSena?, onCambiar: (TipoSena?) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FichaFiltro(stringResource(R.string.vocabulario_todas), elegida = actual == null, onClick = { onCambiar(null) })
        listOf(TipoSena.PALABRA, TipoSena.NUMERO, TipoSena.LETRA).forEach { t ->
            FichaFiltro(stringResource(nombreTipo(t)), elegida = actual == t, onClick = { onCambiar(t) })
        }
    }
}

@Composable
private fun FilaSena(e: EntradaVocabulario) {
    val brutal = LocalBrutal.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(e.glosa, style = MaterialTheme.typography.titleMedium)
            Text("«${e.significado}»", color = brutal.textoSuave, modifier = Modifier.weight(1f))
            if (e.interrogativa) Insignia(stringResource(R.string.vocabulario_interrogativa), color = ColoresBrutal.Magenta)
        }
        e.variante?.let {
            Text(stringResource(R.string.vocabulario_variante, it), style = MaterialTheme.typography.bodySmall, color = brutal.textoSuave)
        }
        Separador(Modifier.padding(top = 6.dp))
    }
}

private fun nombreTipo(tipo: TipoSena): Int = when (tipo) {
    TipoSena.PALABRA -> R.string.vocabulario_palabras
    TipoSena.NUMERO -> R.string.vocabulario_numeros
    TipoSena.LETRA -> R.string.vocabulario_letras
}
