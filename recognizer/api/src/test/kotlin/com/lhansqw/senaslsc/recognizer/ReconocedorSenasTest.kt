package com.lhansqw.senaslsc.recognizer

import com.lhansqw.senaslsc.core.model.EntradaVocabulario
import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.PrediccionSena
import com.lhansqw.senaslsc.core.model.TipoSena
import com.lhansqw.senaslsc.core.model.Vocabulario
import org.junit.Assert.fail
import org.junit.Test

class ReconocedorSenasTest {

    private val reconocedor = object : ReconocedorSenas {
        override val vocabulario = Vocabulario("lsc", listOf(EntradaVocabulario("lsc.hola", "HOLA", "hola", TipoSena.PALABRA)))
        override val ficha = FichaModelo("prueba", "0.0.0", "lsc", emptyList())
        override val fotogramasPorVentana = 3
        override suspend fun predecir(ventana: List<LandmarkFrame>): PrediccionSena = error("no se usa")
        override fun close() = Unit
    }

    private fun frames(vararg tiempos: Long) = tiempos.map { LandmarkFrame(it, null, null, null, null) }

    private fun esperarError(bloque: () -> Unit) {
        try {
            bloque()
            fail("Se esperaba IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
        }
    }

    @Test
    fun `acepta una ventana del tamano esperado en orden`() {
        reconocedor.validarVentana(frames(0, 33, 66))
    }

    @Test
    fun `rechaza ventanas de otro tamano`() {
        esperarError { reconocedor.validarVentana(frames(0, 33)) }
    }

    @Test
    fun `rechaza fotogramas desordenados`() {
        esperarError { reconocedor.validarVentana(frames(0, 66, 33)) }
    }
}
