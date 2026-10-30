"""Métricas de evaluación, implementadas con NumPy para que sean fáciles de revisar."""
from __future__ import annotations

from dataclasses import dataclass

import numpy as np


def softmax(logits: np.ndarray, temperatura: float = 1.0) -> np.ndarray:
    z = logits.astype(np.float64) / temperatura
    z -= z.max(axis=1, keepdims=True)
    e = np.exp(z)
    return e / e.sum(axis=1, keepdims=True)


def energia(logits: np.ndarray, temperatura: float = 1.0) -> np.ndarray:
    """−T · log Σ exp(logit / T), igual que `PostProcesador.kt`. Alta = la entrada no encaja."""
    z = logits.astype(np.float64) / temperatura
    m = z.max(axis=1)
    return -temperatura * (m + np.log(np.exp(z - m[:, None]).sum(axis=1)))


def matriz_confusion(verdad: np.ndarray, prediccion: np.ndarray, clases: int) -> np.ndarray:
    """Filas: clase real. Columnas: clase predicha."""
    m = np.zeros((clases, clases), dtype=np.int64)
    np.add.at(m, (verdad, prediccion), 1)
    return m


@dataclass(frozen=True)
class PorClase:
    precision: float
    recall: float
    f1: float
    soporte: int


def por_clase(matriz: np.ndarray) -> list[PorClase]:
    salida = []
    for c in range(len(matriz)):
        vp = matriz[c, c]
        predichas = matriz[:, c].sum()
        reales = matriz[c, :].sum()
        p = vp / predichas if predichas else 0.0
        r = vp / reales if reales else 0.0
        f1 = 2 * p * r / (p + r) if (p + r) else 0.0
        salida.append(PorClase(float(p), float(r), float(f1), int(reales)))
    return salida


def exactitud(matriz: np.ndarray) -> float:
    total = matriz.sum()
    return float(np.trace(matriz) / total) if total else 0.0


def macro_f1(matriz: np.ndarray, solo_con_soporte: bool = True) -> float:
    """Media de F1 por clase. Por defecto ignora clases sin ejemplos en el conjunto evaluado."""
    clases = [x for x in por_clase(matriz) if x.soporte > 0 or not solo_con_soporte]
    return float(np.mean([x.f1 for x in clases])) if clases else 0.0


def ece(probabilidades: np.ndarray, verdad: np.ndarray, contenedores: int = 10) -> float:
    """Error de calibración esperado: diferencia media entre confianza y acierto real."""
    confianza = probabilidades.max(axis=1)
    acierto = (probabilidades.argmax(axis=1) == verdad).astype(np.float64)
    bordes = np.linspace(0.0, 1.0, contenedores + 1)
    total = 0.0
    for a, b in zip(bordes[:-1], bordes[1:]):
        en = (confianza > a) & (confianza <= b) if a > 0 else (confianza >= a) & (confianza <= b)
        if en.any():
            total += en.mean() * abs(acierto[en].mean() - confianza[en].mean())
    return float(total)


def nll(logits: np.ndarray, verdad: np.ndarray, temperatura: float) -> float:
    p = softmax(logits, temperatura)
    return float(-np.mean(np.log(np.clip(p[np.arange(len(verdad)), verdad], 1e-12, 1.0))))
