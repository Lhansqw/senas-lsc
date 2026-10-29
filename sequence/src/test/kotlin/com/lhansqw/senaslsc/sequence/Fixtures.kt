package com.lhansqw.senaslsc.sequence

import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.Punto3D
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.core.model.Vocabulario

internal val vocabularioPrueba = Vocabulario(
    "lsc",
    listOf(
        EntradaVocabulario("lsc.hola", "HOLA", "hola", TipoSena.PALABRA),
        EntradaVocabulario("lsc.gracias", "GRACIAS", "gracias", TipoSena.PALABRA),
        EntradaVocabulario("lsc.l", "L", "l", TipoSena.LETRA),
    ),
)

private val mano = List(LandmarkFrame.PUNTOS_POR_MANO) { Punto3D(0.5f, 0.5f, 0f) }

internal fun frame(ms: Long, conManos: Boolean = true) =
    LandmarkFrame(ms, manoIzquierda = null, manoDerecha = if (conManos) mano else null, cuerpo = null, rostro = null)

/** Predicción con una candidata principal y, opcionalmente, una segunda. */
internal fun prediccion(
    indice: Int,
    id: String,
    probabilidad: Float,
    segunda: Pair<String, Float>? = null,
    desconocida: Boolean = false,
): PrediccionSena {
    val candidatas = buildList {
        add(Candidata(id, probabilidad))
        segunda?.let { add(Candidata(it.first, it.second)) }
    }
    val inicio = indice * 333L
    return PrediccionSena(inicio, inicio + 1_000, candidatas, desconocida)
}

internal fun desconocida(indice: Int) =
    PrediccionSena(indice * 333L, indice * 333L + 1_000, listOf(Candidata("lsc.hola", 0.3f)), desconocida = true)

internal fun confirmada(id: String, inicioMs: Long, finMs: Long, confianza: Float = 0.9f) =
    SenaConfirmada(vocabularioPrueba.buscar(id)!!, confianza, inicioMs, finMs)
