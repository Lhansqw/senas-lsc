package com.lhansqw.senaslsc.core.grabacion

import com.lhansqw.senaslsc.core.json.EscritorJson
import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.LectorJson
import com.lhansqw.senaslsc.core.json.comoObjeto
import com.lhansqw.senaslsc.core.json.entero
import com.lhansqw.senaslsc.core.json.lista
import com.lhansqw.senaslsc.core.json.numero
import com.lhansqw.senaslsc.core.json.texto
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D
import kotlin.math.roundToLong

/** Una seña (o un movimiento «sin seña») grabada con la app para entrenar: solo puntos, sin imágenes. */
data class MuestraGrabada(
    /** Alias de la persona (no su nombre): separa entrenamiento y prueba por persona. */
    val persona: String,
    /** Identificador del vocabulario (`lsc.hola`) o [FormatoGrabacion.SIN_SENA]. */
    val sena: String,
    val repeticion: Int,
    val fotogramas: List<LandmarkFrame>,
)

/**
 * Archivo JSON de una muestra grabada (fase 6, herramienta de grabación). Usa los mismos nombres
 * de campo que los casos compartidos con Python (`t`, `izquierda`, `derecha`, `cuerpo`, `rostro`,
 * `aspecto`), y `training/importar_grabaciones.py` lo convierte en una muestra de entrenamiento.
 */
object FormatoGrabacion {

    const val FORMATO = 1
    const val FUENTE = "GRABACION_APP"
    const val SIN_SENA = "sin_sena"

    /** Alias de persona: corto, sin espacios ni datos personales evidentes. */
    val ALIAS_VALIDO = Regex("^[a-z0-9_-]{1,24}$")
    private val SENA_VALIDA = Regex("^([a-z]{2,8}\\.[a-z0-9_]+|$SIN_SENA)$")

    fun nombreArchivo(m: MuestraGrabada): String = "${FUENTE}__${m.persona}__${m.sena}__r${m.repeticion}.json"

    fun escribir(m: MuestraGrabada): String {
        require(ALIAS_VALIDO.matches(m.persona)) { "Alias no válido: ${m.persona}" }
        require(SENA_VALIDA.matches(m.sena)) { "Seña no válida: ${m.sena}" }
        require(m.fotogramas.isNotEmpty()) { "La muestra no tiene fotogramas" }
        val fotogramas = m.fotogramas.map { f ->
            Json.Objeto(
                linkedMapOf(
                    "t" to Json.Numero(f.timestampMs.toDouble()),
                    "aspecto" to Json.Numero(redondear(f.relacionAspecto)),
                    "izquierda" to puntos(f.manoIzquierda),
                    "derecha" to puntos(f.manoDerecha),
                    "cuerpo" to puntos(f.cuerpo),
                    "rostro" to puntos(f.rostro),
                ),
            )
        }
        return EscritorJson.escribir(
            Json.Objeto(
                linkedMapOf(
                    "formato" to Json.Numero(FORMATO.toDouble()),
                    "fuente" to Json.Texto(FUENTE),
                    "persona" to Json.Texto(m.persona),
                    "sena" to Json.Texto(m.sena),
                    "repeticion" to Json.Numero(m.repeticion.toDouble()),
                    "fotogramas" to Json.Lista(fotogramas),
                ),
            ),
        )
    }

    fun leer(texto: String): MuestraGrabada {
        val r = LectorJson.leer(texto).comoObjeto("raíz")
        require(r.entero("formato") == FORMATO.toLong()) { "Formato de grabación no compatible" }
        val fotogramas = r.lista("fotogramas").mapIndexed { i, e ->
            val f = e.comoObjeto("fotogramas[$i]")
            LandmarkFrame(
                timestampMs = f.entero("t"),
                manoIzquierda = leerPuntos(f["izquierda"]),
                manoDerecha = leerPuntos(f["derecha"]),
                cuerpo = leerPuntos(f["cuerpo"]),
                rostro = leerPuntos(f["rostro"]),
                relacionAspecto = f.numero("aspecto").toFloat(),
            )
        }
        return MuestraGrabada(r.texto("persona"), r.texto("sena"), r.entero("repeticion").toInt(), fotogramas)
    }

    /** 5 decimales en coordenadas normalizadas: menos de una centésima de píxel, archivos más cortos. */
    private fun redondear(v: Float): Double = (v * 100_000.0).roundToLong() / 100_000.0

    private fun puntos(p: List<Punto3D>?): Json =
        p?.let { lista -> Json.Lista(lista.map { Json.Lista(listOf(Json.Numero(redondear(it.x)), Json.Numero(redondear(it.y)), Json.Numero(redondear(it.z)))) }) }
            ?: Json.Nulo

    private fun leerPuntos(v: Json?): List<Punto3D>? = when (v) {
        null, Json.Nulo -> null
        is Json.Lista -> v.elementos.map { p ->
            val c = (p as Json.Lista).elementos.map { (it as Json.Numero).valor.toFloat() }
            Punto3D(c[0], c[1], c[2])
        }
        else -> throw IllegalArgumentException("Puntos con formato no válido")
    }
}
