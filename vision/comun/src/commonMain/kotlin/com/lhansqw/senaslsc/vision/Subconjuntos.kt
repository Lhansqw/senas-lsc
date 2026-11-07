package com.lhansqw.senaslsc.vision

import com.lhansqw.senaslsc.core.model.LandmarkFrame
import com.lhansqw.senaslsc.core.model.Punto3D

/**
 * Qué puntos de MediaPipe se conservan en cada [LandmarkFrame].
 *
 * Las manos se guardan completas (21 puntos). Del cuerpo (33 puntos) y del rostro (478) solo se
 * conserva lo útil para reconocer señas: la posición de los brazos respecto al cuerpo y las
 * expresiones no manuales (cejas, ojos, boca). Así el modelo es más pequeño y aprende menos ruido.
 *
 * IMPORTANTE: el entrenamiento en Python (fase 6) debe usar exactamente estos índices y este orden.
 * Si se cambian, hay que volver a extraer los puntos del conjunto de datos y reentrenar.
 */
object Subconjuntos {

    /** Versión de la selección; se guarda con cada conjunto de datos y en la ficha del modelo. */
    const val VERSION = 1

    /** Índices del Pose Landmarker: nariz, hombros, codos y muñecas (izquierda antes que derecha). */
    val CUERPO: List<Int> = listOf(
        0,       // nariz
        11, 12,  // hombro izquierdo, hombro derecho
        13, 14,  // codo izquierdo, codo derecho
        15, 16,  // muñeca izquierda, muñeca derecha
    )

    /** Índices de la malla facial (Face Landmarker). */
    val ROSTRO: List<Int> = listOf(
        // Labios, contorno exterior
        61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185,
        // Ceja izquierda y ceja derecha
        70, 63, 105, 66, 107, 336, 296, 334, 293, 300,
        // Ojo izquierdo y ojo derecho: comisuras y párpados
        33, 133, 159, 145, 362, 263, 386, 374,
        // Punta de la nariz (referencia)
        1,
    )

    /** Puntos mínimos que debe tener cada resultado para poder seleccionar los índices anteriores. */
    const val PUNTOS_POSE = 33
    const val PUNTOS_ROSTRO_MINIMO = 468

    /**
     * Conexiones de la mano (índices 0–20) para dibujarla:
     * muñeca → pulgar, índice, medio, anular, meñique y la base de la palma.
     */
    val CONEXIONES_MANO: List<Pair<Int, Int>> = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        5 to 9, 9 to 10, 10 to 11, 11 to 12,
        9 to 13, 13 to 14, 14 to 15, 15 to 16,
        13 to 17, 0 to 17, 17 to 18, 18 to 19, 19 to 20,
    )

    /** Conexiones del cuerpo, con posiciones dentro de [CUERPO] (no índices de MediaPipe). */
    val CONEXIONES_CUERPO: List<Pair<Int, Int>> = listOf(
        1 to 2,          // hombros
        1 to 3, 3 to 5,  // brazo izquierdo
        2 to 4, 4 to 6,  // brazo derecho
    )

    /**
     * Construye un [LandmarkFrame] a partir de los puntos completos de MediaPipe.
     * Las listas vacías (no detectado) o incompletas se convierten en `null`.
     */
    fun construirFrame(
        timestampMs: Long,
        manoIzquierda: List<Punto3D>,
        manoDerecha: List<Punto3D>,
        pose: List<Punto3D>,
        rostro: List<Punto3D>,
        relacionAspecto: Float = 1f,
    ): LandmarkFrame = LandmarkFrame(
        timestampMs = timestampMs,
        manoIzquierda = manoIzquierda.takeIf { it.size == LandmarkFrame.PUNTOS_POR_MANO },
        manoDerecha = manoDerecha.takeIf { it.size == LandmarkFrame.PUNTOS_POR_MANO },
        cuerpo = pose.takeIf { it.size >= PUNTOS_POSE }?.let { puntos -> CUERPO.map { puntos[it] } },
        rostro = rostro.takeIf { it.size >= PUNTOS_ROSTRO_MINIMO }?.let { puntos -> ROSTRO.map { puntos[it] } },
        relacionAspecto = relacionAspecto,
    )
}
