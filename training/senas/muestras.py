"""Muestras de entrenamiento: los puntos de una persona haciendo una seña una vez.

Cada muestra se guarda en un archivo `.npz` (sin imágenes: solo coordenadas), con su persona,
seña, repetición y fuente. La persona es necesaria para separar entrenamiento, validación y prueba
sin compartir personas.
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

import numpy as np

from .fotograma import Fotograma, a_arrays, desde_arrays

_SEGURO = re.compile(r"[^A-Za-z0-9_.-]+")


def nombre_archivo(fuente: str, persona: str, sena: str, repeticion: str) -> str:
    """Nombre del `.npz` de una muestra (sirve para saber si ya se extrajo antes de procesarla)."""
    return "__".join(_SEGURO.sub("-", p) for p in (fuente, persona, sena, repeticion)) + ".npz"


@dataclass(frozen=True)
class Muestra:
    persona: str
    sena: str          # identificador del vocabulario, p. ej. "lsc.hola"
    repeticion: str
    fuente: str        # conjunto de datos de origen, p. ej. "LSC70"
    fotogramas: list[Fotograma]

    def __post_init__(self):
        if not self.fotogramas:
            raise ValueError("Una muestra necesita al menos un fotograma")
        tiempos = [f.timestamp_ms for f in self.fotogramas]
        if any(b < a for a, b in zip(tiempos, tiempos[1:])):
            raise ValueError("Los fotogramas deben estar en orden cronológico")

    @property
    def nombre_archivo(self) -> str:
        return nombre_archivo(self.fuente, self.persona, self.sena, self.repeticion)

    @property
    def proporcion_con_manos(self) -> float:
        return sum(f.hay_manos for f in self.fotogramas) / len(self.fotogramas)


def guardar(muestra: Muestra, carpeta: Path) -> Path:
    carpeta.mkdir(parents=True, exist_ok=True)
    ruta = carpeta / muestra.nombre_archivo
    np.savez_compressed(
        ruta,
        persona=np.array(muestra.persona),
        sena=np.array(muestra.sena),
        repeticion=np.array(muestra.repeticion),
        fuente=np.array(muestra.fuente),
        **a_arrays(muestra.fotogramas),
    )
    return ruta


def cargar(ruta: Path) -> Muestra:
    with np.load(ruta, allow_pickle=False) as d:
        return Muestra(
            persona=str(d["persona"]),
            sena=str(d["sena"]),
            repeticion=str(d["repeticion"]),
            fuente=str(d["fuente"]),
            fotogramas=desde_arrays(d["timestamps"], d["mano_izquierda"], d["mano_derecha"], d["cuerpo"], d["rostro"], d["relacion_aspecto"]),
        )


def cargar_carpeta(carpeta: Path) -> list[Muestra]:
    rutas = sorted(carpeta.rglob("*.npz"))
    if not rutas:
        raise FileNotFoundError(f"No hay muestras .npz en {carpeta}")
    return [cargar(r) for r in rutas]
