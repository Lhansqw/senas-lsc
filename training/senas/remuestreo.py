"""Remuestreo de una ventana a instantes equiespaciados. Copia exacta de `Remuestreo.kt`.

Los instantes van de `fin − duración` a `fin`, ambos incluidos, con división entera (como en
Kotlin con `Long`). Para cada instante se toma el fotograma más cercano y, en caso de empate, el
anterior.
"""
from __future__ import annotations

from .fotograma import Fotograma


def a_tiempo_uniforme(fotogramas: list[Fotograma], cantidad: int, duracion_ms: int) -> list[Fotograma]:
    if not fotogramas:
        raise ValueError("No hay fotogramas que remuestrear")
    if cantidad < 2:
        raise ValueError("Se necesitan al menos 2 instantes")
    if duracion_ms <= 0:
        raise ValueError("La duración debe ser positiva")

    fin = fotogramas[-1].timestamp_ms
    inicio = fin - duracion_ms
    j = 0
    salida = []
    for i in range(cantidad):
        instante = inicio + duracion_ms * i // (cantidad - 1)
        while j + 1 < len(fotogramas) and abs(fotogramas[j + 1].timestamp_ms - instante) < abs(fotogramas[j].timestamp_ms - instante):
            j += 1
        salida.append(fotogramas[j].con_timestamp(max(0, instante)))
    return salida
