"""Escritura y validación de `modelo.json` (formato 1), igual que lo lee `LectorContrato.kt`."""
from __future__ import annotations

import json
import re
from pathlib import Path

from . import caracteristicas as C
from .vocabulario import Vocabulario

FORMATO = 1
ETIQUETA_SIN_SENA = "sin_sena"
_ID = re.compile(r"^[a-z]{2,8}\.[a-z0-9_]+$")


def construir(nombre: str, version: str, vocabulario: Vocabulario, etiquetas: list[str], fotogramas: int, duracion_ms: int,
              temperatura: float, umbral_energia: float | None, datos_entrenamiento: list[str], metricas: dict[str, float],
              usa_rostro: bool = True) -> dict:
    entradas = [vocabulario.por_id(e) for e in etiquetas if e != ETIQUETA_SIN_SENA]
    if any(e is None for e in entradas):
        faltan = [e for e in etiquetas if e != ETIQUETA_SIN_SENA and vocabulario.por_id(e) is None]
        raise ValueError(f"Etiquetas sin entrada de vocabulario: {faltan}")
    contrato = {
        "formato": FORMATO,
        "nombre": nombre,
        "version": version,
        "lengua": vocabulario.lengua,
        "caracteristicas": {"version": C.VERSION, "version_subconjuntos": C.VERSION_SUBCONJUNTOS, "por_fotograma": C.POR_FOTOGRAMA,
                            "usa_rostro": bool(usa_rostro)},
        "ventana": {"fotogramas": fotogramas, "duracion_ms": duracion_ms},
        "salida": {
            "etiquetas": etiquetas,
            "etiqueta_sin_sena": ETIQUETA_SIN_SENA if ETIQUETA_SIN_SENA in etiquetas else None,
            "temperatura": float(temperatura),
            "umbral_energia": None if umbral_energia is None else float(umbral_energia),
        },
        "vocabulario": [e.a_json() for e in entradas],
        "datos_entrenamiento": datos_entrenamiento,
        "metricas": {k: float(v) for k, v in metricas.items()},
    }
    errores = validar(contrato)
    if errores:
        raise ValueError("modelo.json no sería válido: " + "; ".join(errores))
    return contrato


def validar(c: dict) -> list[str]:
    """Mismas comprobaciones que `ContratoModelo.validar()` en la app."""
    e = []
    car, ven, sal = c["caracteristicas"], c["ventana"], c["salida"]
    if c["formato"] != FORMATO:
        e.append(f"formato {c['formato']}")
    if car["version"] != C.VERSION or car["version_subconjuntos"] != C.VERSION_SUBCONJUNTOS or car["por_fotograma"] != C.POR_FOTOGRAMA:
        e.append("características incompatibles con la app")
    if not 2 <= ven["fotogramas"] <= 256:
        e.append("número de fotogramas no válido")
    if not 200 <= ven["duracion_ms"] <= 10_000:
        e.append("duración de ventana no válida")
    if not sal["temperatura"] > 0:
        e.append("la temperatura debe ser positiva")
    etiquetas = sal["etiquetas"]
    if len(set(etiquetas)) != len(etiquetas):
        e.append("etiquetas repetidas")
    sin = sal["etiqueta_sin_sena"]
    if sin is not None and sin not in etiquetas:
        e.append("la etiqueta «sin seña» no está entre las etiquetas")
    senas = [x for x in etiquetas if x != sin]
    if not senas:
        e.append("el modelo no tiene ninguna seña")
    ids = [v["id"] for v in c["vocabulario"]]
    if set(senas) != set(ids):
        e.append("las etiquetas y el vocabulario no coinciden")
    if any(not _ID.match(i) or not i.startswith(c["lengua"] + ".") for i in ids):
        e.append("identificadores de vocabulario no válidos")
    return e


def guardar(contrato: dict, ruta: Path) -> Path:
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_text(json.dumps(contrato, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return ruta
