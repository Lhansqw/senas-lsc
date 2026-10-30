"""Conversión de los resultados de MediaPipe Holistic en fotogramas (sin depender de MediaPipe).

Sigue exactamente lo que hace la app (`ExtractorLandmarks.kt` + `Subconjuntos.kt`):
- `left_hand_landmarks` → mano izquierda, `right_hand_landmarks` → mano derecha, con la imagen
  **sin espejo** (si un conjunto de datos está grabado en espejo, hay que voltearlo antes).
- Listas vacías o incompletas → no detectado.
"""
from __future__ import annotations

from typing import Iterable, Optional, Sequence

import numpy as np

from . import subconjuntos as S
from .fotograma import Fotograma


def _a_array(puntos: Sequence) -> np.ndarray:
    return np.array([[p.x, p.y, p.z] for p in puntos], dtype=np.float64)


def _mano(puntos: Sequence) -> Optional[np.ndarray]:
    return _a_array(puntos) if puntos is not None and len(puntos) == S.PUNTOS_POR_MANO else None


def _seleccion(puntos: Sequence, minimo: int, indices: list[int]) -> Optional[np.ndarray]:
    if puntos is None or len(puntos) < minimo:
        return None
    todos = _a_array(puntos)
    return todos[indices]


def fotograma_desde_resultado(resultado, timestamp_ms: int, ancho: int, alto: int) -> Fotograma:
    """`resultado` es un `HolisticLandmarkerResult` (o cualquier objeto con los mismos campos)."""
    return Fotograma(
        timestamp_ms=timestamp_ms,
        mano_izquierda=_mano(resultado.left_hand_landmarks),
        mano_derecha=_mano(resultado.right_hand_landmarks),
        cuerpo=_seleccion(resultado.pose_landmarks, S.PUNTOS_POSE, S.CUERPO),
        rostro=_seleccion(resultado.face_landmarks, S.PUNTOS_ROSTRO_MINIMO, S.ROSTRO),
        relacion_aspecto=ancho / alto,
    )


def tiempos_uniformes(cantidad: int, duracion_ms: int) -> list[int]:
    """Marcas de tiempo para una secuencia de imágenes sin tiempos propios (p. ej. LSC70).

    Se reparten en `duracion_ms`, del 0 al final, con redondeo al entero más cercano.
    """
    if cantidad < 1:
        raise ValueError("Se necesita al menos una imagen")
    if cantidad == 1:
        return [0]
    return [round(i * duracion_ms / (cantidad - 1)) for i in range(cantidad)]


def ordenar_imagenes(nombres: Iterable[str]) -> list[str]:
    """Orden natural: «frame2» antes que «frame10»."""
    import re

    def clave(n: str):
        return [int(t) if t.isdigit() else t.lower() for t in re.split(r"(\d+)", n)]

    return sorted(nombres, key=clave)
