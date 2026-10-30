"""Aumento de datos sobre los puntos (no sobre imágenes).

Con pocas personas, el modelo necesita ver variaciones realistas de la misma seña:
- pequeña rotación y escala alrededor del centro de los hombros (postura, distancia);
- desplazamiento (posición en la imagen);
- ruido en las coordenadas (imprecisión de MediaPipe);
- cambio de velocidad (personas que señan más rápido o más despacio).

No se hace espejo izquierda/derecha por defecto: en LSC la mano dominante importa y muchas señas
no son simétricas.
"""
from __future__ import annotations

import math
from dataclasses import replace

import numpy as np

from .fotograma import Fotograma


def _transformar(puntos, cx: float, cy: float, cos: float, sen: float, escala: float, dx: float, dy: float, ar: float, ruido: np.ndarray | None):
    if puntos is None:
        return None
    p = puntos.astype(np.float64).copy()
    # La rotación se hace en unidades reales (x por la relación de aspecto) para no deformar.
    x = (p[:, 0] - cx) * ar
    y = p[:, 1] - cy
    xr = (x * cos - y * sen) * escala
    yr = (x * sen + y * cos) * escala
    p[:, 0] = xr / ar + cx + dx
    p[:, 1] = yr + cy + dy
    p[:, 2] = p[:, 2] * escala
    if ruido is not None:
        p += ruido[: len(p)]
    return p


def aumentar(fotogramas: list[Fotograma], rnd: np.random.Generator,
             rotacion_grados: float = 8.0, escala: float = 0.1, desplazamiento: float = 0.05,
             ruido: float = 0.004, velocidad: float = 0.2) -> list[Fotograma]:
    """Devuelve una variación aleatoria de la secuencia (no modifica la original)."""
    angulo = math.radians(rnd.uniform(-rotacion_grados, rotacion_grados))
    cos, sen = math.cos(angulo), math.sin(angulo)
    s = 1.0 + rnd.uniform(-escala, escala)
    dx, dy = rnd.uniform(-desplazamiento, desplazamiento, size=2)
    factor_t = 1.0 + rnd.uniform(-velocidad, velocidad)

    t0 = fotogramas[0].timestamp_ms
    salida = []
    for f in fotogramas:
        cuerpo = f.cuerpo
        if cuerpo is not None:
            cx, cy = float((cuerpo[1, 0] + cuerpo[2, 0]) / 2), float((cuerpo[1, 1] + cuerpo[2, 1]) / 2)
        else:
            cx, cy = 0.5, 0.5

        def t(p):
            r = rnd.normal(0.0, ruido, size=(len(p), 3)) if (p is not None and ruido > 0) else None
            return _transformar(p, cx, cy, cos, sen, s, dx, dy, f.relacion_aspecto, r)

        salida.append(replace(
            f,
            timestamp_ms=int(round(t0 + (f.timestamp_ms - t0) * factor_t)),
            mano_izquierda=t(f.mano_izquierda),
            mano_derecha=t(f.mano_derecha),
            cuerpo=t(f.cuerpo),
            rostro=t(f.rostro),
        ))
    return salida
