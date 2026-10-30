package com.lhansqw.senaslsc.core.model

/** Quién generó una propuesta de frase. Se muestra siempre al usuario. */
enum class OrigenInterpretacion {
    /** Intérprete determinista: une significados sin añadir nada. */
    REGLAS,

    /** Modelo de lenguaje ejecutado en el teléfono. */
    LOCAL,

    /** Proveedor remoto, solo con consentimiento y solo con texto. */
    REMOTO,

    /** El usuario escribió o corrigió la frase. Es definitiva: ninguna IA la vuelve a cambiar. */
    USUARIO,
}

/**
 * Frase propuesta a partir de una secuencia de señas confirmadas.
 * Es una propuesta revisable, nunca una traducción confirmada.
 */
data class PropuestaFrase(
    val texto: String,
    /** Identificadores de las señas en que se basa; siempre un subconjunto de la entrada. */
    val senasUsadas: List<String>,
    val incierta: Boolean,
    /** Explicación de la incertidumbre; obligatoria cuando [incierta] es `true`. */
    val motivo: String?,
    val preguntaAclaracion: String?,
    val origen: OrigenInterpretacion,
) {
    init {
        require(!incierta || !motivo.isNullOrBlank()) { "Una propuesta incierta debe explicar el motivo" }
    }
}
