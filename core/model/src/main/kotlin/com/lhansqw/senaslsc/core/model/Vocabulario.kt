package com.lhansqw.senaslsc.core.model

enum class TipoSena {
    /** Letra del alfabeto manual (dactilología). Varias seguidas forman una palabra deletreada. */
    LETRA,
    NUMERO,
    PALABRA,
}

/**
 * Una seña que el reconocedor sabe identificar.
 *
 * @property id identificador estable con el prefijo de la lengua, p. ej. `lsc.gracias`.
 * @property glosa forma convencional en mayúsculas con la que se escribe la seña: `GRACIAS`.
 * @property significado texto en español que se muestra al usuario: `gracias`.
 * @property interrogativa la seña convierte la frase en pregunta (CÓMO, QUÉ, DÓNDE…).
 * @property variante región o fuente de la variante validada, si se conoce.
 */
data class EntradaVocabulario(
    val id: String,
    val glosa: String,
    val significado: String,
    val tipo: TipoSena,
    val interrogativa: Boolean = false,
    val variante: String? = null,
) {
    init {
        require(PATRON_ID.matches(id)) { "Identificador no válido: '$id' (formato esperado: lengua.nombre, p. ej. lsc.gracias)" }
        require(glosa.isNotBlank()) { "La seña $id no tiene glosa" }
        require(significado.isNotBlank()) { "La seña $id no tiene significado" }
    }

    /** Prefijo de lengua del identificador: `lsc` en `lsc.gracias`. */
    val lengua: String get() = id.substringBefore('.')

    private companion object {
        val PATRON_ID = Regex("^[a-z]{2,8}\\.[a-z0-9_]+$")
    }
}

/**
 * Conjunto de señas de una lengua que un modelo concreto reconoce.
 * Es la lista que la app publica como «vocabulario compatible».
 */
class Vocabulario(val lengua: String, entradas: List<EntradaVocabulario>) {

    private val porId: Map<String, EntradaVocabulario>

    init {
        require(entradas.isNotEmpty()) { "El vocabulario de '$lengua' está vacío" }
        val ajenas = entradas.filter { it.lengua != lengua }.map { it.id }
        require(ajenas.isEmpty()) { "Señas de otra lengua en el vocabulario de '$lengua': $ajenas" }
        val duplicados = entradas.groupBy { it.id }.filterValues { it.size > 1 }.keys
        require(duplicados.isEmpty()) { "Identificadores repetidos: $duplicados" }
        porId = entradas.associateBy { it.id }
    }

    val entradas: List<EntradaVocabulario> = entradas.toList()
    val tamano: Int get() = entradas.size

    fun buscar(id: String): EntradaVocabulario? = porId[id]
    operator fun contains(id: String): Boolean = id in porId
}
