"""Convierte las grabaciones de la app (fase 6, herramienta de grabación) en muestras de entrenamiento.

La app guarda cada grabación como un JSON con solo puntos de MediaPipe (nunca imágenes) en
`Android/data/com.lhansqw.senaslsc/files/grabaciones/` del teléfono. Copia esa carpeta al PC
(por USB o con `adb pull`) y ejecuta:

    python importar_grabaciones.py --entrada data/grabaciones --salida data/puntos --vocabulario vocabulario.lsc70.json

Cada persona se guarda como `app_<alias>`, distinta de las de LSC70, para que la partición por
persona no mezcle a nadie entre entrenamiento y prueba.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

import numpy as np

from senas import muestras as M
from senas import subconjuntos as S
from senas import vocabulario as V
from senas.contrato import ETIQUETA_SIN_SENA
from senas.fotograma import Fotograma

FORMATO = 1
FUENTE = "GRABACION_APP"
_TAMANOS = {"izquierda": S.PUNTOS_POR_MANO, "derecha": S.PUNTOS_POR_MANO, "cuerpo": S.PUNTOS_CUERPO, "rostro": S.PUNTOS_ROSTRO}


def _puntos(valor, parte: str):
    if valor is None:
        return None
    arr = np.asarray(valor, dtype=np.float64)
    if arr.shape != (_TAMANOS[parte], 3):
        return None   # subconjunto de otro tamaño: igual que en la app, se trata como no detectado
    return arr


def leer(ruta: Path) -> M.Muestra:
    datos = json.loads(ruta.read_text(encoding="utf-8"))
    if datos.get("formato") != FORMATO or datos.get("fuente") != FUENTE:
        raise ValueError(f"{ruta.name}: no es una grabación de la app (formato {datos.get('formato')})")
    fotogramas = [
        Fotograma(int(f["t"]), _puntos(f["izquierda"], "izquierda"), _puntos(f["derecha"], "derecha"),
                  _puntos(f["cuerpo"], "cuerpo"), _puntos(f["rostro"], "rostro"), float(f["aspecto"]))
        for f in datos["fotogramas"]
    ]
    return M.Muestra(f"app_{datos['persona']}", datos["sena"], f"r{int(datos['repeticion'])}", FUENTE, fotogramas)


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrada", type=Path, required=True, help="Carpeta con los .json copiados del teléfono")
    ap.add_argument("--salida", type=Path, required=True)
    ap.add_argument("--vocabulario", type=Path, help="Si se indica, se omiten las señas que no están en él")
    args = ap.parse_args(argv)

    vocabulario = V.leer(args.vocabulario) if args.vocabulario else None
    guardadas, omitidas, errores = Counter(), Counter(), []
    for ruta in sorted(args.entrada.rglob("*.json")):
        try:
            muestra = leer(ruta)
        except (ValueError, KeyError, json.JSONDecodeError) as e:
            errores.append(f"{ruta.name}: {e}")
            continue
        if vocabulario and muestra.sena != ETIQUETA_SIN_SENA and vocabulario.por_id(muestra.sena) is None:
            omitidas[muestra.sena] += 1
            continue
        M.guardar(muestra, args.salida)
        guardadas[muestra.sena] += 1

    print(f"Grabaciones importadas en {args.salida}: {sum(guardadas.values())}")
    for sena, n in sorted(guardadas.items()):
        print(f"  {sena}: {n}")
    if omitidas:
        print(f"Señas fuera del vocabulario (omitidas): {', '.join(sorted(omitidas))}")
    for e in errores:
        print(f"  No se pudo leer {e}")
    return 0 if guardadas else 1


if __name__ == "__main__":
    sys.exit(main())
