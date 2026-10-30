package com.lhansqw.senaslsc.interpreter.remoto

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.interpreter.GeneradorTexto
import com.lhansqw.senaslsc.interpreter.InterpreteGenerativo
import com.lhansqw.senaslsc.interpreter.PlantillaPrompt
import com.lhansqw.senaslsc.interpreter.ResultadoGenerativo
import com.lhansqw.senaslsc.interpreter.rules.InterpreteReglas
import java.io.File
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.system.exitProcess

/**
 * Comparativa de la fase 10: intérprete por reglas frente a Gemini con las mismas secuencias.
 *
 * Mide lo que se puede medir sin una persona: si la respuesta cumple el contrato (la acepta el
 * validador), la latencia y los tokens (coste). La **calidad** de cada frase la juzga una persona:
 * el informe pone las dos propuestas una al lado de la otra para revisarlas.
 */

/** Una secuencia de prueba con señas del vocabulario de LSC70. */
data class Caso(val descripcion: String, val senas: List<SenaConfirmada>)

data class ResultadoCaso(
    val caso: Caso,
    val reglas: String,
    val gemini: ResultadoGenerativo,
    val respuesta: RespuestaGemini?,
    val error: String?,
)

/** Precio por millón de tokens (USD, nivel de pago) según la página oficial en octubre de 2026. */
data class Precio(val entrada: Double, val salida: Double)

val PRECIOS = mapOf(
    "gemini-3.5-flash-lite" to Precio(0.30, 2.50),
    "gemini-3.1-flash-lite" to Precio(0.25, 1.50),
    "gemini-3.8-flash" to Precio(0.75, 3.75),
)

private fun sena(glosa: String, tipo: TipoSena = TipoSena.PALABRA, confianza: Float = 0.9f, significado: String = glosa.lowercase()): SenaConfirmada {
    val id = "lsc." + glosa.lowercase().replace("í", "i").replace("ñ", "n").let { if (tipo == TipoSena.NUMERO) "n$it" else it }
    return SenaConfirmada(EntradaVocabulario(id, glosa, significado, tipo), confianza, 0, 100)
}

private fun deletreo(palabra: String) = palabra.map { sena(it.toString(), TipoSena.LETRA, significado = it.lowercase()) }

/** Secuencias con las 42 señas de `vocabulario.lsc70.json`. */
val CASOS = listOf(
    Caso("Saludo y nombre deletreado", listOf(sena("HOLA"), sena("YO"), sena("NOMBRE")) + deletreo("ANA")),
    Caso("Buenas noches", listOf(sena("BUENAS"), sena("NOCHES"))),
    Caso("Buenas tardes", listOf(sena("BUENAS"), sena("TARDES"))),
    Caso("Edad", listOf(sena("YO"), sena("AÑOS"), sena("10", TipoSena.NUMERO, significado = "10"))),
    Caso("Saludo completo", listOf(sena("HOLA"), sena("BUENAS"), sena("TARDES"), sena("YO"), sena("NOMBRE")) + deletreo("LUIS")),
    Caso("Orden de la LSC: nombre al final", listOf(sena("NOMBRE"), sena("YO")) + deletreo("MARIA")),
    Caso("Seña dudosa", listOf(sena("HOLA", confianza = 0.55f), sena("YO"), sena("NOMBRE")) + deletreo("SOL")),
    Caso("Edad con número pequeño", listOf(sena("YO"), sena("5", TipoSena.NUMERO, significado = "5"), sena("AÑOS"))),
    Caso("Días", listOf(sena("BUENAS"), sena("DÍAS", significado = "días"))),
    Caso("Solo deletreo", deletreo("CASA")),
)

/** Ejecuta una función suspendida sin puntos de suspensión reales (todo aquí es bloqueante). */
private fun <T> bloqueando(bloque: suspend () -> T): T {
    var resultado: Result<T>? = null
    bloque.startCoroutine(Continuation(EmptyCoroutineContext) { resultado = it })
    return resultado!!.getOrThrow()
}

fun comparar(casos: List<Caso>, llamar: (String, String) -> RespuestaGemini): List<ResultadoCaso> {
    val reglas = InterpreteReglas()
    return casos.map { caso ->
        var respuesta: RespuestaGemini? = null
        var error: String? = null
        val generador = GeneradorTexto { instrucciones, mensaje ->
            try {
                llamar(instrucciones, mensaje).also { respuesta = it }.texto
            } catch (e: ErrorGemini) {
                error = e.message
                throw e
            }
        }
        val gemini = bloqueando { InterpreteGenerativo(generador, OrigenInterpretacion.REMOTO, reglas).interpretar(caso.senas) }
        ResultadoCaso(caso, bloqueando { reglas.proponer(caso.senas, null) }.texto, gemini, respuesta, error)
    }
}

