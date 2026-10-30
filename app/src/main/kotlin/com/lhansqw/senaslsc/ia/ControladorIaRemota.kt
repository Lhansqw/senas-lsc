package com.lhansqw.senaslsc.ia

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.interpreter.GeneradorTexto
import com.lhansqw.senaslsc.interpreter.InterpreteGenerativo
import com.lhansqw.senaslsc.interpreter.ResultadoGenerativo
import com.lhansqw.senaslsc.interpreter.remoto.ClienteGemini
import com.lhansqw.senaslsc.interpreter.rules.InterpreteReglas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * IA de internet (Gemini). Igual que la del teléfono, pasa por [InterpreteGenerativo]: misma
 * plantilla, mismo validador y respaldo por reglas si la respuesta falla o no cumple el contrato.
 * La red se usa siempre en [Dispatchers.IO].
 */
class ControladorIaRemota(private val preferencias: PreferenciasIaRemota) {

    val permitido: Boolean get() = preferencias.permitido

    /** Frase para [senas], o `null` si no está permitido o no hay clave. */
    suspend fun interpretar(senas: List<SenaConfirmada>): ResultadoGenerativo? {
        if (!preferencias.permitido) return null
        val cliente = cliente() ?: return null
        return InterpreteGenerativo(generador(cliente), OrigenInterpretacion.REMOTO, InterpreteReglas()).interpretar(senas)
    }

    /**
     * Prueba la clave y el modelo con una secuencia mínima (HOLA YO). Devuelve `null` si todo fue
     * bien, o el motivo del fallo para mostrarlo. Solo se llama cuando el usuario lo pide.
     */
    suspend fun probar(): String? {
        val cliente = cliente() ?: return "No hay ninguna clave guardada."
        val prueba = listOf(sena("lsc.hola", "HOLA", "hola"), sena("lsc.yo", "YO", "yo"))
        return when (val r = InterpreteGenerativo(generador(cliente), OrigenInterpretacion.REMOTO, InterpreteReglas()).interpretar(prueba)) {
            is ResultadoGenerativo.Aceptada -> null
            is ResultadoGenerativo.Descartada -> r.motivos.firstOrNull() ?: "Gemini no respondió como se esperaba."
            is ResultadoGenerativo.NoAplica -> null
        }
    }

    private fun cliente(): ClienteGemini? {
        val clave = preferencias.leerClave() ?: return null
        return runCatching { ClienteGemini(clave, preferencias.modelo.value) }.getOrNull()
    }

    /** Los errores ([com.lhansqw.senaslsc.interpreter.remoto.ErrorGemini]) llegan con un mensaje en español sin la clave. */
    private fun generador(cliente: ClienteGemini) = GeneradorTexto { instrucciones, mensaje ->
        withContext(Dispatchers.IO) { cliente.generar(instrucciones, mensaje).texto }
    }

    private fun sena(id: String, glosa: String, significado: String) =
        SenaConfirmada(EntradaVocabulario(id, glosa, significado, TipoSena.PALABRA), 0.95f, 0, 100)
}
