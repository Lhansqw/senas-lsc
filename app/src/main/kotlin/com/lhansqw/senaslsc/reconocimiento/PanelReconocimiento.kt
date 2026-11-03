package com.lhansqw.senaslsc.reconocimiento

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.ui.theme.ColoresCamara
import com.lhansqw.senaslsc.ui.theme.EstiloCamara
import com.lhansqw.senaslsc.ui.theme.TemaSenasLSC

/** El velo es blando (radio grande); el papel es un recorte (radio pequeño). Así se distinguen. */
private val FormaVelo = RoundedCornerShape(20.dp)
private val FormaPapel = RoundedCornerShape(6.dp)

/**
 * Resultado del reconocimiento sobre la imagen de la cámara: lo que el modelo está viendo, las señas
 * confirmadas y, aparte, en papel, la propuesta de frase marcada con su origen.
 *
 * @param detalles muestra el nombre del modelo y sus métricas (lo activa el diagnóstico de la cámara).
 */
@Composable
fun PanelReconocimiento(
    estado: EstadoReconocimiento,
    onInterpretar: () -> Unit,
    onBorrarUltima: () -> Unit,
    onLimpiar: () -> Unit,
    onCorregir: (String) -> Unit = {},
    onCopiar: (String) -> Unit = {},
    onCompartir: (String) -> Unit = {},
    /** `null` si el historial está desactivado: entonces no se muestra el botón «Guardar». */
    onGuardar: (() -> Unit)? = null,
    detalles: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when (val modelo = estado.modelo) {
            EstadoModeloSenas.Cargando -> Velo { Nota(stringResource(R.string.reconocedor_cargando)) }
            EstadoModeloSenas.SinModelo -> Velo {
                Titulo(stringResource(R.string.reconocedor_sin_modelo_titulo))
                Nota(stringResource(R.string.reconocedor_sin_modelo_texto))
            }
            is EstadoModeloSenas.Invalido -> Velo {
                Titulo(stringResource(R.string.reconocedor_invalido_titulo))
                modelo.errores.take(MAXIMO_ERRORES).forEach { Nota("• $it", ColoresCamara.Ambar) }
            }
            is EstadoModeloSenas.Fallo -> Velo {
                Titulo(stringResource(R.string.reconocedor_fallo_titulo))
                Nota(modelo.mensaje, ColoresCamara.Ambar)
            }
            is EstadoModeloSenas.Listo -> {
                estado.propuesta?.let {
                    Propuesta(it, AccionesPropuesta(onCorregir, onCopiar, onCompartir, onGuardar))
                }
                Velo {
                    if (detalles) DetallesModelo(modelo)
                    EnCurso(senaEnCurso(estado.ultimaPrediccion) { id -> modelo.glosas[id] })
                    Secuencia(estado.senas)
                    EstadoIa(estado)
                    AccionesSecuencia(estado, onInterpretar, onBorrarUltima, onLimpiar)
                }
            }
        }
    }
}

@Composable
private fun Velo(contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColoresCamara.Velo, FormaVelo)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { contenido() }
}

@Composable
private fun DetallesModelo(modelo: EstadoModeloSenas.Listo) {
    Column {
        Text(
            stringResource(R.string.reconocedor_modelo, modelo.nombre, modelo.version, modelo.numeroSenas),
            style = EstiloCamara.cifras,
            color = ColoresCamara.TextoSecundario,
        )
        resumenMetricas(modelo.metricas)?.let {
            Text(it, style = EstiloCamara.cifras, color = ColoresCamara.TextoSecundario)
        }
    }
}

