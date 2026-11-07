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

    num_muestras_base = len(muestras)
    num_muestras_totales = num_muestras_base * (1 + copias_aumentadas)

    if num_muestras_totales == 0:
        return np.array([]), np.array([])

    # Inferir dimensiones de X y Y procesando una única muestra
    # Esto asegura que las dimensiones sean correctas para la pre-asignación
    primera_muestra = muestras[0]
    if primera_muestra.sena not in indice:
        raise ValueError(f"La seña {primera_muestra.sena} no está entre las etiquetas del modelo")

    base_primera = preparar(primera_muestra.fotogramas)
    x_primera = ventana(base_primera, fotogramas, duracion_ms, usar_rostro)

    # Pre-asignar los arrays de NumPy
    xs_np = np.zeros((num_muestras_totales, *x_primera.shape), dtype=np.float32)
    ys_np = np.zeros(num_muestras_totales, dtype=np.int64) # Las etiquetas suelen ser int64

    idx_actual = 0
    for m in muestras:
        if m.sena not in indice:
            raise ValueError(f"La seña {m.sena} no está entre las etiquetas del modelo")

        base = preparar(m.fotogramas)

        # Muestra original
        xs_np[idx_actual] = ventana(base, fotogramas, duracion_ms, usar_rostro)
        ys_np[idx_actual] = indice[m.sena]
        idx_actual += 1

        # Copias aumentadas
        for _ in range(copias_aumentadas):
            xs_np[idx_actual] = ventana(aumentar(base, rnd), fotogramas, duracion_ms, usar_rostro)
            ys_np[idx_actual] = indice[m.sena]
            idx_actual += 1

    return xs_np, ys_np
