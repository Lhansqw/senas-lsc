package com.lhansqw.senaslsc.reconocimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lhansqw.senaslsc.R
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada

private val FONDO = Color.Black.copy(alpha = 0.65f)
private val TEXTO = Color.White
private val TEXTO_SECUNDARIO = Color(0xFFCFD8DC)
private val AVISO = Color(0xFFFFD54F)

/**
 * Resultado del reconocimiento sobre la imagen de la cámara: señas confirmadas, lo que el modelo
 * está viendo y la propuesta de frase, siempre separada y marcada con su origen.
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FONDO, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (val modelo = estado.modelo) {
            EstadoModeloSenas.Cargando -> Texto(stringResource(R.string.reconocedor_cargando))
            EstadoModeloSenas.SinModelo -> {
                Titulo(stringResource(R.string.reconocedor_sin_modelo_titulo))
                Texto(stringResource(R.string.reconocedor_sin_modelo_texto), TEXTO_SECUNDARIO)
            }
            is EstadoModeloSenas.Invalido -> {
                Titulo(stringResource(R.string.reconocedor_invalido_titulo))
                modelo.errores.take(MAXIMO_ERRORES).forEach { Texto("• $it", AVISO) }
            }
            is EstadoModeloSenas.Fallo -> {
                Titulo(stringResource(R.string.reconocedor_fallo_titulo))
                Texto(modelo.mensaje, AVISO)
            }
            is EstadoModeloSenas.Listo -> Contenido(
                modelo, estado, onInterpretar, onBorrarUltima, onLimpiar,
                AccionesPropuesta(onCorregir, onCopiar, onCompartir, onGuardar),
            )
        }
    }
}

@Composable
private fun Contenido(
    modelo: EstadoModeloSenas.Listo,
    estado: EstadoReconocimiento,
    onInterpretar: () -> Unit,
    onBorrarUltima: () -> Unit,
    onLimpiar: () -> Unit,
    acciones: AccionesPropuesta,
) {
    Texto(
        stringResource(R.string.reconocedor_modelo, modelo.nombre, modelo.version, modelo.numeroSenas),
        TEXTO_SECUNDARIO,
    )
    resumenMetricas(modelo.metricas)?.let { Texto(it, TEXTO_SECUNDARIO) }

    val enCurso = textoPrediccionEnCurso(estado.ultimaPrediccion) { id -> modelo.glosas[id] }
    Texto(
        if (enCurso != null) stringResource(R.string.reconocedor_viendo, enCurso) else stringResource(R.string.reconocedor_viendo_nada),
        TEXTO_SECUNDARIO,
    )

    FilaSenas(estado.senas)
    estado.propuesta?.let { Propuesta(it, acciones) }
    if (estado.generando) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp), color = TEXTO)
            Texto(stringResource(R.string.propuesta_generando), TEXTO_SECUNDARIO)
        }
    }
    estado.motivoDescarteIa?.let { motivo ->
        Texto(stringResource(R.string.propuesta_ia_descartada, motivo), TEXTO_SECUNDARIO)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onBorrarUltima, enabled = estado.senas.isNotEmpty()) {
            Text(stringResource(R.string.reconocedor_borrar_ultima))
        }
        TextButton(onClick = onLimpiar, enabled = estado.senas.isNotEmpty() || estado.propuesta != null) {
            Text(stringResource(R.string.reconocedor_limpiar))
        }
        Button(
            onClick = onInterpretar,
            enabled = estado.senas.isNotEmpty() && !estado.secuenciaCerrada,
            modifier = Modifier.weight(1f),
        ) { Text(stringResource(R.string.reconocedor_interpretar)) }
    }
}

@Composable
private fun FilaSenas(senas: List<SenaConfirmada>) {
    if (senas.isEmpty()) {
        Texto(stringResource(R.string.reconocedor_sin_senas), TEXTO_SECUNDARIO)
        return
    }
    val descripcion = stringResource(R.string.reconocedor_senas_lector, listaParaLector(senas))
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .semantics {
                contentDescription = descripcion
                liveRegion = LiveRegionMode.Polite
            },
    ) {
        senas.forEach { sena ->
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    etiquetaSena(sena),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Lo que se puede hacer con la frase propuesta. */
private class AccionesPropuesta(
    val corregir: (String) -> Unit,
    val copiar: (String) -> Unit,
    val compartir: (String) -> Unit,
    val guardar: (() -> Unit)?,
)

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
        Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Texto(origen, TEXTO_SECUNDARIO)
        Text(
            propuesta.texto,
            color = TEXTO,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        if (propuesta.incierta) {
            Texto(stringResource(R.string.propuesta_incierta, propuesta.motivo.orEmpty()), AVISO)
        }
        propuesta.preguntaAclaracion?.let { Texto(it, AVISO) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(onClick = { editando = true }) { Text(stringResource(R.string.accion_editar)) }
        TextButton(onClick = { acciones.copiar(propuesta.texto) }) { Text(stringResource(R.string.accion_copiar)) }
        TextButton(onClick = { acciones.compartir(propuesta.texto) }) { Text(stringResource(R.string.accion_compartir)) }
        acciones.guardar?.let { guardar ->
            TextButton(onClick = guardar) { Text(stringResource(R.string.accion_guardar)) }
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

@Composable
private fun Titulo(texto: String) {
    Text(
        texto,
        color = TEXTO,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun Texto(texto: String, color: Color = TEXTO) {
    Text(texto, color = color, style = MaterialTheme.typography.bodyMedium)
}

private const val MAXIMO_ERRORES = 4
