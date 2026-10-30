"""Características por fotograma. Copia exacta de `Caracteristicas.kt` (versión 1).

Orden del vector de cada fotograma (352 valores):
  1. Mano izquierda (127): presencia; 21×(x, y, z) respecto a los hombros; 21×(x, y, z) respecto a
     la muñeca, escalados por el tamaño de la mano.
  2. Mano derecha (127): igual.
  3. Cuerpo (15): presencia; 7×(x, y) respecto a los hombros.
  4. Rostro (79): presencia; 39×(x, y) respecto a los hombros.
  5. Velocidad (4): (vx, vy) de la muñeca izquierda y de la derecha, por segundo.

Se calcula en float32 para reproducir la aritmética de Kotlin (`Float`).
"""
from __future__ import annotations

import math
from dataclasses import dataclass

import numpy as np

from . import subconjuntos as S
from .fotograma import Fotograma

VERSION = 1
VERSION_SUBCONJUNTOS = S.VERSION

POR_MANO = 1 + S.PUNTOS_POR_MANO * 3 * 2          # 127
POR_CUERPO = 1 + S.PUNTOS_CUERPO * 2              # 15
POR_ROSTRO = 1 + S.PUNTOS_ROSTRO * 2              # 79
POR_VELOCIDAD = 4
POR_FOTOGRAMA = 2 * POR_MANO + POR_CUERPO + POR_ROSTRO + POR_VELOCIDAD   # 352

HOMBRO_IZQUIERDO = 1
HOMBRO_DERECHO = 2
BASE_DEDO_MEDIO = 9
ESCALA_SIN_CUERPO = np.float32(0.25)
MINIMO = np.float32(1e-6)

f32 = np.float32


@dataclass(frozen=True)
class Referencia:
    origen_x: np.float32
    origen_y: np.float32
    escala: np.float32


def _dist(x1, y1, x2, y2) -> np.float32:
    dx = f32(x1) - f32(x2)
    dy = f32(y1) - f32(y2)
    return f32(math.sqrt(f32(dx * dx + dy * dy)))


def referencia(f: Fotograma) -> Referencia:
    ar = f32(f.relacion_aspecto)
    cuerpo = f.cuerpo if f.cuerpo is not None and len(f.cuerpo) == S.PUNTOS_CUERPO else None
    if cuerpo is not None:
        izq = cuerpo[HOMBRO_IZQUIERDO].astype(np.float32)
        der = cuerpo[HOMBRO_DERECHO].astype(np.float32)
        d = _dist(izq[0] * ar, izq[1], der[0] * ar, der[1])
        if d > MINIMO:
            return Referencia(f32((izq[0] + der[0]) / f32(2) * ar), f32((izq[1] + der[1]) / f32(2)), d)
    return Referencia(f32(f32(0.5) * ar), f32(0.5), ESCALA_SIN_CUERPO)


def de_ventana(ventana: list[Fotograma]) -> np.ndarray:
    """Vector `(len(ventana), 352)` en float32, fotograma a fotograma."""
    salida = np.zeros((len(ventana), POR_FOTOGRAMA), dtype=np.float32)
    for i, f in enumerate(ventana):
        salida[i] = de_fotograma(f, ventana[i - 1] if i > 0 else None)
    return salida


def de_fotograma(f: Fotograma, anterior: Fotograma | None) -> np.ndarray:
    d = np.zeros(POR_FOTOGRAMA, dtype=np.float32)
    ref = referencia(f)
    ar = f32(f.relacion_aspecto)
    i = 0
    i = _mano(f.mano_izquierda, ref, ar, d, i)
    i = _mano(f.mano_derecha, ref, ar, d, i)
    cuerpo = f.cuerpo if f.cuerpo is not None and len(f.cuerpo) == S.PUNTOS_CUERPO else None
    rostro = f.rostro if f.rostro is not None and len(f.rostro) == S.PUNTOS_ROSTRO else None
    i = _puntos_2d(cuerpo, S.PUNTOS_CUERPO, ref, ar, d, i)
    i = _puntos_2d(rostro, S.PUNTOS_ROSTRO, ref, ar, d, i)

    if anterior is not None:
        dt = f32(f32(f.timestamp_ms - anterior.timestamp_ms) / f32(1000))
        if dt > 0:
            ref_ant = referencia(anterior)
            ar_ant = f32(anterior.relacion_aspecto)
            _velocidad(f.mano_izquierda, anterior.mano_izquierda, ref, ref_ant, ar, ar_ant, dt, d, i)
            _velocidad(f.mano_derecha, anterior.mano_derecha, ref, ref_ant, ar, ar_ant, dt, d, i + 2)
    return d


def _mano(mano, ref: Referencia, ar, d: np.ndarray, inicio: int) -> int:
    if mano is None or len(mano) != S.PUNTOS_POR_MANO:
        return inicio + POR_MANO
    m = mano.astype(np.float32)
    i = inicio
    d[i] = 1
    i += 1
    for p in m:
        d[i] = f32(p[0] * ar - ref.origen_x) / ref.escala
        d[i + 1] = f32(p[1] - ref.origen_y) / ref.escala
        d[i + 2] = f32(p[2] * ar) / ref.escala
        i += 3
    muneca = m[0]
    base = m[BASE_DEDO_MEDIO]
    tam = _dist(muneca[0] * ar, muneca[1], base[0] * ar, base[1])
    if tam > MINIMO:
        for p in m:
            d[i] = f32(f32(p[0] - muneca[0]) * ar) / tam
            d[i + 1] = f32(p[1] - muneca[1]) / tam
            d[i + 2] = f32(f32(p[2] - muneca[2]) * ar) / tam
            i += 3
    else:
        i += S.PUNTOS_POR_MANO * 3
    return i


def _puntos_2d(puntos, cantidad: int, ref: Referencia, ar, d: np.ndarray, inicio: int) -> int:
    if puntos is None:
        return inicio + 1 + cantidad * 2
    i = inicio
    d[i] = 1
    i += 1
    for p in puntos.astype(np.float32):
        d[i] = f32(p[0] * ar - ref.origen_x) / ref.escala
        d[i + 1] = f32(p[1] - ref.origen_y) / ref.escala
        i += 2
    return i


def _velocidad(mano, mano_ant, ref, ref_ant, ar, ar_ant, dt, d: np.ndarray, i: int) -> None:
    if mano is None or mano_ant is None:
        return
    a = mano[0].astype(np.float32)
    p = mano_ant[0].astype(np.float32)
    ax = f32(a[0] * ar - ref.origen_x) / ref.escala
    ay = f32(a[1] - ref.origen_y) / ref.escala
    px = f32(p[0] * ar_ant - ref_ant.origen_x) / ref_ant.escala
    py = f32(p[1] - ref_ant.origen_y) / ref_ant.escala
    d[i] = f32(ax - px) / dt
    d[i + 1] = f32(ay - py) / dt
