package com.lhansqw.senaslsc.recognizer.litert

import android.content.Context
import com.google.ai.edge.litert.Accelerator
import com.google.ai.edge.litert.CompiledModel
import com.google.ai.edge.litert.Environment
import com.google.ai.edge.litert.TensorBuffer
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
 * Reconocedor de señas que ejecuta el modelo entrenado con LiteRT (API CompiledModel).
 *
 * Los búferes de entrada y salida se crean una vez y se reutilizan. Según la documentación de
 * LiteRT, el modelo se crea, se ejecuta y se cierra siempre desde el **mismo hilo de fondo**: quien
 * lo use debe garantizarlo (la app usa un hilo dedicado).
 */
class ReconocedorLiteRT private constructor(
    private val modelo: CompiledModel,
    private val entradas: List<TensorBuffer>,
    private val salidas: List<TensorBuffer>,
    private val contrato: ContratoModelo,
) : ReconocedorSenas {

    override val vocabulario: Vocabulario = contrato.vocabulario()
    override val ficha: FichaModelo = contrato.ficha()
    override val fotogramasPorVentana: Int = contrato.fotogramas

    private val postProcesador = PostProcesador(contrato)
    private val tamanoEntrada = contrato.fotogramas * Caracteristicas.POR_FOTOGRAMA
    private var cerrado = false

    override suspend fun predecir(ventana: List<LandmarkFrame>): PrediccionSena {
        check(!cerrado) { "El reconocedor está cerrado" }
        validarVentana(ventana)
        val logits = ejecutar(Caracteristicas.deVentana(ventana, contrato.usaRostro))
        return postProcesador.aplicar(logits, ventana.first().timestampMs, ventana.last().timestampMs)
    }

    private fun ejecutar(datos: FloatArray): FloatArray {
        check(datos.size == tamanoEntrada) { "Se generaron ${datos.size} valores; el modelo espera $tamanoEntrada" }
        entradas[0].writeFloat(datos)
        modelo.run(entradas, salidas)
        return salidas[0].readFloat()
    }

    override fun close() {
        if (cerrado) return
        cerrado = true
        // La documentación pide cerrar los búferes antes que el modelo.
        (entradas + salidas).forEach { it.close() }
        modelo.close()
    }

    companion object {
        /** Archivos que deben estar en `recognizer/litert/src/main/assets/`. */
        const val ARCHIVO_MODELO = "reconocedor_lsc.tflite"
        const val ARCHIVO_CONTRATO = "modelo.json"

        private val entorno by lazy { Environment.create() }

        /**
         * Carga el modelo desde los assets y comprueba con una ejecución en vacío que su salida
         * tiene una posición por etiqueta. Lanza una excepción si no se puede cargar o si la salida
         * no coincide con el contrato.
         */
        fun crear(context: Context, contrato: ContratoModelo, acelerador: Accelerator = Accelerator.CPU): ReconocedorLiteRT {
            val modelo = CompiledModel.create(
                context.applicationContext.assets,
                ARCHIVO_MODELO,
                CompiledModel.Options(acelerador),
                entorno,
            )
            val entradas = mutableListOf<TensorBuffer>()
            val salidas = mutableListOf<TensorBuffer>()
            try {
                entradas += modelo.createInputBuffers()
                salidas += modelo.createOutputBuffers()
                check(entradas.size == 1 && salidas.size == 1) {
                    "El modelo debe tener una entrada y una salida (tiene ${entradas.size} y ${salidas.size})"
                }
                val reconocedor = ReconocedorLiteRT(modelo, entradas, salidas, contrato)
                val prueba = reconocedor.ejecutar(FloatArray(reconocedor.tamanoEntrada))
                check(prueba.size == contrato.etiquetas.size) {
                    "La salida del modelo tiene ${prueba.size} valores y modelo.json declara ${contrato.etiquetas.size} etiquetas"
                }
                return reconocedor
            } catch (e: Exception) {
                (entradas + salidas).forEach { runCatching { it.close() } }
                modelo.close()
                throw e
            }
        }
    }
}
