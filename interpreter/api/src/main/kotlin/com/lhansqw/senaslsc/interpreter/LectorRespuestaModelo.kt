package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.json.CampoInvalido
import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.LectorJson
import com.lhansqw.senaslsc.core.json.booleano
import com.lhansqw.senaslsc.core.json.listaTextos
import com.lhansqw.senaslsc.core.json.texto
import com.lhansqw.senaslsc.core.json.textoOpcional

sealed interface LecturaRespuesta {
    data class Correcta(val respuesta: RespuestaModelo) : LecturaRespuesta
    data class Incorrecta(val motivo: String) : LecturaRespuesta
}

/**
 * Convierte el texto generado por un modelo de lenguaje en [RespuestaModelo].
 *
 * Los modelos a veces envuelven el JSON en texto o en un bloque de código aunque se les pida solo
 * JSON, así que se busca el primer objeto válido. Después [ValidadorRespuesta] comprueba el
 * contenido: este lector solo se ocupa del formato.
 */
object LectorRespuestaModelo {

    fun leer(texto: String): LecturaRespuesta {
        if (texto.isBlank()) return LecturaRespuesta.Incorrecta("El modelo no respondió nada.")
        val objeto = LectorJson.extraerPrimerObjeto(texto)
            ?: return LecturaRespuesta.Incorrecta("La respuesta no contiene un objeto JSON.")
        return try {
            LecturaRespuesta.Correcta(convertir(objeto))
        } catch (e: CampoInvalido) {
            LecturaRespuesta.Incorrecta("Respuesta con formato incorrecto: ${e.message}")
        }
    }

    private fun convertir(o: Json.Objeto) = RespuestaModelo(
        propuesta = o.texto("propuesta"),
        senasUsadas = o.listaTextos("senas_usadas"),
        incierta = o.booleano("incierta"),
        motivo = o.textoOpcional("motivo"),
        preguntaAclaracion = o.textoOpcional("pregunta_aclaracion"),
    )
}
