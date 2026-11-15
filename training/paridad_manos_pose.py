"""Compara el modelo de señas con puntos de Holistic frente a Hand + Pose Landmarker.

El modelo se entrena con MediaPipe Holistic (`extraer.py`). iOS, y la app de Android si deja de usar
Holistic para ir más rápido, combinan Hand Landmarker y Pose Landmarker. Este script vuelve a extraer
las muestras de prueba de LSC70 con Hand + Pose, asigna cada mano a la muñeca más cercana de la pose
(igual que `ConstructorFotograma.kt`) y ejecuta el `.tflite` con los dos juegos de puntos, sobre las
mismas muestras.

Ejemplo:
    python paridad_manos_pose.py --entrenamiento outputs/lsc-v0.3.0 --vocabulario vocabulario.lsc.json
"""
from __future__ import annotations

import argparse
import sys
import time
import urllib.request
from pathlib import Path
from types import SimpleNamespace

import numpy as np

from extraer import _mp_image, repeticiones
from senas import muestras as M
from senas import vocabulario as V
from senas.extraccion import fotograma_desde_resultado, tiempos_uniformes

MODELOS = Path(__file__).resolve().parent / "modelos"
URL_MANOS = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
URL_POSE = "https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_full/float16/1/pose_landmarker_full.task"
ASSETS_APP = Path(__file__).resolve().parents[1] / "recognizer" / "litert" / "src" / "main" / "assets"
FUENTES_LSC70 = ("LSC70AN", "LSC70W")

# Índices de las muñecas en los 33 puntos de la pose (izquierda y derecha de la persona).
MUNECA_IZQUIERDA, MUNECA_DERECHA = 15, 16


def descargar(url: str) -> Path:
    destino = MODELOS / url.rsplit("/", 1)[1]
    if not destino.is_file():
        MODELOS.mkdir(parents=True, exist_ok=True)
        print(f"Descargando {url}")
        urllib.request.urlretrieve(url, destino)
    return destino


def crear_landmarkers(confianza: float = 0.5):
    from mediapipe.tasks.python.core.base_options import BaseOptions
    from mediapipe.tasks.python.vision import hand_landmarker as h
    from mediapipe.tasks.python.vision import pose_landmarker as p

    manos = h.HandLandmarker.create_from_options(h.HandLandmarkerOptions(
        base_options=BaseOptions(model_asset_path=str(descargar(URL_MANOS))),
        num_hands=2,
        min_hand_detection_confidence=confianza,
        min_hand_presence_confidence=confianza,
        min_tracking_confidence=confianza,
    ))
    pose = p.PoseLandmarker.create_from_options(p.PoseLandmarkerOptions(
        base_options=BaseOptions(model_asset_path=str(descargar(URL_POSE))),
        num_poses=1,
        min_pose_detection_confidence=confianza,
        min_pose_presence_confidence=confianza,
        min_tracking_confidence=confianza,
        output_segmentation_masks=False,
    ))
    return manos, pose


def _distancia2(a, b) -> float:
    return (a.x - b.x) ** 2 + (a.y - b.y) ** 2


def asignar_manos(manos: list, etiquetas: list[str], pose: list):
    """Devuelve (izquierda, derecha) de la persona, como `ConstructorFotograma.asignarManos`."""
    manos, etiquetas = manos[:2], etiquetas[:2]
    if not manos:
        return None, None
    if len(pose) < 33:
        # MediaPipe etiqueta suponiendo espejo: con la imagen sin espejo, «Right» es la izquierda.
        izquierda = next((m for m, e in zip(manos, etiquetas) if e == "Right"), None)
        derecha = next((m for m, e in zip(manos, etiquetas) if e != "Right"), None)
        if len(manos) == 2 and (izquierda is None or derecha is None):
            izquierda, derecha = (manos[0], manos[1]) if etiquetas[0] == "Right" else (manos[1], manos[0])
        return izquierda, derecha
    mi, md = pose[MUNECA_IZQUIERDA], pose[MUNECA_DERECHA]
    if len(manos) == 1:
        m = manos[0]
        return (m, None) if _distancia2(m[0], mi) <= _distancia2(m[0], md) else (None, m)
    a, b = manos
    directa = _distancia2(a[0], mi) + _distancia2(b[0], md)
    cruzada = _distancia2(b[0], mi) + _distancia2(a[0], md)
    return (a, b) if directa <= cruzada else (b, a)


