package com.lhansqw.senaslsc.flujo

import com.lhansqw.senaslsc.recognizer.Caracteristicas
import com.lhansqw.senaslsc.vision.Subconjuntos
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * `Caracteristicas` (:recognizer:api) y `Subconjuntos` (:vision:landmarks) viven en módulos
 * distintos que no se conocen entre sí. Esta prueba, en la app que usa los dos, falla si alguien
 * cambia uno sin el otro: el modelo recibiría datos desalineados sin dar ningún error.
 */
class CoherenciaContratoTest {

    @Test
    fun `los tamanos de los subconjuntos coinciden con las caracteristicas`() {
        assertEquals(Subconjuntos.CUERPO.size, Caracteristicas.PUNTOS_CUERPO)
        assertEquals(Subconjuntos.ROSTRO.size, Caracteristicas.PUNTOS_ROSTRO)
    }

    @Test
    fun `las versiones coinciden`() {
        assertEquals(Subconjuntos.VERSION, Caracteristicas.VERSION_SUBCONJUNTOS)
    }

    @Test
    fun `los hombros estan donde los espera la referencia`() {
        // Caracteristicas usa las posiciones 1 y 2 del cuerpo como hombro izquierdo y derecho.
        assertEquals(11, Subconjuntos.CUERPO[1])
        assertEquals(12, Subconjuntos.CUERPO[2])
    }
}
