"""Convierte muestras en tensores para el modelo, con la misma ventana que la app.

Una muestra se convierte en una ventana que termina en su último fotograma y cubre `duracion_ms`
(como cuando la persona acaba de hacer la seña delante de la cámara), se remuestrea a `fotogramas`
instantes y se calculan las 352 características de cada uno.
"""
from __future__ import annotations

import numpy as np

from . import caracteristicas as C
from .aumento import aumentar
from .muestras import Muestra
from .remuestreo import a_tiempo_uniforme


def ventana(muestra_fotogramas, fotogramas: int, duracion_ms: int) -> np.ndarray:
    return C.de_ventana(a_tiempo_uniforme(muestra_fotogramas, fotogramas, duracion_ms))


def construir(muestras: list[Muestra], etiquetas: list[str], fotogramas: int, duracion_ms: int,
              copias_aumentadas: int = 0, semilla: int = 0) -> tuple[np.ndarray, np.ndarray]:
    """Devuelve `X` de forma `(N, fotogramas, 352)` en float32 y `y` con el índice de cada etiqueta.

    Con `copias_aumentadas > 0` añade esa cantidad de variaciones aleatorias por muestra (solo
    para entrenamiento; validación y prueba se evalúan sin aumento).
    """
    indice = {e: i for i, e in enumerate(etiquetas)}
    rnd = np.random.default_rng(semilla)
    xs, ys = [], []
    for m in muestras:
        if m.sena not in indice:
            raise ValueError(f"La seña {m.sena} no está entre las etiquetas del modelo")
        xs.append(ventana(m.fotogramas, fotogramas, duracion_ms))
        ys.append(indice[m.sena])
        for _ in range(copias_aumentadas):
            xs.append(ventana(aumentar(m.fotogramas, rnd), fotogramas, duracion_ms))
            ys.append(indice[m.sena])
    return np.stack(xs).astype(np.float32), np.array(ys, dtype=np.int64)
