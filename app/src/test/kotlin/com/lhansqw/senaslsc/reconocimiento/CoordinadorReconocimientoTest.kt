package com.lhansqw.senaslsc.reconocimiento

import com.lhansqw.senaslsc.core.model.Candidata
import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.OrigenInterpretacion
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.PropuestaFrase
import com.lhansqw.senaslsc.core.model.Punto3D
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.core.model.Vocabulario
import com.lhansqw.senaslsc.interpreter.ResultadoGenerativo
import com.lhansqw.senaslsc.recognizer.FichaModelo
import com.lhansqw.senaslsc.recognizer.ReconocedorSenas
import com.lhansqw.senaslsc.sequence.MotorSenas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class CoordinadorReconocimientoTest {

    /** Ejecuta código suspendido que nunca se suspende de verdad (todo es síncrono en las pruebas). */
    private fun <T> ejecutar(bloque: suspend () -> T): T {
        var resultado: Result<T>? = null
        bloque.startCoroutine(Continuation(EmptyCoroutineContext) { resultado = it })
        return resultado!!.getOrThrow()
    }

    private val vocabulario = Vocabulario(
        "lsc",
        listOf(
            EntradaVocabulario("lsc.hola", "HOLA", "hola", TipoSena.PALABRA),
            EntradaVocabulario("lsc.gracias", "GRACIAS", "gracias", TipoSena.PALABRA),
        ),
    )

    /** Reconocedor falso: responde la seña que indique [actual] (null = desconocida). */
    private class ReconocedorFalso(override val vocabulario: Vocabulario) : ReconocedorSenas {
        var actual: String? = null
        var llamadas = 0
        override val ficha = FichaModelo("falso", "0", "lsc", emptyList())
        override val fotogramasPorVentana = 8
        override suspend fun predecir(ventana: List<LandmarkFrame>): PrediccionSena {
            llamadas++
            val id = actual
            return PrediccionSena(
                ventana.first().timestampMs,
                ventana.last().timestampMs,
                listOf(Candidata(id ?: "lsc.hola", if (id == null) 0.1f else 0.95f)),
                desconocida = id == null,
            )
        }
        override fun close() = Unit
    }

    private val mano = List(21) { Punto3D(0.5f, 0.5f, 0f) }
    private fun frame(ms: Long, manos: Boolean = true) = LandmarkFrame(ms, null, if (manos) mano else null, null, null)

    private val listo = EstadoModeloSenas.Listo("falso", "0", 2, emptyMap())

    private fun coordinador(r: ReconocedorFalso) =
        CoordinadorReconocimiento(r, MotorSenas(vocabulario, fotogramasModelo = 8, duracionVentanaMs = 1_000), listo)

    private fun simular(c: CoordinadorReconocimiento, desde: Long, ms: Long, manos: Boolean = true): Long {
        var t = desde
        while (t < desde + ms) {
            ejecutar { c.alRecibirFotograma(frame(t, manos)) }
            t += 33
        }
        return t
    }

    @Test
    fun `sin modelo no hace nada y conserva el estado del modelo`() {
        val c = CoordinadorReconocimiento(null, null, EstadoModeloSenas.SinModelo)
        ejecutar { c.alRecibirFotograma(frame(0)) }
        assertEquals(EstadoModeloSenas.SinModelo, c.estado.modelo)
        assertTrue(c.estado.senas.isEmpty())
    }

    @Test
    fun `confirma senas y propone una frase al hacer una pausa`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)

        r.actual = "lsc.hola"
        var t = simular(c, 0, 2_500)
        r.actual = "lsc.gracias"
        t = simular(c, t, 2_500)
        assertEquals(listOf("lsc.hola", "lsc.gracias"), c.estado.senas.map { it.id })
        assertNull(c.estado.propuesta)

        r.actual = null
        simular(c, t, 2_000, manos = false)
        assertTrue(c.estado.secuenciaCerrada)
        assertEquals("Hola gracias.", c.estado.propuesta?.texto)
        assertEquals(OrigenInterpretacion.REGLAS, c.estado.propuesta?.origen)
        assertTrue(r.llamadas > 0)
    }

    @Test
    fun `muestra la prediccion en curso y la borra sin manos`() {
        val r = ReconocedorFalso(vocabulario).apply { actual = "lsc.hola" }
        val c = coordinador(r)
        val t = simular(c, 0, 1_200)
        assertEquals("lsc.hola", c.estado.ultimaPrediccion?.mejor?.id)

        ejecutar { c.alRecibirFotograma(frame(t, manos = false)) }
        assertNull(c.estado.ultimaPrediccion)
    }

    @Test
    fun `interpretar cierra la secuencia a peticion`() {
        val r = ReconocedorFalso(vocabulario).apply { actual = "lsc.gracias" }
        val c = coordinador(r)
        simular(c, 0, 2_500)
        ejecutar { c.interpretar() }

        assertTrue(c.estado.secuenciaCerrada)
        assertEquals("Gracias", c.estado.propuesta?.texto)  // una sola seña: su significado
    }

    @Test
    fun `una sena nueva tras cerrar empieza otra frase y borra la propuesta`() {
        val r = ReconocedorFalso(vocabulario).apply { actual = "lsc.hola" }
        val c = coordinador(r)
        var t = simular(c, 0, 2_500)
        ejecutar { c.interpretar() }
        assertNotNull(c.estado.propuesta)

        r.actual = null
        t = simular(c, t, 700)          // libera la seña sostenida
        r.actual = "lsc.gracias"
        simular(c, t, 2_500)
        assertEquals(listOf("lsc.gracias"), c.estado.senas.map { it.id })
        assertNull(c.estado.propuesta)
    }

    @Test
    fun `eliminar y limpiar actualizan el estado`() {
        val r = ReconocedorFalso(vocabulario).apply { actual = "lsc.hola" }
        val c = coordinador(r)
        simular(c, 0, 2_500)
        c.eliminarUltima()
        assertTrue(c.estado.senas.isEmpty())

        simular(c, 5_000, 2_500)
        c.limpiar()
        assertTrue(c.estado.senas.isEmpty())
        assertNull(c.estado.ultimaPrediccion)
    }

    @Test
    fun `usa el interprete indicado para proponer`() {
        val r = ReconocedorFalso(vocabulario).apply { actual = "lsc.hola" }
        val c = CoordinadorReconocimiento(
            r,
            MotorSenas(vocabulario, 8, 1_000),
            listo,
            proponer = { senas ->
                PropuestaFrase("propia: ${senas.size}", senas.map { it.id }, false, null, null, OrigenInterpretacion.LOCAL)
            },
        )
        simular(c, 0, 2_500)
        ejecutar { c.interpretar() }
        assertEquals("propia: 1", c.estado.propuesta?.texto)
    }

    // --- Mejora asíncrona con el modelo de lenguaje -------------------------------------------

    private fun propuestaIa(texto: String, ids: List<String>) =
        PropuestaFrase(texto, ids, false, null, null, OrigenInterpretacion.LOCAL)

    /** Deja una secuencia HOLA + GRACIAS cerrada con el refinamiento activado. */
    private fun secuenciaCerrada(r: ReconocedorFalso, c: CoordinadorReconocimiento): Long {
        c.refinamientoDisponible = true
        r.actual = "lsc.hola"
        var t = simular(c, 0, 2_500)
        r.actual = "lsc.gracias"
        t = simular(c, t, 2_500)
        ejecutar { c.interpretar() }
        return t
    }

    @Test
    fun `con la ia disponible deja una solicitud y marca que esta generando`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)

        assertEquals("Hola gracias.", c.estado.propuesta?.texto)  // reglas, de inmediato
        assertTrue(c.estado.generando)
        val solicitud = c.tomarSolicitud()
        assertEquals(listOf("lsc.hola", "lsc.gracias"), solicitud?.senas?.map { it.id })
        assertNull(c.tomarSolicitud())                              // solo se entrega una vez
    }

    @Test
    fun `una respuesta aceptada sustituye la propuesta de reglas`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)
        val solicitud = c.tomarSolicitud()!!

        c.aplicarRefinamiento(solicitud.id, ResultadoGenerativo.Aceptada(propuestaIa("¡Hola, gracias!", listOf("lsc.hola", "lsc.gracias"))))
        assertEquals("¡Hola, gracias!", c.estado.propuesta?.texto)
        assertEquals(OrigenInterpretacion.LOCAL, c.estado.propuesta?.origen)
        assertEquals(false, c.estado.generando)
    }

    @Test
    fun `una respuesta descartada conserva las reglas y guarda el motivo`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)
        val solicitud = c.tomarSolicitud()!!
        val reglas = c.estado.propuesta!!

        c.aplicarRefinamiento(solicitud.id, ResultadoGenerativo.Descartada(listOf("Usa señas que no se reconocieron"), reglas))
        assertEquals("Hola gracias.", c.estado.propuesta?.texto)
        assertEquals("Usa señas que no se reconocieron", c.estado.motivoDescarteIa)
        assertEquals(false, c.estado.generando)
    }

    @Test
    fun `un resultado que llega tarde no pisa una secuencia distinta`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)
        val solicitud = c.tomarSolicitud()!!

        c.eliminarUltima()                                            // el usuario corrige mientras la IA piensa
        c.aplicarRefinamiento(solicitud.id, ResultadoGenerativo.Aceptada(propuestaIa("¡Hola, gracias!", listOf("lsc.hola", "lsc.gracias"))))
        assertNull(c.estado.propuesta)
        assertEquals(false, c.estado.generando)
    }

    @Test
    fun `una sola sena no pide ayuda a la ia`() {
        val r = ReconocedorFalso(vocabulario).apply { actual = "lsc.hola" }
        val c = coordinador(r).apply { refinamientoDisponible = true }
        simular(c, 0, 2_500)
        ejecutar { c.interpretar() }
        assertNull(c.tomarSolicitud())
        assertEquals(false, c.estado.generando)
    }

    @Test
    fun `sin la ia disponible no hay solicitudes`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)
        c.refinamientoDisponible = false
        c.limpiar()
        r.actual = "lsc.hola"
        val t = simular(c, 20_000, 2_500)
        r.actual = "lsc.gracias"
        simular(c, t, 2_500)
        ejecutar { c.interpretar() }
        assertNull(c.tomarSolicitud())
    }

    // --- Corrección del usuario ---------------------------------------------------------------

    @Test
    fun `corregir sustituye la propuesta y la marca como del usuario`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)

        assertTrue(c.corregir("  Hola, muchas gracias.  "))
        assertEquals("Hola, muchas gracias.", c.estado.propuesta?.texto)
        assertEquals(OrigenInterpretacion.USUARIO, c.estado.propuesta?.origen)
        assertEquals(listOf("lsc.hola", "lsc.gracias"), c.estado.propuesta?.senasUsadas)
    }

    @Test
    fun `una correccion es definitiva aunque luego llegue la ia`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        secuenciaCerrada(r, c)
        val solicitud = c.tomarSolicitud()!!

        c.corregir("Hola, gracias de verdad.")
        assertEquals(false, c.estado.generando)
        c.aplicarRefinamiento(solicitud.id, ResultadoGenerativo.Aceptada(propuestaIa("¡Hola, gracias!", listOf("lsc.hola", "lsc.gracias"))))
        assertEquals("Hola, gracias de verdad.", c.estado.propuesta?.texto)
    }

    @Test
    fun `no se corrige sin propuesta ni con texto vacio`() {
        val r = ReconocedorFalso(vocabulario)
        val c = coordinador(r)
        assertEquals(false, c.corregir("Algo"))
        secuenciaCerrada(r, c)
        assertEquals(false, c.corregir("   "))
        assertEquals("Hola gracias.", c.estado.propuesta?.texto)
    }
}
