"""Convierte muestras en tensores para el modelo, con la misma ventana que la app.

Una muestra se convierte en una ventana que termina en su último fotograma y cubre `duracion_ms`
(como cuando la persona acaba de hacer la seña delante de la cámara), se remuestrea a `fotogramas`
instantes y se calculan las 352 características de cada uno.

Las muestras con pocos fotogramas por segundo (las 6 fotos de LSC70) se densifican antes a
`FPS_APP`, para que las velocidades se parezcan a las que mide la app (ver `interpolacion.py`).
"""
from __future__ import annotations

import numpy as np

from . import caracteristicas as C
from .aumento import aumentar
from .interpolacion import densificar, necesita_densificar
from .muestras import Muestra
from .remuestreo import a_tiempo_uniforme


FPS_APP = 30.0
FPS_MINIMO = 20.0


def preparar(muestra_fotogramas):
    """Densifica la muestra si tiene menos de `FPS_MINIMO` fotogramas por segundo."""
    if necesita_densificar(muestra_fotogramas, FPS_MINIMO):
        return densificar(muestra_fotogramas, FPS_APP)
    return muestra_fotogramas


def ventana(muestra_fotogramas, fotogramas: int, duracion_ms: int, usar_rostro: bool = True) -> np.ndarray:
    return C.de_ventana(a_tiempo_uniforme(muestra_fotogramas, fotogramas, duracion_ms), usar_rostro)


def proporcion_con_rostro(muestras: list[Muestra]) -> float:
    """Fracción de fotogramas con la cara detectada (en LSC70 es 0: las caras están difuminadas)."""
    total = sum(len(m.fotogramas) for m in muestras)
    return sum(f.rostro is not None for m in muestras for f in m.fotogramas) / total if total else 0.0


def construir(muestras: list[Muestra], etiquetas: list[str], fotogramas: int, duracion_ms: int,
              copias_aumentadas: int = 0, semilla: int = 0, usar_rostro: bool = True) -> tuple[np.ndarray, np.ndarray]:
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
        base = preparar(m.fotogramas)
        xs.append(ventana(base, fotogramas, duracion_ms, usar_rostro))
        ys.append(indice[m.sena])
        for _ in range(copias_aumentadas):
            xs.append(ventana(aumentar(base, rnd), fotogramas, duracion_ms, usar_rostro))
            ys.append(indice[m.sena])
    return np.stack(xs).astype(np.float32), np.array(ys, dtype=np.int64)
