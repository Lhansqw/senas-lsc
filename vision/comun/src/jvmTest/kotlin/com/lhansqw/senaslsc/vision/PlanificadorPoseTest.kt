package com.lhansqw.senaslsc.vision

import org.junit.Assert.assertEquals
import org.junit.Test

class PlanificadorPoseTest {

    /** Ejecuta [n] fotogramas con las mismas manos y devuelve en cuáles tocó la pose. */
    private fun PlanificadorPose.serie(n: Int, manos: Int = 1, siempre: Boolean = false): List<Boolean> {
        var hayPose = false
        return List(n) {
            debeEjecutar(manos, hayPose, siempre).also { if (it) hayPose = true }
        }
    }

    @Test
    fun `con cada 3 ejecuta uno de cada tres fotogramas`() {
        assertEquals(
            listOf(true, false, false, true, false, false, true),
            PlanificadorPose(3).serie(7),
        )
    }

    @Test
    fun `con cada 1 ejecuta todos`() {
        assertEquals(List(5) { true }, PlanificadorPose(1).serie(5))
    }

    @Test
    fun `sin pose previa la ejecuta aunque no toque`() {
        val p = PlanificadorPose(3)
        p.debeEjecutar(manos = 1, hayPose = false)
        assertEquals(true, p.debeEjecutar(manos = 1, hayPose = false))
    }

    @Test
    fun `si cambia el numero de manos la ejecuta y reinicia la cuenta`() {
        val p = PlanificadorPose(3)
        assertEquals(true, p.debeEjecutar(manos = 1, hayPose = false))
        assertEquals(false, p.debeEjecutar(manos = 1, hayPose = true))
        assertEquals(true, p.debeEjecutar(manos = 2, hayPose = true))
        assertEquals(false, p.debeEjecutar(manos = 2, hayPose = true))
        assertEquals(false, p.debeEjecutar(manos = 2, hayPose = true))
        assertEquals(true, p.debeEjecutar(manos = 2, hayPose = true))
    }

    @Test
    fun `al grabar la ejecuta en todos los fotogramas`() {
        assertEquals(List(4) { true }, PlanificadorPose(3).serie(4, siempre = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `cada debe ser al menos 1`() {
        PlanificadorPose(0)
    }
}
