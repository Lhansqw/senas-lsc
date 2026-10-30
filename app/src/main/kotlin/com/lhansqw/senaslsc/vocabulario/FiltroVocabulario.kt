package com.lhansqw.senaslsc.vocabulario

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.core.texto.coincideBusqueda
import com.lhansqw.senaslsc.core.texto.normalizarParaBusqueda

/** Grupo de señas de un mismo tipo, para la pantalla de vocabulario. */
data class GrupoVocabulario(val tipo: TipoSena, val entradas: List<EntradaVocabulario>)

/**
 * Filtra y agrupa el vocabulario del modelo.
 *
 * - La búsqueda mira la glosa y el significado, sin tildes ni mayúsculas.
 * - Los grupos salen en orden fijo (palabras, números, letras) y, dentro de cada uno, por orden
 *   alfabético del significado (los números, por su valor).
 * - Los grupos vacíos no se incluyen.
 */
fun filtrarVocabulario(
    entradas: List<EntradaVocabulario>,
    consulta: String = "",
    tipo: TipoSena? = null,
): List<GrupoVocabulario> {
    val filtradas = entradas.filter { e ->
        (tipo == null || e.tipo == tipo) &&
            (coincideBusqueda(e.significado, consulta) || coincideBusqueda(e.glosa, consulta))
    }
    return ORDEN_TIPOS.mapNotNull { t ->
        filtradas.filter { it.tipo == t }
            .sortedWith(comparadorDentroDeGrupo(t))
            .takeIf { it.isNotEmpty() }
            ?.let { GrupoVocabulario(t, it) }
    }
}

private val ORDEN_TIPOS = listOf(TipoSena.PALABRA, TipoSena.NUMERO, TipoSena.LETRA)

private fun comparadorDentroDeGrupo(tipo: TipoSena): Comparator<EntradaVocabulario> =
    if (tipo == TipoSena.NUMERO) {
        compareBy<EntradaVocabulario> { it.significado.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { normalizarParaBusqueda(it.significado) }
    } else {
        compareBy { normalizarParaBusqueda(it.significado) }
    }
