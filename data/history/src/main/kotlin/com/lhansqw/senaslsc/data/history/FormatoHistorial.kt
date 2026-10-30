package com.lhansqw.senaslsc.data.history

import com.lhansqw.senaslsc.core.json.EscritorJson
import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.LectorJson
import com.lhansqw.senaslsc.core.json.aJson
import com.lhansqw.senaslsc.core.json.comoObjeto
import com.lhansqw.senaslsc.core.json.entero
import com.lhansqw.senaslsc.core.json.json
import com.lhansqw.senaslsc.core.json.lista
import com.lhansqw.senaslsc.core.json.listaTextos
import com.lhansqw.senaslsc.core.json.texto

/** El historial no se pudo leer (datos dañados, otro formato o clave de cifrado perdida). */
class HistorialIlegible(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/**
 * Formato del historial en disco, **antes** de cifrarlo:
 * `{"formato": 1, "entradas": [{"id", "creada_ms", "texto", "glosas", "origen"}, …]}`.
 */
object FormatoHistorial {

    const val FORMATO = 1

    fun escribir(entradas: List<EntradaHistorial>): ByteArray {
        val raiz = json(
            "formato" to FORMATO.aJson(),
            "entradas" to entradas.map { e ->
                json(
                    "id" to e.id.aJson(),
                    "creada_ms" to e.creadaMs.aJson(),
                    "texto" to e.texto.aJson(),
                    "glosas" to e.glosas.map { it.aJson() }.aJson(),
                    "origen" to e.origen.clave.aJson(),
                )
            }.aJson(),
        )
        return EscritorJson.escribir(raiz).toByteArray(Charsets.UTF_8)
    }

    fun leer(datos: ByteArray): List<EntradaHistorial> {
        try {
            val raiz = LectorJson.leer(datos.toString(Charsets.UTF_8)) as? Json.Objeto
                ?: throw HistorialIlegible("El historial no contiene un objeto")
            val formato = raiz.entero("formato")
            if (formato != FORMATO.toLong()) throw HistorialIlegible("Formato de historial desconocido: $formato")
            return raiz.lista("entradas").mapIndexed { i, e ->
                val o = e.comoObjeto("entradas[$i]")
                EntradaHistorial(
                    id = o.texto("id"),
                    creadaMs = o.entero("creada_ms"),
                    texto = o.texto("texto"),
                    glosas = o.listaTextos("glosas"),
                    origen = OrigenTexto.deClave(o.texto("origen"))
                        ?: throw HistorialIlegible("Origen desconocido en entradas[$i]"),
                )
            }
        } catch (e: HistorialIlegible) {
            throw e
        } catch (e: IllegalArgumentException) {
            // Incluye JSON inválido (JsonInvalido), campos de tipo incorrecto (CampoInvalido) y
            // entradas que no pasan su validación.
            throw HistorialIlegible("Historial dañado: ${e.message}", e)
        }
    }
}
