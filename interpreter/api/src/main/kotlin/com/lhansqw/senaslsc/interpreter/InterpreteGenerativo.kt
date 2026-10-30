package com.lhansqw.senaslsc.interpreter

import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.SenaConfirmada
import kotlin.coroutines.cancellation.CancellationException

/**
 * Genera texto con un modelo de lenguaje a partir de unas instrucciones y un mensaje.
 * Lo implementan LiteRT-LM (en el teléfono) y, en la fase 10, un proveedor remoto.
 */
fun interface GeneradorTexto {
    suspend fun generar(instrucciones: String, mensaje: String): String
}

/** Qué pasó al pedir una frase al modelo de lenguaje. */
sealed interface ResultadoGenerativo {
    val propuesta: PropuestaFrase

    /** La respuesta del modelo cumplió el contrato y se muestra. */
    data class Aceptada(override val propuesta: PropuestaFrase) : ResultadoGenerativo

    /** El modelo falló o no cumplió el contrato: se muestra la propuesta por reglas. */
    data class Descartada(val motivos: List<String>, override val propuesta: PropuestaFrase) : ResultadoGenerativo

    /** No se consultó al modelo (una sola seña): se muestra su significado. */
    data class NoAplica(override val propuesta: PropuestaFrase) : ResultadoGenerativo
}

/**
 * Intérprete con modelo de lenguaje y red de seguridad.
 *
 * 1. Con menos de [ReglasInterpretacion.MINIMO_SENAS_MODELO] señas no se consulta al modelo.
 * 2. Se envían las instrucciones de [PlantillaPrompt] y la secuencia.
 * 3. La respuesta se lee ([LectorRespuestaModelo]) y se valida ([ValidadorRespuesta]).
 * 4. Si cualquier paso falla, se usa [respaldo] (normalmente el intérprete por reglas) y se
 *    informa del motivo. El usuario nunca ve una propuesta que no haya pasado el validador.
 */
class InterpreteGenerativo(
    private val generador: GeneradorTexto,
    override val origen: OrigenInterpretacion,
    private val respaldo: Interprete,
    private val validador: ValidadorRespuesta = ValidadorRespuesta(),
) : Interprete {

    init {
        require(origen == OrigenInterpretacion.LOCAL || origen == OrigenInterpretacion.REMOTO) {
            "Un intérprete generativo es local o remoto"
        }
    }

    override suspend fun proponer(secuencia: List<SenaConfirmada>, contexto: String?): PropuestaFrase =
        interpretar(secuencia, contexto).propuesta

    suspend fun interpretar(secuencia: List<SenaConfirmada>, contexto: String? = null): ResultadoGenerativo {
        require(secuencia.isNotEmpty()) { "La secuencia está vacía" }
        if (secuencia.size < ReglasInterpretacion.MINIMO_SENAS_MODELO) {
            return ResultadoGenerativo.NoAplica(respaldo.proponer(secuencia, contexto))
        }

        val texto = try {
            generador.generar(PlantillaPrompt.INSTRUCCIONES, PlantillaPrompt.construirEntrada(secuencia, contexto))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return descartar(listOf("El modelo de lenguaje falló: ${e.message ?: e.javaClass.simpleName}"), secuencia, contexto)
        }

        val respuesta = when (val lectura = LectorRespuestaModelo.leer(texto)) {
            is LecturaRespuesta.Incorrecta -> return descartar(listOf(lectura.motivo), secuencia, contexto)
            is LecturaRespuesta.Correcta -> lectura.respuesta
        }

        return when (val validacion = validador.validar(secuencia, respuesta, origen)) {
            is ResultadoValidacion.Valida -> ResultadoGenerativo.Aceptada(validacion.propuesta)
            is ResultadoValidacion.Invalida -> descartar(validacion.errores, secuencia, contexto)
        }
    }

    private suspend fun descartar(motivos: List<String>, secuencia: List<SenaConfirmada>, contexto: String?) =
        ResultadoGenerativo.Descartada(motivos, respaldo.proponer(secuencia, contexto))
}
