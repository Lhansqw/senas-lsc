package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.SenaConfirmada

/**
 * Instrucciones para el modelo de lenguaje, locales o remotas.
 *
 * El modelo recibe solo glosas, significados y confianzas: nunca imágenes ni puntos de referencia.
 * Las mismas reglas se comprueban después con [ValidadorRespuesta], porque un modelo de lenguaje
 * puede no seguir las instrucciones.
 */
object PlantillaPrompt {

    /** Versión de la plantilla; se registra junto con cada evaluación de calidad. */
    const val VERSION = "1"

    val INSTRUCCIONES: String = """
        Eres un asistente que ayuda a convertir señas de la Lengua de Señas Colombiana (LSC),
        ya reconocidas por otro sistema, en una frase en español.

        Reglas obligatorias:
        1. Usa solo las señas de la lista. No añadas señas, nombres ni datos que no aparezcan.
        2. Puedes reordenar las señas y añadir artículos, preposiciones o conjugaciones para que la
           frase sea natural en español: la gramática de la LSC no sigue el orden del español.
        3. No cambies el significado de ninguna seña.
        4. Las letras seguidas forman una palabra deletreada (por ejemplo, un nombre propio).
        5. Si alguna seña tiene confianza menor que %UMBRAL%, marca "incierta": true y explica por
           qué en "motivo".
        6. Si omites una seña porque no encaja, marca "incierta": true y dilo en "motivo".
        7. Si la secuencia admite varias interpretaciones, marca "incierta": true y escribe en
           "pregunta_aclaracion" una pregunta breve para el usuario.
        8. Responde solo con un objeto JSON, sin texto antes ni después, con esta forma:
           {"propuesta": "...", "senas_usadas": ["id", ...], "incierta": false,
            "motivo": null, "pregunta_aclaracion": null}
        9. En "senas_usadas" escribe los identificadores (id) exactos de las señas que usaste.
    """.trimIndent().replace("%UMBRAL%", formatearConfianzaMaquina(ReglasInterpretacion.UMBRAL_SENA_DUDOSA))

    /** Mensaje con la secuencia concreta que se envía tras [INSTRUCCIONES]. */
    fun construirEntrada(secuencia: List<SenaConfirmada>, contexto: String? = null): String {
        require(secuencia.isNotEmpty()) { "La secuencia está vacía" }
        return buildString {
            if (!contexto.isNullOrBlank()) {
                appendLine("Frase anterior (ya revisada por el usuario): \"${contexto.trim()}\"")
                appendLine()
            }
            appendLine("Señas reconocidas, en el orden en que se hicieron:")
            secuencia.forEachIndexed { i, sena ->
                append("${i + 1}. id=${sena.id} | glosa=${sena.entrada.glosa} | ")
                append("significado=${sena.entrada.significado} | tipo=${sena.entrada.tipo.name.lowercase()} | ")
                append("confianza=${formatearConfianzaMaquina(sena.confianza)}")
                if (sena.entrada.interrogativa) append(" | interrogativa")
                appendLine()
            }
        }.trimEnd()
    }
}
