package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.Vocabulario
import com.lhansqw.senaslsc.recognizer.Caracteristicas
import com.lhansqw.senaslsc.recognizer.ContratoModelo
import com.lhansqw.senaslsc.recognizer.FichaModelo
import com.lhansqw.senaslsc.recognizer.PostProcesador
import com.lhansqw.senaslsc.recognizer.ReconocedorSenas
import com.lhansqw.senaslsc.recognizer.validarVentana

/**
 * Ejecuta el modelo `.tflite` de señas. Lo implementa cada plataforma: en iOS, Swift con
 * TensorFlow Lite; en Android, la app usa directamente `ReconocedorLiteRT`.
 *
 * No es `suspend` a propósito: así Swift lo implementa como una función normal.
 */
interface InferenciaModelo {
    /**
     * Recibe `fotogramas × Caracteristicas.POR_FOTOGRAMA` valores (lo que pide `modelo.json`) y
     * devuelve un logit por etiqueta, en el orden de `modelo.json`.
     */
    fun ejecutar(entrada: FloatArray): FloatArray
}

/**
 * [ReconocedorSenas] que calcula las características y el post-procesado en Kotlin común y solo
 * delega la inferencia en [inferencia]. Hace lo mismo que `ReconocedorLiteRT` en Android, para que
 * las dos plataformas den las mismas predicciones con el mismo modelo.
 */
class ReconocedorExterno(
    private val contrato: ContratoModelo,
    private val inferencia: InferenciaModelo,
) : ReconocedorSenas {

    override val vocabulario: Vocabulario = contrato.vocabulario()
    override val ficha: FichaModelo = contrato.ficha()
    override val fotogramasPorVentana: Int = contrato.fotogramas

    private val postProcesador = PostProcesador(contrato)
    private val tamanoEntrada = contrato.fotogramas * Caracteristicas.POR_FOTOGRAMA

    override suspend fun predecir(ventana: List<LandmarkFrame>): PrediccionSena {
        validarVentana(ventana)
        val logits = ejecutar(Caracteristicas.deVentana(ventana, contrato.usaRostro))
        return postProcesador.aplicar(logits, ventana.first().timestampMs, ventana.last().timestampMs)
    }

    /** Ejecuta el modelo con una entrada a cero y comprueba que la salida coincide con `modelo.json`. */
    fun comprobar() {
        ejecutar(FloatArray(tamanoEntrada))
    }

    private fun ejecutar(datos: FloatArray): FloatArray {
        check(datos.size == tamanoEntrada) { "Se generaron ${datos.size} valores; el modelo espera $tamanoEntrada" }
        val logits = inferencia.ejecutar(datos)
        check(logits.size == contrato.etiquetas.size) {
            "La salida del modelo tiene ${logits.size} valores y modelo.json declara ${contrato.etiquetas.size} etiquetas"
        }
        return logits
    }

    override fun close() = Unit
}
