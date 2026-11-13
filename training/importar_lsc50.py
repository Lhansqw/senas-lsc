"""Convierte los puntos de LSC50 (Universidad de los Andes) en muestras de entrenamiento.

LSC50 ya trae los puntos de MediaPipe Holistic en CSV, así que no hace falta procesar vídeos. Basta
con `LANDMARKS.zip` (se lee sin descomprimir):

    powershell -ExecutionPolicy Bypass -File .\\descargar_lsc50.ps1 -Filtro LANDMARKS
    python importar_lsc50.py --zip data/LSC50/LANDMARKS.zip --salida data/puntos --vocabulario vocabulario.lsc.json

Formato (comprobado con la descarga real):
- `LANDMARKS/BODY_LANDMARKS/CCCC_VVVV_RRRR.csv` (seña, persona, repetición) con los 33 puntos del
  cuerpo, y `HANDS_LANDMARKS/LEFT_HAND_LANDMARKS/…` y `RIGHT_HAND_LANDMARKS/…` con 21 por mano.
  Una fila por fotograma (vídeo de 24 fps, 1920×1080), sin espejo: la mano izquierda es la real.
- El rostro sale de otra cámara (50 fps, sin sincronizar) y no se usa.
- Las manos nunca faltan: cuando MediaPipe no las ve, el CSV trae una mano encogida casi a un punto.
  Se trata como no detectada, igual que en la app.

Cada clip dura unos 2,7 s, con la persona quieta al principio y al final. Se recorta a la parte en
la que alguna muñeca sube por encima de la cintura (mediana 1,1 s) y se reparte en 1000 ms, como las
6 fotos de LSC70, para que la ventana del modelo vea la seña entera y no la vuelta al reposo.

Las personas se guardan como `LSC50_vN`, distintas de las de LSC70.
"""
from __future__ import annotations

import argparse
import io
import re
import sys
import zipfile
from collections import Counter
from pathlib import Path

import numpy as np

from senas import muestras as M
from senas import subconjuntos as S
from senas import vocabulario as V
from senas.fotograma import Fotograma

FUENTE = "LSC50"
FPS = 24
ASPECTO = 1920 / 1080
DURACION_MS = 1000
MARGEN = 3                 # fotogramas antes y después de la parte activa
MANO_MINIMA = 0.05         # tamaño de la mano (muñeca → nudillo medio) / anchura de hombros
_NOMBRE = re.compile(r"^(\d{4})_(\d{4})_(\d{4})\.csv$")
_CUERPO = "LANDMARKS/BODY_LANDMARKS/"
_MANOS = {"izquierda": "LANDMARKS/HANDS_LANDMARKS/LEFT_HAND_LANDMARKS/",
          "derecha": "LANDMARKS/HANDS_LANDMARKS/RIGHT_HAND_LANDMARKS/"}


def _leer_csv(z: zipfile.ZipFile, ruta: str, puntos: int) -> np.ndarray:
    """Filas `índice, landmark_0_x, landmark_0_y, landmark_0_z, …` → array `(fotogramas, puntos, 3)`."""
    datos = np.loadtxt(io.TextIOWrapper(z.open(ruta), encoding="utf-8"), delimiter=",", skiprows=1, ndmin=2)[:, 1:]
    if datos.shape[1] != puntos * 3:
        raise ValueError(f"{ruta}: {datos.shape[1]} columnas, se esperaban {puntos * 3}")
    return datos.reshape(len(datos), puntos, 3)


def parte_activa(pose: np.ndarray, margen: int = MARGEN) -> tuple[int, int] | None:
    """Primer y último fotograma (incluidos) con alguna muñeca por encima de la cintura."""
    cintura = (pose[:, 11, 1] + pose[:, 12, 1] + pose[:, 23, 1] + pose[:, 24, 1]) / 4
    activos = np.where((pose[:, 15, 1] < cintura) | (pose[:, 16, 1] < cintura))[0]
    if len(activos) < 2:
        return None
    return max(0, activos[0] - margen), min(len(pose) - 1, activos[-1] + margen)


