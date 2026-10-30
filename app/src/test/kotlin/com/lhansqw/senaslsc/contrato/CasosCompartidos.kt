package com.lhansqw.senaslsc.contrato

import com.lhansqw.senaslsc.core.json.Json
import com.lhansqw.senaslsc.core.json.LectorJson
import com.lhansqw.senaslsc.core.json.comoObjeto
import com.lhansqw.senaslsc.core.json.entero
import com.lhansqw.senaslsc.core.json.lista
import com.lhansqw.senaslsc.core.json.numero
import com.lhansqw.senaslsc.core.json.texto
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D
import com.lhansqw.senaslsc.recognizer.Caracteristicas
import com.lhansqw.senaslsc.sequence.Remuestreo
import java.io.File

/**
 * Casos compartidos con el entrenamiento en Python (`training/pruebas/casos_caracteristicas.json`).
 * Garantizan que Kotlin y Python calculan exactamente las mismas características.
 */
internal object CasosCompartidos {

    const val RUTA = "training/pruebas/casos_caracteristicas.json"

    data class Caso(val nombre: String, val duracionMs: Long, val instantes: Int, val fotogramas: List<LandmarkFrame>)

    /** Busca el archivo subiendo desde la carpeta de trabajo (Gradle ejecuta las pruebas desde app/). */
    fun archivo(): File {
        var carpeta: File? = File(System.getProperty("user.dir")).absoluteFile
        while (carpeta != null) {
            val candidato = File(carpeta, RUTA)
            if (candidato.isFile) return candidato
            carpeta = carpeta.parentFile
        }
        error("No se encontró $RUTA")
    }

    fun leerRaiz(): Json.Objeto = LectorJson.leer(archivo().readText(Charsets.UTF_8)) as Json.Objeto

    fun casos(raiz: Json.Objeto): List<Pair<Caso, Json.Objeto>> = raiz.lista("casos").mapIndexed { i, e ->
        val o = e.comoObjeto("casos[$i]")
        val fotogramas = o.lista("fotogramas").mapIndexed { j, f ->
            val fo = f.comoObjeto("fotogramas[$j]")
            LandmarkFrame(
                timestampMs = fo.entero("t"),
                manoIzquierda = puntos(fo["izquierda"]),
                manoDerecha = puntos(fo["derecha"]),
                cuerpo = puntos(fo["cuerpo"]),
                rostro = puntos(fo["rostro"]),
                relacionAspecto = fo.numero("aspecto").toFloat(),
            )
        }
        Caso(o.texto("nombre"), o.entero("duracion_ms"), o.entero("instantes").toInt(), fotogramas) to o
    }

    /** Lo que calcula la app para un caso: ventana remuestreada y sus características. */
    fun calcular(caso: Caso): Pair<List<Long>, FloatArray> {
        val ventana = Remuestreo.aTiempoUniforme(caso.fotogramas, caso.instantes, caso.duracionMs)
        return ventana.map { it.timestampMs } to Caracteristicas.deVentana(ventana)
    }

    private fun puntos(valor: Json?): List<Punto3D>? = when (valor) {
        null, Json.Nulo -> null
        is Json.Lista -> valor.elementos.map { p ->
            val c = (p as Json.Lista).elementos.map { (it as Json.Numero).valor.toFloat() }
            Punto3D(c[0], c[1], c[2])
        }
        else -> error("Puntos con formato no válido")
    }
}
