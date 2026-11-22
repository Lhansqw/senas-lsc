package com.lhansqw.senaslsc.data.history

/** De dónde salió el texto guardado. */
enum class OrigenTexto(val clave: String) {
    REGLAS("reglas"),
    IA_LOCAL("ia_local"),
    IA_REMOTA("ia_remota"),

    /** El usuario escribió o corrigió el texto. */
    USUARIO("usuario"),
    ;

    companion object {
        fun deClave(clave: String): OrigenTexto? = entries.firstOrNull { it.clave == clave }
    }
}

/**
 * Una frase guardada en el historial. Solo texto: nunca imágenes, vídeo ni puntos de referencia.
 *
 * @property glosas señas reconocidas que originaron la frase (p. ej. `HOLA`, `GRACIAS`).
 */
data class EntradaHistorial(
    val id: String,
    val creadaMs: Long,
    val texto: String,
    val glosas: List<String>,
    val origen: OrigenTexto,
) {
    init {
        require(id.isNotBlank()) { "La entrada necesita un identificador" }
        require(creadaMs >= 0) { "Fecha no válida" }
        require(texto.isNotBlank()) { "El texto no puede estar vacío" }
        require(texto.length <= LONGITUD_MAXIMA) { "El texto supera $LONGITUD_MAXIMA caracteres" }
    }

    companion object {
        const val LONGITUD_MAXIMA = 2_000
    }
}
