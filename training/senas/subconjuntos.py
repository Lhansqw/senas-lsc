"""Puntos de MediaPipe que se conservan. Copia exacta de `Subconjuntos.kt` (versión 1)."""

VERSION = 1

PUNTOS_POR_MANO = 21
PUNTOS_POSE = 33
PUNTOS_ROSTRO_MINIMO = 468

# Índices del Pose Landmarker: nariz, hombros, codos y muñecas (izquierda antes que derecha).
CUERPO = [0, 11, 12, 13, 14, 15, 16]

# Índices de la malla facial: labios (contorno exterior), cejas, ojos y punta de la nariz.
ROSTRO = [
    61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 409, 270, 269, 267, 0, 37, 39, 40, 185,
    70, 63, 105, 66, 107, 336, 296, 334, 293, 300,
    33, 133, 159, 145, 362, 263, 386, 374,
    1,
]

PUNTOS_CUERPO = len(CUERPO)   # 7
PUNTOS_ROSTRO = len(ROSTRO)   # 39
