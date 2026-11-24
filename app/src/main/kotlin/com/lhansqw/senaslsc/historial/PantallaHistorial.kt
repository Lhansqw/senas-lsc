package com.lhansqw.senaslsc.historial

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.data.history.EntradaHistorial
import com.lhansqw.senaslsc.data.history.FormatoFecha
import com.lhansqw.senaslsc.data.history.OrigenTexto
import com.lhansqw.senaslsc.ui.AccionesTexto
import com.lhansqw.senaslsc.ui.brutal.AvisoBrutal
import com.lhansqw.senaslsc.ui.brutal.BarraSuperiorBrutal
import com.lhansqw.senaslsc.ui.brutal.BotonBrutal
import com.lhansqw.senaslsc.ui.brutal.CampoBrutal
import com.lhansqw.senaslsc.ui.brutal.DialogoBrutal
import com.lhansqw.senaslsc.ui.brutal.FilaInterruptor
import com.lhansqw.senaslsc.ui.brutal.Insignia
import com.lhansqw.senaslsc.ui.brutal.TarjetaBrutal
import com.lhansqw.senaslsc.ui.brutal.TipoAviso
import com.lhansqw.senaslsc.ui.brutal.VarianteBoton
import com.lhansqw.senaslsc.ui.theme.ColoresBrutal
import com.lhansqw.senaslsc.ui.theme.LocalBrutal

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
        topBar = { BarraSuperiorBrutal(stringResource(R.string.historial_titulo), onVolver) },
    ) { relleno ->
        LazyColumn(
            modifier = Modifier
                .padding(relleno)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                TarjetaBrutal {
                    FilaInterruptor(
                        texto = stringResource(R.string.historial_guardar),
                        ayuda = stringResource(R.string.historial_guardar_ayuda),
                        activo = guardar,
                        onCambio = viewModel::establecerGuardar,
                    )
                }
            }
            if (estado.recuperadoDeError) {
                item { AvisoBrutal(stringResource(R.string.historial_recuperado), tipo = TipoAviso.Error) }
            }
            item {
                CampoBrutal(valor = consulta, onCambio = viewModel::buscar, etiqueta = stringResource(R.string.historial_buscar))
            }
            if (estado.cargado && visibles.isEmpty()) {
                item {
                    EstadoVacio(stringResource(if (consulta.isBlank()) R.string.historial_vacio else R.string.historial_sin_resultados))
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
                    BotonBrutal(stringResource(R.string.historial_vaciar), onClick = { confirmarVaciar = true }, variante = VarianteBoton.Texto)
                }
            }
        }
    }

    if (confirmarVaciar) {
        DialogoBrutal(
            titulo = stringResource(R.string.historial_vaciar_titulo),
            texto = stringResource(R.string.historial_vaciar_texto),
            accion = stringResource(R.string.historial_vaciar),
            onAccion = {
                confirmarVaciar = false
                viewModel.vaciar()
            },
            onCerrar = { confirmarVaciar = false },
            peligro = true,
        )
    }
}

/** Caja de borde punteado: aquí irán las frases cuando las haya. */
@Composable
private fun EstadoVacio(texto: String) {
    val brutal = LocalBrutal.current
    Text(
        texto,
        style = MaterialTheme.typography.bodyLarge,
        color = brutal.textoSuave,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val g = brutal.grosor.toPx()
                drawRect(
                    brutal.trazo,
                    topLeft = Offset(g / 2, g / 2),
                    size = Size(size.width - g, size.height - g),
                    style = Stroke(g, pathEffect = PathEffect.dashPathEffect(floatArrayOf(g * 4, g * 3))),
                )
            }
            .padding(24.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaEntrada(entrada: EntradaHistorial, fecha: String, onCopiar: () -> Unit, onEliminar: () -> Unit) {
    TarjetaBrutal(espacio = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                fecha,
                style = MaterialTheme.typography.labelMedium,
                color = LocalBrutal.current.textoSuave,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Insignia(stringResource(etiquetaOrigen(entrada.origen)), color = colorOrigen(entrada.origen))
        }
        Text(entrada.texto, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold))
        if (entrada.glosas.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                entrada.glosas.forEach { glosa ->
                    Text(
                        glosa,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier
                            .border(2.dp, LocalBrutal.current.trazo)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BotonBrutal(stringResource(R.string.accion_copiar), onCopiar, variante = VarianteBoton.Texto)
            BotonBrutal(stringResource(R.string.historial_eliminar), onEliminar, variante = VarianteBoton.Texto)
        }
    }
}

private fun etiquetaOrigen(origen: OrigenTexto): Int = when (origen) {
    OrigenTexto.REGLAS -> R.string.historial_origen_reglas
    OrigenTexto.IA_LOCAL -> R.string.historial_origen_ia_local
    OrigenTexto.IA_REMOTA -> R.string.historial_origen_ia_remota
    OrigenTexto.USUARIO -> R.string.historial_origen_usuario
}

/** Cada origen con su color, para distinguirlos de un vistazo. Reglas va sin color. */
private fun colorOrigen(origen: OrigenTexto): Color? = when (origen) {
    OrigenTexto.REGLAS -> null
    OrigenTexto.IA_LOCAL -> ColoresBrutal.Cian
    OrigenTexto.IA_REMOTA -> ColoresBrutal.Magenta
    OrigenTexto.USUARIO -> ColoresBrutal.Amarillo
}
