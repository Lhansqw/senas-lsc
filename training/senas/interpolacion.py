"""Densificación de secuencias con pocos fotogramas.

LSC70 guarda cada seña como 6 fotos repartidas en ~1 s, mientras que la app analiza unos 30
fotogramas por segundo. Si se remuestrea directamente, casi todos los instantes repiten la misma
foto (velocidad 0) y unos pocos saltan de golpe (velocidad seis veces mayor que la real): el modelo
aprendería velocidades que la cámara nunca produce. Para evitarlo, antes de calcular la ventana se
interpolan los puntos linealmente entre fotos consecutivas hasta `fps` fotogramas por segundo.

Una parte que solo se detectó en una de las dos fotos se copia de la foto más cercana (o queda sin
detectar si la más cercana no la tiene), igual que ocurriría al grabar.
"""
from __future__ import annotations

import numpy as np

from .fotograma import Fotograma

PARTES = ("mano_izquierda", "mano_derecha", "cuerpo", "rostro")


def intervalo_mediano_ms(fotogramas: list[Fotograma]) -> float:
    tiempos = np.array([f.timestamp_ms for f in fotogramas], dtype=np.float64)
    if len(tiempos) < 2:
        return 0.0
    return float(np.median(np.diff(tiempos)))


def necesita_densificar(fotogramas: list[Fotograma], fps_minimo: float) -> bool:
    """Verdadero si los fotogramas están más separados de lo que vería la app a `fps_minimo`."""
    intervalo = intervalo_mediano_ms(fotogramas)
    return intervalo > 1000.0 / fps_minimo


def _parte(a, b, fraccion: float):
    if a is not None and b is not None:
        return a + (b - a) * fraccion
    return a if fraccion < 0.5 else b


def densificar(fotogramas: list[Fotograma], fps: float = 30.0) -> list[Fotograma]:
    """Devuelve fotogramas cada `1000 / fps` ms entre el primero y el último (ambos incluidos)."""
    if len(fotogramas) < 2:
        return list(fotogramas)
    inicio, fin = fotogramas[0].timestamp_ms, fotogramas[-1].timestamp_ms
    pasos = max(1, round((fin - inicio) * fps / 1000.0))
    salida, j = [], 0
    for k in range(pasos + 1):
        t = inicio + round((fin - inicio) * k / pasos)
        while j + 2 < len(fotogramas) and fotogramas[j + 1].timestamp_ms <= t:
            j += 1
        a, b = fotogramas[j], fotogramas[j + 1]
        duracion = b.timestamp_ms - a.timestamp_ms
        fraccion = 0.0 if duracion <= 0 else min(1.0, max(0.0, (t - a.timestamp_ms) / duracion))
        partes = {p: _parte(getattr(a, p), getattr(b, p), fraccion) for p in PARTES}
        aspecto = a.relacion_aspecto + (b.relacion_aspecto - a.relacion_aspecto) * fraccion
        salida.append(Fotograma(t, relacion_aspecto=aspecto, **partes))
    return salida
