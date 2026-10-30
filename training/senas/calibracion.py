"""Calibración con el conjunto de validación (personas que no se usaron para entrenar).

- Temperatura: el valor que minimiza la log-verosimilitud negativa, para que la confianza que
  muestra la app (p. ej. «82 %») se parezca a la probabilidad real de acertar.
- Umbral de energía: si no hay ejemplos de movimientos que no son señas, se fija para conservar el
  `cobertura` (p. ej. 95 %) de las señas conocidas de validación; por encima, la entrada se
  considera desconocida.
"""
from __future__ import annotations

import numpy as np

from .metricas import energia, nll


def temperatura_optima(logits: np.ndarray, verdad: np.ndarray, minimo: float = 0.25, maximo: float = 10.0, pasos: int = 400) -> float:
    if len(verdad) == 0:
        return 1.0
    candidatas = np.exp(np.linspace(np.log(minimo), np.log(maximo), pasos))
    perdidas = [nll(logits, verdad, t) for t in candidatas]
    return float(candidatas[int(np.argmin(perdidas))])


def umbral_energia(logits_conocidas: np.ndarray, temperatura: float, cobertura: float = 0.95) -> float:
    if not 0 < cobertura < 1:
        raise ValueError("La cobertura debe estar entre 0 y 1")
    if len(logits_conocidas) == 0:
        raise ValueError("Hacen falta ejemplos de validación para fijar el umbral")
    return float(np.quantile(energia(logits_conocidas, temperatura), cobertura))
