package com.lhansqw.senaslsc.contrato

import com.lhansqw.senaslsc.core.json.EscritorJson
import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.aJson
import com.lhansqw.senaslsc.recognizer.Caracteristicas

/**
 * Completa `casos_caracteristicas.json` con las salidas de Kotlin (la referencia).
 *
 * Solo se ejecuta a mano cuando cambia el contrato, después de
 * `python training/pruebas/generar_entradas_casos.py`. En Android Studio: botón ▶ junto a `main`.
 */
object GeneradorCasosCompartidos {

    @JvmStatic
    fun main(args: Array<String>) {
        val raiz = CasosCompartidos.leerRaiz()
        val casos = CasosCompartidos.casos(raiz).map { (caso, json) ->
            val (tiempos, caracteristicas) = CasosCompartidos.calcular(caso)
            val porFotograma = Caracteristicas.POR_FOTOGRAMA
            val filas = (tiempos.indices).map { i ->
                (0 until porFotograma).map { k -> caracteristicas[i * porFotograma + k].toDouble().aJson() }.aJson()
            }
            val campos = LinkedHashMap(json.campos)
            campos["esperado_timestamps"] = tiempos.map { it.aJson() }.aJson()
            campos["esperado_caracteristicas"] = filas.aJson()
            Json.Objeto(campos)
        }
        val salida = LinkedHashMap(raiz.campos)
        salida["casos"] = casos.aJson()
        CasosCompartidos.archivo().writeText(EscritorJson.escribir(Json.Objeto(salida)), Charsets.UTF_8)
        println("Casos completados: ${casos.size} en ${CasosCompartidos.archivo()}")
    }
}
