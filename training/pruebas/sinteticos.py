"""Muestras sintéticas para PROBAR el sistema de entrenamiento. No son señas de LSC.

Cada «seña» sintética es un movimiento distinto de la mano derecha (estática arriba, de izquierda
a derecha, en círculo…) con variaciones por persona. Sirven para comprobar que el código entrena,
evalúa y exporta correctamente; un modelo entrenado con ellas no se debe usar nunca en la app.
"""
from __future__ import annotations

import math

import numpy as np

from senas.fotograma import Fotograma
from senas.muestras import Muestra

CUERPO = np.array([[0.5, 0.3, 0], [0.4, 0.5, 0], [0.6, 0.5, 0], [0.35, 0.7, 0], [0.65, 0.7, 0], [0.3, 0.85, 0], [0.7, 0.85, 0]], dtype=np.float64)
FORMA_MANO = np.array([[0, 0, 0]] + [[(j % 4 - 1.5) * 0.012, -0.02 - (j // 4) * 0.012, 0] for j in range(20)], dtype=np.float64)


def _trayectoria(clase: int, fase: float) -> tuple[float, float]:
    if clase == 0:   # quieta arriba
        return 0.55, 0.35
    if clase == 1:   # de izquierda a derecha
        return 0.40 + 0.25 * fase, 0.45
    if clase == 2:   # círculo
        return 0.55 + 0.08 * math.cos(2 * math.pi * fase), 0.45 + 0.08 * math.sin(2 * math.pi * fase)
    if clase == 3:   # de arriba abajo
        return 0.55, 0.30 + 0.25 * fase
    return 0.62, 0.62  # quieta abajo


def muestra(clase: int, persona: int, repeticion: int, rnd: np.random.Generator, fotogramas: int = 30, duracion_ms: int = 1000) -> Muestra:
    escala_persona = 1.0 + 0.1 * (persona % 3 - 1)
    desplazamiento = np.array([0.02 * (persona % 5 - 2), 0.01 * (persona % 3 - 1), 0])
    fotos = []
    for i in range(fotogramas):
        fase = i / (fotogramas - 1)
        x, y = _trayectoria(clase, fase)
        mano = FORMA_MANO * escala_persona + np.array([x, y, 0]) + desplazamiento + rnd.normal(0, 0.003, (21, 3))
        cuerpo = CUERPO + desplazamiento + rnd.normal(0, 0.002, (7, 3))
        fotos.append(Fotograma(round(i * duracion_ms / (fotogramas - 1)), None, mano, cuerpo, None, 0.75))
    return Muestra(f"p{persona:02d}", f"lsc.sintetica{clase}", f"r{repeticion}", "SINTETICO", fotos)


def conjunto(clases: int = 4, personas: int = 8, repeticiones: int = 3, semilla: int = 7) -> list[Muestra]:
    rnd = np.random.default_rng(semilla)
    return [muestra(c, p, r, rnd) for c in range(clases) for p in range(personas) for r in range(repeticiones)]
