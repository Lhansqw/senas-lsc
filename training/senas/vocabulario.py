"""Vocabulario del modelo y correspondencia con las carpetas de los conjuntos de datos.

Archivo `vocabulario.json` (ver `vocabulario.ejemplo.json`):
{
  "lengua": "lsc",
  "senas": [
    {"id": "lsc.hola", "glosa": "HOLA", "significado": "hola", "tipo": "palabra",
     "carpetas": ["Hola", "HOLA"], "interrogativa": false, "variante": null}
  ]
}

Cada seña debe estar validada por una persona competente en LSC antes de entrenar con ella.
"""
from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from pathlib import Path

TIPOS = ("letra", "numero", "palabra")
_ID = re.compile(r"^[a-z]{2,8}\.[a-z0-9_]+$")


@dataclass(frozen=True)
class EntradaVocabulario:
    id: str
    glosa: str
    significado: str
    tipo: str
    carpetas: tuple[str, ...] = field(default_factory=tuple)
    interrogativa: bool = False
    variante: str | None = None

    def a_json(self) -> dict:
        """Forma que espera `modelo.json` (sin las carpetas, que solo usa el entrenamiento)."""
        salida = {"id": self.id, "glosa": self.glosa, "significado": self.significado, "tipo": self.tipo}
        if self.interrogativa:
            salida["interrogativa"] = True
        if self.variante:
            salida["variante"] = self.variante
        return salida


@dataclass(frozen=True)
class Vocabulario:
    lengua: str
    entradas: tuple[EntradaVocabulario, ...]

    def por_carpeta(self, carpeta: str) -> EntradaVocabulario | None:
        clave = carpeta.strip().lower()
        for e in self.entradas:
            if clave in (c.lower() for c in e.carpetas):
                return e
        return None

    def por_id(self, ident: str) -> EntradaVocabulario | None:
        return next((e for e in self.entradas if e.id == ident), None)


def leer(ruta: Path) -> Vocabulario:
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    lengua = datos["lengua"]
    entradas = []
    vistos_ids, vistas_carpetas = set(), {}
    for i, s in enumerate(datos["senas"]):
        e = EntradaVocabulario(
            id=s["id"], glosa=s["glosa"], significado=s["significado"], tipo=s["tipo"],
            carpetas=tuple(s.get("carpetas", [])), interrogativa=bool(s.get("interrogativa", False)), variante=s.get("variante"),
        )
        if not _ID.match(e.id):
            raise ValueError(f"senas[{i}]: id no válido '{e.id}' (formato lengua.nombre)")
        if not e.id.startswith(lengua + "."):
            raise ValueError(f"senas[{i}]: '{e.id}' no es de la lengua '{lengua}'")
        if e.tipo not in TIPOS:
            raise ValueError(f"senas[{i}]: tipo '{e.tipo}' no válido ({', '.join(TIPOS)})")
        if not e.glosa.strip() or not e.significado.strip():
            raise ValueError(f"senas[{i}]: glosa y significado no pueden estar vacíos")
        if e.id in vistos_ids:
            raise ValueError(f"Identificador repetido: {e.id}")
        vistos_ids.add(e.id)
        for c in e.carpetas:
            if c.lower() in vistas_carpetas:
                raise ValueError(f"La carpeta '{c}' está asignada a {vistas_carpetas[c.lower()]} y a {e.id}")
            vistas_carpetas[c.lower()] = e.id
        entradas.append(e)
    if not entradas:
        raise ValueError("El vocabulario está vacío")
    return Vocabulario(lengua, tuple(entradas))