def _mano(mano: np.ndarray, hombros: float) -> np.ndarray | None:
    tamano = np.linalg.norm(mano[0, :2] - mano[9, :2])
    return mano.astype(np.float64) if hombros > 0 and tamano / hombros >= MANO_MINIMA else None


def muestra(pose: np.ndarray, izquierda: np.ndarray, derecha: np.ndarray, sena: str, persona: int, repeticion: int) -> M.Muestra | None:
    """Une los tres CSV de un clip en una muestra recortada a la seña; None si no hay seña."""
    n = min(len(pose), len(izquierda), len(derecha))
    pose, izquierda, derecha = pose[:n], izquierda[:n], derecha[:n]
    tramo = parte_activa(pose)
    if tramo is None:
        return None
    inicio, fin = tramo
    cantidad = fin - inicio + 1
    fotogramas = []
    for j, i in enumerate(range(inicio, fin + 1)):
        hombros = float(np.linalg.norm(pose[i, 11, :2] - pose[i, 12, :2]))
        fotogramas.append(Fotograma(
            timestamp_ms=round(j * DURACION_MS / (cantidad - 1)),
            mano_izquierda=_mano(izquierda[i], hombros),
            mano_derecha=_mano(derecha[i], hombros),
            cuerpo=pose[i][S.CUERPO].astype(np.float64),
            rostro=None,
            relacion_aspecto=ASPECTO,
        ))
    return M.Muestra(f"LSC50_v{persona}", sena, f"r{repeticion + 1}", FUENTE, fotogramas)


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--zip", type=Path, required=True, help="LANDMARKS.zip de LSC50")
    ap.add_argument("--salida", type=Path, required=True)
    ap.add_argument("--vocabulario", type=Path, required=True, help="Las señas se buscan por su código (0000…0049) en «carpetas»")
    args = ap.parse_args(argv)

    vocabulario = V.leer(args.vocabulario)
    guardadas, omitidas, existentes, sin_sena = Counter(), Counter(), 0, []
    with zipfile.ZipFile(args.zip) as z:
        clips = sorted(n[len(_CUERPO):] for n in z.namelist() if n.startswith(_CUERPO) and _NOMBRE.match(n[len(_CUERPO):]))
        if not clips:
            print(f"{args.zip} no tiene {_CUERPO}*.csv: ¿es el LANDMARKS.zip de LSC50?")
            return 1
        for nombre in clips:
            clase, persona, repeticion = _NOMBRE.match(nombre).groups()
            entrada = vocabulario.por_carpeta(clase)
            if entrada is None:
                omitidas[clase] += 1
                continue
            if (args.salida / M.nombre_archivo(FUENTE, f"LSC50_v{int(persona)}", entrada.id, f"r{int(repeticion) + 1}")).exists():
                existentes += 1
                continue
            m = muestra(
                _leer_csv(z, _CUERPO + nombre, S.PUNTOS_POSE),
                _leer_csv(z, _MANOS["izquierda"] + nombre, S.PUNTOS_POR_MANO),
                _leer_csv(z, _MANOS["derecha"] + nombre, S.PUNTOS_POR_MANO),
                entrada.id, int(persona), int(repeticion),
            )
            if m is None:
                sin_sena.append(nombre)
                continue
            M.guardar(m, args.salida)
            guardadas[m.sena] += 1

    print(f"LSC50 importado en {args.salida}: {sum(guardadas.values())} muestras nuevas de {len(guardadas)} señas"
          + (f" ({existentes} ya estaban)" if existentes else ""))
    if omitidas:
        print(f"Clases fuera del vocabulario (omitidas): {', '.join(sorted(omitidas))}")
    if sin_sena:
        print(f"Clips sin parte activa (omitidos): {', '.join(sin_sena)}")
    return 0 if guardadas or existentes else 1


if __name__ == "__main__":
    sys.exit(main())