/** La seña que el modelo ve ahora, grande, con una barra fina que muestra su probabilidad. */
@Composable
private fun EnCurso(sena: SenaEnCurso?) {
    val lector = if (sena != null) {
        stringResource(R.string.reconocedor_viendo_lector, sena.glosa, formatearPorcentaje(sena.probabilidad))
    } else {
        stringResource(R.string.reconocedor_viendo_nada_lector)
    }
    val fraccion by animateFloatAsState(
        targetValue = sena?.probabilidad ?: 0f,
        animationSpec = tween(durationMillis = 160),
        label = "confianza",
    )
    Column(
        modifier = Modifier.clearAndSetSemantics { contentDescription = lector },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.reconocedor_viendo_titulo),
                style = EstiloCamara.nota,
                color = ColoresCamara.TextoSecundario,
                modifier = Modifier.padding(bottom = 5.dp),
            )
            Text(
                sena?.glosa ?: "—",
                style = EstiloCamara.glosaEnCurso,
                color = if (sena != null) ColoresCamara.Turquesa else ColoresCamara.TextoApagado,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (sena != null) {
                Text(
                    formatearPorcentaje(sena.probabilidad),
                    style = EstiloCamara.cifras,
                    color = ColoresCamara.TextoSecundario,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(ColoresCamara.PistaConfianza, RoundedCornerShape(2.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraccion)
                    .fillMaxHeight()
                    .background(ColoresCamara.Turquesa, RoundedCornerShape(2.dp)),
            )
        }
    }
}

/** Las señas confirmadas, en orden, como se leerían en una glosa: la última siempre a la vista. */
@Composable
private fun Secuencia(senas: List<SenaConfirmada>) {
    if (senas.isEmpty()) {
        Nota(stringResource(R.string.reconocedor_sin_senas))
        return
    }
    val desplazamiento = rememberScrollState()
    LaunchedEffect(senas.size) { desplazamiento.animateScrollTo(desplazamiento.maxValue) }
    val descripcion = stringResource(R.string.reconocedor_senas_lector, listaParaLector(senas))
    Row(
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(desplazamiento)
            .clearAndSetSemantics {
                contentDescription = descripcion
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        senas.forEach { sena ->
            Column(Modifier.width(IntrinsicSize.Max)) {
                Text(sena.entrada.glosa, style = EstiloCamara.glosa, color = ColoresCamara.Texto)
                Box(
                    Modifier
                        .padding(top = 3.dp, bottom = 2.dp)
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(ColoresCamara.Turquesa),
                )
                Text(formatearPorcentaje(sena.confianza), style = EstiloCamara.cifras, color = ColoresCamara.TextoApagado)
            }
        }
    }
}

@Composable
private fun EstadoIa(estado: EstadoReconocimiento) {
    if (estado.generando) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(14.dp), color = ColoresCamara.Turquesa)
            Nota(stringResource(R.string.propuesta_generando))
        }
    }
    estado.motivoDescarteIa?.let { motivo -> Nota(stringResource(R.string.propuesta_ia_descartada, motivo)) }
}

@Composable
private fun AccionesSecuencia(
    estado: EstadoReconocimiento,
    onInterpretar: () -> Unit,
    onBorrarUltima: () -> Unit,
    onLimpiar: () -> Unit,
) {
    val secundario = ButtonDefaults.textButtonColors(
        contentColor = ColoresCamara.Texto,
        disabledContentColor = ColoresCamara.TextoApagado,
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(onClick = onBorrarUltima, enabled = estado.senas.isNotEmpty(), colors = secundario) {
            Text(stringResource(R.string.reconocedor_borrar_ultima))
        }
        TextButton(
            onClick = onLimpiar,
            enabled = estado.senas.isNotEmpty() || estado.propuesta != null,
            colors = secundario,
        ) { Text(stringResource(R.string.reconocedor_limpiar)) }
        Button(
            onClick = onInterpretar,
            enabled = estado.senas.isNotEmpty() && !estado.secuenciaCerrada,
            colors = ButtonDefaults.buttonColors(
                containerColor = ColoresCamara.Turquesa,
                contentColor = ColoresCamara.Tinta,
                disabledContainerColor = ColoresCamara.PistaConfianza,
                disabledContentColor = ColoresCamara.TextoApagado,
            ),
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.reconocedor_interpretar), fontWeight = FontWeight.Bold) }
    }
}

/** Lo que se puede hacer con la frase propuesta. */
private class AccionesPropuesta(
    val corregir: (String) -> Unit,
    val copiar: (String) -> Unit,
    val compartir: (String) -> Unit,
    val guardar: (() -> Unit)?,
)

