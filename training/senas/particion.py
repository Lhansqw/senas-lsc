"""Partición por persona: entrenamiento, validación y prueba no comparten personas.

Es la única forma honesta de estimar cómo funcionará el reconocedor con alguien nuevo. Si una
persona aparece en entrenamiento y en prueba, el modelo puede memorizar su forma de señar y las
métricas salen infladas.
"""
from __future__ import annotations

import random
from dataclasses import dataclass

from .muestras import Muestra


@dataclass(frozen=True)
class Particion:
    entrenamiento: tuple[str, ...]
    validacion: tuple[str, ...]
    prueba: tuple[str, ...]

    def de(self, persona: str) -> str:
        if persona in self.entrenamiento:
            return "entrenamiento"
        if persona in self.validacion:
            return "validacion"
        if persona in self.prueba:
            return "prueba"
        raise KeyError(persona)

    def a_json(self) -> dict:
        return {"entrenamiento": list(self.entrenamiento), "validacion": list(self.validacion), "prueba": list(self.prueba)}


def por_persona(muestras: list[Muestra], fraccion_validacion: float = 0.15, fraccion_prueba: float = 0.15, semilla: int = 42) -> Particion:
    """Reparte las personas al azar (reproducible con la semilla).

    Garantiza al menos una persona en cada parte y comprueba que todas las señas aparecen en
    entrenamiento; si no, lanza un error que explica qué falta.
    """
    if not 0 < fraccion_validacion < 1 or not 0 < fraccion_prueba < 1 or fraccion_validacion + fraccion_prueba >= 1:
        raise ValueError("Fracciones no válidas")
    personas = sorted({m.persona for m in muestras})
    if len(personas) < 3:
        raise ValueError(f"Hacen falta al menos 3 personas distintas para separar por persona (hay {len(personas)})")

    rnd = random.Random(semilla)
    rnd.shuffle(personas)
    n = len(personas)
    n_prueba = max(1, round(n * fraccion_prueba))
    n_val = max(1, round(n * fraccion_validacion))
    if n_prueba + n_val >= n:
        n_prueba, n_val = 1, 1

    particion = Particion(
        entrenamiento=tuple(sorted(personas[n_prueba + n_val:])),
        validacion=tuple(sorted(personas[n_prueba:n_prueba + n_val])),
        prueba=tuple(sorted(personas[:n_prueba])),
    )

    senas = {m.sena for m in muestras}
    en_entrenamiento = {m.sena for m in muestras if m.persona in particion.entrenamiento}
    faltan = senas - en_entrenamiento
    if faltan:
        raise ValueError(f"Estas señas no tienen ninguna muestra en entrenamiento: {sorted(faltan)}. Prueba otra semilla o graba más personas.")
    return particion
