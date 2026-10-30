"""Representación de los puntos de un fotograma, equivalente a `LandmarkFrame` en Kotlin.

Cada parte es un array `(n, 3)` de float con coordenadas normalizadas de MediaPipe, o `None` si no
se detectó. «Mano izquierda» es la mano izquierda real de la persona: la imagen llega a MediaPipe
sin espejo (regla de la fase 5).
"""
from __future__ import annotations

from dataclasses import dataclass, replace
from typing import Optional

import numpy as np

from . import subconjuntos as S


@dataclass(frozen=True)
class Fotograma:
    timestamp_ms: int
    mano_izquierda: Optional[np.ndarray]
    mano_derecha: Optional[np.ndarray]
    cuerpo: Optional[np.ndarray]
    rostro: Optional[np.ndarray]
    relacion_aspecto: float = 1.0

    def __post_init__(self):
        if self.timestamp_ms < 0:
            raise ValueError(f"timestamp_ms no puede ser negativo: {self.timestamp_ms}")
        if not self.relacion_aspecto > 0:
            raise ValueError(f"La relación de aspecto debe ser positiva: {self.relacion_aspecto}")
        for nombre in ("mano_izquierda", "mano_derecha"):
            mano = getattr(self, nombre)
            if mano is not None and mano.shape != (S.PUNTOS_POR_MANO, 3):
                raise ValueError(f"{nombre} debe tener forma (21, 3), no {mano.shape}")

    @property
    def hay_manos(self) -> bool:
        return self.mano_izquierda is not None or self.mano_derecha is not None

    def con_timestamp(self, timestamp_ms: int) -> "Fotograma":
        return replace(self, timestamp_ms=timestamp_ms)


def _o_nada(arr: np.ndarray) -> Optional[np.ndarray]:
    """Convierte un bloque con NaN (no detectado, tal como se guarda en disco) en None."""
    return None if np.isnan(arr).any() else arr.astype(np.float64)


def desde_arrays(timestamps, mano_izq, mano_der, cuerpo, rostro, aspecto) -> list[Fotograma]:
    """Reconstruye fotogramas a partir de los arrays de un archivo `.npz` (NaN = no detectado)."""
    return [
        Fotograma(
            int(timestamps[i]),
            _o_nada(mano_izq[i]),
            _o_nada(mano_der[i]),
            _o_nada(cuerpo[i]),
            _o_nada(rostro[i]),
            float(aspecto[i]),
        )
        for i in range(len(timestamps))
    ]


def a_arrays(fotogramas: list[Fotograma]) -> dict[str, np.ndarray]:
    """Inverso de `desde_arrays`: rellena con NaN lo que no se detectó."""
    def apilar(attr: str, puntos: int) -> np.ndarray:
        salida = np.full((len(fotogramas), puntos, 3), np.nan, dtype=np.float32)
        for i, f in enumerate(fotogramas):
            valor = getattr(f, attr)
            if valor is not None:
                salida[i] = valor
        return salida

    return {
        "timestamps": np.array([f.timestamp_ms for f in fotogramas], dtype=np.int64),
        "mano_izquierda": apilar("mano_izquierda", S.PUNTOS_POR_MANO),
        "mano_derecha": apilar("mano_derecha", S.PUNTOS_POR_MANO),
        "cuerpo": apilar("cuerpo", S.PUNTOS_CUERPO),
        "rostro": apilar("rostro", S.PUNTOS_ROSTRO),
        "relacion_aspecto": np.array([f.relacion_aspecto for f in fotogramas], dtype=np.float32),
    }