/** La frase propuesta, en papel: un texto aparte, legible por la otra persona, que se revisa antes de usarlo. */
@Composable
private fun Propuesta(propuesta: PropuestaFrase, acciones: AccionesPropuesta) {
    var editando by rememberSaveable { mutableStateOf(false) }
    val origen = stringResource(
        when (propuesta.origen) {
            OrigenInterpretacion.REGLAS -> R.string.propuesta_origen_reglas
            OrigenInterpretacion.LOCAL -> R.string.propuesta_origen_local
            OrigenInterpretacion.REMOTO -> R.string.propuesta_origen_remoto
            OrigenInterpretacion.USUARIO -> R.string.propuesta_origen_usuario
        },
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColoresCamara.Papel, FormaPapel)
            .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Column(
            Modifier
                .padding(end = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(origen, style = EstiloCamara.nota, color = ColoresCamara.PapelTextoSecundario)
            Text(propuesta.texto, style = EstiloCamara.frase, color = ColoresCamara.PapelTexto)
            if (propuesta.incierta) {
                AvisoPapel(stringResource(R.string.propuesta_incierta, propuesta.motivo.orEmpty()))
            }
            propuesta.preguntaAclaracion?.let { AvisoPapel(it) }
        }
        val accion = ButtonDefaults.textButtonColors(contentColor = ColoresCamara.PapelAccion)
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 4.dp),
        ) {
            TextButton(onClick = { editando = true }, colors = accion) {
                Text(stringResource(R.string.accion_editar), fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { acciones.copiar(propuesta.texto) }, colors = accion) {
                Text(stringResource(R.string.accion_copiar))
            }
            TextButton(onClick = { acciones.compartir(propuesta.texto) }, colors = accion) {
                Text(stringResource(R.string.accion_compartir))
            }
            acciones.guardar?.let { guardar ->
                TextButton(onClick = guardar, colors = accion) { Text(stringResource(R.string.accion_guardar)) }
            }
        }
    }
    if (editando) {
        DialogoEdicion(
            textoInicial = propuesta.texto,
            onConfirmar = {
                acciones.corregir(it)
                editando = false
            },
            onCancelar = { editando = false },
        )
    }
}

/** Aviso de incertidumbre sobre el papel: una regla ámbar a la izquierda, no un recuadro más. */
@Composable
private fun AvisoPapel(texto: String) {
    Row(Modifier.height(IntrinsicSize.Min).padding(top = 2.dp)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(ColoresCamara.PapelAviso))
        Text(
            texto,
            style = EstiloCamara.nota,
            color = ColoresCamara.PapelAviso,
            modifier = Modifier
                .background(ColoresCamara.PapelAvisoFondo)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        color = ColoresCamara.Texto,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Nota(texto: String, color: Color = ColoresCamara.TextoSecundario) {
    Text(texto, color = color, style = EstiloCamara.nota)
}

private const val MAXIMO_ERRORES = 4

@Preview(name = "Secuencia con propuesta incierta", backgroundColor = 0xFF5A6B70, showBackground = true, widthDp = 380)
@Composable
private fun VistaPreviaPanel() {
    fun entrada(glosa: String) = EntradaVocabulario("lsc.${glosa.lowercase()}", glosa, glosa.lowercase(), TipoSena.PALABRA)
    val estado = EstadoReconocimiento(
        modelo = EstadoModeloSenas.Listo("lsc70", "1.0.0", 42, mapOf("exactitud" to 0.907f), mapOf("lsc.gracias" to "GRACIAS")),
        ultimaPrediccion = PrediccionSena(0, 100, listOf(Candidata("lsc.gracias", 0.82f)), desconocida = false),
        senas = listOf(SenaConfirmada(entrada("HOLA"), 0.94f, 0, 1), SenaConfirmada(entrada("YO"), 0.88f, 2, 3), SenaConfirmada(entrada("GUSTAR"), 0.71f, 4, 5)),
        propuesta = PropuestaFrase(
            texto = "Hola, me gusta.",
            senasUsadas = listOf("lsc.hola", "lsc.yo", "lsc.gustar"),
            origen = OrigenInterpretacion.REGLAS,
            incierta = true,
            motivo = "GUSTAR tiene confianza baja",
            preguntaAclaracion = null,
        ),
    )
    TemaSenasLSC(oscuro = true, colorDinamico = false) {
        Box(Modifier.padding(12.dp)) {
            PanelReconocimiento(estado, {}, {}, {}, onGuardar = {}, detalles = true)
        }
    }
}
