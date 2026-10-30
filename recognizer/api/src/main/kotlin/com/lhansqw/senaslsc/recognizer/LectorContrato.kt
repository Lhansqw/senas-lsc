package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.json.CampoInvalido
import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.JsonInvalido
import com.lhansqw.senaslsc.core.json.LectorJson
import com.lhansqw.senaslsc.core.json.booleanoOpcional
import com.lhansqw.senaslsc.core.json.comoObjeto
import com.lhansqw.senaslsc.core.json.entero
import com.lhansqw.senaslsc.core.json.lista
import com.lhansqw.senaslsc.core.json.listaTextos
import com.lhansqw.senaslsc.core.json.numero
import com.lhansqw.senaslsc.core.json.numeroOpcional
import com.lhansqw.senaslsc.core.json.objeto
import com.lhansqw.senaslsc.core.json.objetoOpcional
import com.lhansqw.senaslsc.core.json.texto
import com.lhansqw.senaslsc.core.json.textoOpcional
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.TipoSena

sealed interface ResultadoContrato {
    data class Valido(val contrato: ContratoModelo) : ResultadoContrato
    data class Invalido(val errores: List<String>) : ResultadoContrato
}

/**
 * Lee `modelo.json` (formato 1; ejemplo en `docs/modelo.ejemplo.json`) y comprueba que el modelo
 * es compatible con la app.
 */
object LectorContrato {

    const val FORMATO = 1

    fun leer(texto: String): ResultadoContrato {
        val contrato = try {
            val raiz = LectorJson.leer(texto) as? Json.Objeto
                ?: return ResultadoContrato.Invalido(listOf("modelo.json debe contener un objeto"))
            convertir(raiz)
        } catch (e: JsonInvalido) {
            return ResultadoContrato.Invalido(listOf("modelo.json no es JSON válido: ${e.message}"))
        } catch (e: CampoInvalido) {
            return ResultadoContrato.Invalido(listOf("modelo.json, ${e.message}"))
        } catch (e: IllegalArgumentException) {
            return ResultadoContrato.Invalido(listOf("modelo.json: ${e.message}"))
        }
        val errores = contrato.validar()
        return if (errores.isEmpty()) ResultadoContrato.Valido(contrato) else ResultadoContrato.Invalido(errores)
    }

    private fun convertir(r: Json.Objeto): ContratoModelo {
        val formato = r.entero("formato")
        if (formato != FORMATO.toLong()) throw CampoInvalido("formato", "la app lee el formato $FORMATO, no el $formato")

        val car = r.objeto("caracteristicas")
        val ventana = r.objeto("ventana")
        val salida = r.objeto("salida")

        return ContratoModelo(
            nombre = r.texto("nombre"),
            version = r.texto("version"),
            lengua = r.texto("lengua"),
            versionCaracteristicas = car.entero("version", "caracteristicas.version").toInt(),
            versionSubconjuntos = car.entero("version_subconjuntos", "caracteristicas.version_subconjuntos").toInt(),
            caracteristicasPorFotograma = car.entero("por_fotograma", "caracteristicas.por_fotograma").toInt(),
            usaRostro = car.booleanoOpcional("usa_rostro", true, "caracteristicas.usa_rostro"),
            fotogramas = ventana.entero("fotogramas", "ventana.fotogramas").toInt(),
            duracionVentanaMs = ventana.entero("duracion_ms", "ventana.duracion_ms"),
            etiquetas = salida.listaTextos("etiquetas", "salida.etiquetas"),
            etiquetaSinSena = salida.textoOpcional("etiqueta_sin_sena", "salida.etiqueta_sin_sena"),
            temperatura = salida.numero("temperatura", "salida.temperatura").toFloat(),
            umbralEnergia = salida.numeroOpcional("umbral_energia", "salida.umbral_energia")?.toFloat(),
            entradas = r.lista("vocabulario").mapIndexed { i, e -> entrada(e.comoObjeto("vocabulario[$i]"), "vocabulario[$i]") },
            datosEntrenamiento = r.listaTextos("datos_entrenamiento"),
            metricas = r.objetoOpcional("metricas")?.campos?.mapValues { (clave, valor) ->
                ((valor as? Json.Numero) ?: throw CampoInvalido("metricas.$clave", "debe ser un número")).valor.toFloat()
            } ?: emptyMap(),
        )
    }

    private fun entrada(o: Json.Objeto, ruta: String): EntradaVocabulario {
        val tipo = when (val t = o.texto("tipo", "$ruta.tipo")) {
            "letra" -> TipoSena.LETRA
            "numero" -> TipoSena.NUMERO
            "palabra" -> TipoSena.PALABRA
            else -> throw CampoInvalido("$ruta.tipo", "debe ser letra, numero o palabra (no '$t')")
        }
        return EntradaVocabulario(
            id = o.texto("id", "$ruta.id"),
            glosa = o.texto("glosa", "$ruta.glosa"),
            significado = o.texto("significado", "$ruta.significado"),
            tipo = tipo,
            interrogativa = o.booleanoOpcional("interrogativa", false, "$ruta.interrogativa"),
            variante = o.textoOpcional("variante", "$ruta.variante"),
        )
    }
}