def extraer_manos_pose(manos_lm, pose_lm, imagenes: list[Path], duracion_ms: int):
    fotogramas = []
    for ruta, t in zip(imagenes, tiempos_uniformes(len(imagenes), duracion_ms)):
        imagen, ancho, alto = _mp_image(ruta, espejo=False)
        rm = manos_lm.detect(imagen)
        rp = pose_lm.detect(imagen)
        pose = rp.pose_landmarks[0] if rp.pose_landmarks else []
        etiquetas = [h[0].category_name if h else "" for h in rm.handedness]
        izquierda, derecha = asignar_manos(list(rm.hand_landmarks), etiquetas, pose)
        resultado = SimpleNamespace(left_hand_landmarks=izquierda, right_hand_landmarks=derecha,
                                    pose_landmarks=pose, face_landmarks=[])
        fotogramas.append(fotograma_desde_resultado(resultado, t, ancho, alto))
    return fotogramas


def main(argv=None) -> int:
    import json

    from senas import modelo as Mo
    from senas.conjunto import construir

    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrenamiento", type=Path, required=True, help="Carpeta con entrenamiento.json")
    ap.add_argument("--vocabulario", type=Path, required=True)
    ap.add_argument("--datos", type=Path, default=Path("data/LSC70"))
    ap.add_argument("--puntos", type=Path, default=Path("data/puntos"), help="Muestras ya extraídas con Holistic")
    ap.add_argument("--modelo", type=Path, default=ASSETS_APP / "reconocedor_lsc.tflite")
    ap.add_argument("--limite", type=int, default=0, help="Procesar solo las primeras N muestras (0 = todas)")
    args = ap.parse_args(argv)

    conf = json.loads((args.entrenamiento / "entrenamiento.json").read_text(encoding="utf-8"))
    etiquetas, prueba = conf["etiquetas"], set(conf["particion"]["prueba"])
    fotogramas_modelo, duracion = conf["fotogramas"], conf["duracion_ms"]
    usar_rostro = conf.get("usa_rostro", True)
    vocabulario = V.leer(args.vocabulario)

    holistic, manos_pose = [], []
    manos_lm, pose_lm = crear_landmarkers()
    inicio = time.monotonic()
    for fuente in FUENTES_LSC70:
        raiz = args.datos / fuente
        if not raiz.is_dir():
            continue
        for carpeta_persona in sorted(p for p in raiz.iterdir() if p.is_dir() and p.name in prueba):
            for carpeta_sena in sorted(p for p in carpeta_persona.iterdir() if p.is_dir()):
                entrada = vocabulario.por_carpeta(carpeta_sena.name)
                if entrada is None or entrada.id not in etiquetas:
                    continue
                for rep, contenido in repeticiones(carpeta_sena):
                    ruta_h = args.puntos / M.nombre_archivo(fuente, carpeta_persona.name, entrada.id, rep)
                    if isinstance(contenido, Path) or not ruta_h.is_file():
                        continue  # solo fotos con su muestra de Holistic (las descartadas no cuentan)
                    holistic.append(M.cargar(ruta_h))
                    fotos = extraer_manos_pose(manos_lm, pose_lm, contenido, duracion)
                    manos_pose.append(M.Muestra(carpeta_persona.name, entrada.id, rep, fuente, fotos))
                    if args.limite and len(holistic) >= args.limite:
                        break
                if args.limite and len(holistic) >= args.limite:
                    break
    manos_lm.close()
    pose_lm.close()
    if not holistic:
        print("No se encontró ninguna muestra de prueba de LSC70 con su versión de Holistic.")
        return 1
    print(f"{len(holistic)} muestras de prueba extraídas con Hand + Pose en {(time.monotonic() - inicio) / 60:.1f} min")

    contenido = args.modelo.read_bytes()
    resultados = {}
    for nombre, muestras in (("Holistic", holistic), ("Hand + Pose", manos_pose)):
        x, y = construir(muestras, etiquetas, fotogramas_modelo, duracion, usar_rostro=usar_rostro)
        pred = Mo.ejecutar_tflite(contenido, x).argmax(axis=1)
        manos = np.mean([f.hay_manos for m in muestras for f in m.fotogramas])
        dos = np.mean([f.mano_izquierda is not None and f.mano_derecha is not None for m in muestras for f in m.fotogramas])
        resultados[nombre] = (pred, y)
        print(f"{nombre:12s}  exactitud {np.mean(pred == y):6.1%}  fotogramas con manos {manos:6.1%}  con dos manos {dos:6.1%}")

    (ph, y), (pm, _) = resultados["Holistic"], resultados["Hand + Pose"]
    print(f"Las dos eligen la misma seña en el {np.mean(ph == pm):.1%} de las muestras")
    return 0


if __name__ == "__main__":
    sys.exit(main())