private fun percentil(valores: List<Long>, p: Int): Long {
    val o = valores.sorted()
    return o[((p * o.size + 99) / 100).coerceIn(1, o.size) - 1]
}

fun informe(modelo: String, resultados: List<ResultadoCaso>): String {
    val es = Locale.forLanguageTag("es-CO")
    val consultados = resultados.filter { it.gemini !is ResultadoGenerativo.NoAplica }
    val aceptados = consultados.count { it.gemini is ResultadoGenerativo.Aceptada }
    val respuestas = consultados.mapNotNull { it.respuesta }
    val sb = StringBuilder()
    sb.appendLine("# Comparativa: reglas frente a $modelo").appendLine()
    sb.appendLine("Plantilla de instrucciones versión ${PlantillaPrompt.VERSION}. ${consultados.size} secuencias enviadas a Gemini.").appendLine()
    sb.appendLine("| Métrica | Valor |").appendLine("|---|---|")
    sb.appendLine("| Respuestas que cumplen el contrato | $aceptados de ${consultados.size} |")
    sb.appendLine("| Errores de la API | ${consultados.count { it.error != null }} |")
    if (respuestas.isNotEmpty()) {
        val lat = respuestas.map { it.latenciaMs }
        sb.appendLine("| Latencia p50 / p95 | ${percentil(lat, 50)} ms / ${percentil(lat, 95)} ms |")
        val ent = respuestas.sumOf { it.tokensEntrada } / respuestas.size
        val sal = respuestas.sumOf { it.tokensSalida } / respuestas.size
        sb.appendLine("| Tokens medios por frase (entrada / salida) | $ent / $sal |")
        PRECIOS[modelo]?.let { p ->
            val costeMil = 1000 * (ent * p.entrada + sal * p.salida) / 1_000_000
            sb.appendLine("| Coste estimado cada 1000 frases (nivel de pago) | ${String.format(es, "%.3f", costeMil)} USD |")
        }
    }
    sb.appendLine("| Intérprete por reglas | inmediato, sin conexión, sin coste |").appendLine()
    sb.appendLine("## Frases para revisar").appendLine()
    sb.appendLine("| Caso | Señas | Reglas | Gemini | ¿Cumple? | ms |").appendLine("|---|---|---|---|---|---|")
    resultados.forEach { r ->
        val senas = r.caso.senas.joinToString(" ") { it.entrada.glosa }
        val (texto, estado) = when (val g = r.gemini) {
            is ResultadoGenerativo.Aceptada -> g.propuesta.texto to "sí"
            is ResultadoGenerativo.Descartada -> "—" to "no: ${g.motivos.firstOrNull().orEmpty().take(80)}"
            is ResultadoGenerativo.NoAplica -> "—" to "no se consulta"
        }
        sb.appendLine("| ${r.caso.descripcion} | $senas | ${r.reglas} | $texto | $estado | ${r.respuesta?.latenciaMs ?: "—"} |")
    }
    return sb.toString()
}

/** `gradlew.bat :interpreter:remoto:compararGemini`, con la clave en GEMINI_API_KEY. */
fun main(args: Array<String>) {
    val clave = System.getenv("GEMINI_API_KEY")
    if (clave.isNullOrBlank()) {
        System.err.println("Falta la variable de entorno GEMINI_API_KEY (en PowerShell: \$env:GEMINI_API_KEY=\"tu-clave\").")
        exitProcess(2)
    }
    val salida = File(args.getOrElse(0) { "comparativa-gemini.md" })
    val modelo = args.getOrElse(1) { ClienteGemini.MODELO_PREDETERMINADO }
    val cliente = ClienteGemini(clave, modelo)
    println("Enviando ${CASOS.size} secuencias a $modelo…")
    val texto = informe(modelo, comparar(CASOS) { i, m -> cliente.generar(i, m) })
    salida.parentFile?.mkdirs()
    salida.writeText(texto, Charsets.UTF_8)
    println(texto)
    println("Informe guardado en ${salida.absolutePath}")
}
