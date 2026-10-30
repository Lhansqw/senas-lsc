"""Extrae los puntos de MediaPipe de un conjunto de datos y los guarda como muestras `.npz`.

Estructura esperada (como LSC70): `<raiz>/<persona>/<seña>/` con, dentro, o bien las imágenes de
una repetición (JPG/PNG), o bien subcarpetas con imágenes, o bien vídeos (cada vídeo es una
repetición). Con `--orden sena-persona` se aceptan carpetas `<raiz>/<seña>/<persona>/`.

Solo se procesan las señas que aparecen en `vocabulario.json`; el resto se informa y se omite.

Ejemplo:
    python extraer.py --raiz data/LSC70/LSC70W --vocabulario vocabulario.json \
        --salida data/puntos --fuente LSC70W
"""
from __future__ import annotations

import argparse
import sys
import time
import urllib.request
from collections import Counter
from pathlib import Path

from senas import muestras as M
from senas import vocabulario as V
from senas.extraccion import fotograma_desde_resultado, ordenar_imagenes, tiempos_uniformes

URL_MODELO = "https://storage.googleapis.com/mediapipe-models/holistic_landmarker/holistic_landmarker/float16/1/holistic_landmarker.task"
IMAGENES = {".jpg", ".jpeg", ".png", ".bmp"}
VIDEOS = {".mp4", ".mov", ".avi", ".mkv", ".webm"}
# El mismo archivo que descarga la compilación de la app (fase 5): así se usan exactamente los mismos puntos.
MODELO_APP = Path(__file__).resolve().parents[1] / "vision" / "landmarks" / "src" / "main" / "assets" / "holistic_landmarker.task"
MODELO_LOCAL = Path(__file__).resolve().parent / "modelos" / "holistic_landmarker.task"


def ubicar_modelo(indicado: Path | None) -> Path:
    """Usa el modelo indicado; si no, el de la app; si tampoco está, lo descarga en `modelos/`."""
    if indicado is not None:
        return descargar_modelo(indicado)
    if MODELO_APP.is_file() and MODELO_APP.stat().st_size > 1_000_000:
        return MODELO_APP
    return descargar_modelo(MODELO_LOCAL)


def descargar_modelo(destino: Path) -> Path:
    """Descarga el mismo modelo Holistic que usa la app, si no está."""
    if destino.is_file() and destino.stat().st_size > 1_000_000:
        return destino
    destino.parent.mkdir(parents=True, exist_ok=True)
    temporal = destino.with_suffix(".descargando")
    print(f"Descargando {URL_MODELO}")
    urllib.request.urlretrieve(URL_MODELO, temporal)
    if temporal.stat().st_size < 1_000_000 or temporal.read_bytes()[:2] != b"PK":
        temporal.unlink()
        raise RuntimeError("El archivo descargado no es un modelo .task válido")
    temporal.replace(destino)
    return destino


def crear_landmarker(modelo: Path, video: bool):
    from mediapipe.tasks.python.core.base_options import BaseOptions
    from mediapipe.tasks.python.vision import holistic_landmarker as h
    from mediapipe.tasks.python.vision.core.vision_task_running_mode import VisionTaskRunningMode

    opciones = h.HolisticLandmarkerOptions(
        base_options=BaseOptions(model_asset_path=str(modelo)),
        running_mode=VisionTaskRunningMode.VIDEO if video else VisionTaskRunningMode.IMAGE,
        min_face_detection_confidence=0.5,
        min_pose_detection_confidence=0.5,
        min_hand_landmarks_confidence=0.5,
    )
    return h.HolisticLandmarker.create_from_options(opciones)


def _mp_image(ruta_o_array, espejo: bool):
    import cv2
    import mediapipe as mp

    bgr = cv2.imread(str(ruta_o_array)) if isinstance(ruta_o_array, Path) else ruta_o_array
    if bgr is None:
        raise ValueError(f"No se pudo leer la imagen {ruta_o_array}")
    if espejo:
        bgr = cv2.flip(bgr, 1)
    rgb = cv2.cvtColor(bgr, cv2.COLOR_BGR2RGB)
    return mp.Image(image_format=mp.ImageFormat.SRGB, data=rgb), rgb.shape[1], rgb.shape[0]


def extraer_imagenes(landmarker, imagenes: list[Path], duracion_ms: int, espejo: bool):
    """Fotos sueltas: cada una se analiza por separado (modo IMAGE), con el mismo landmarker."""
    tiempos = tiempos_uniformes(len(imagenes), duracion_ms)
    fotogramas = []
    for ruta, t in zip(imagenes, tiempos):
        imagen, ancho, alto = _mp_image(ruta, espejo)
        fotogramas.append(fotograma_desde_resultado(landmarker.detect(imagen), t, ancho, alto))
    return fotogramas


def extraer_video(modelo: Path, ruta: Path, espejo: bool):
    import cv2

    captura = cv2.VideoCapture(str(ruta))
    fps = captura.get(cv2.CAP_PROP_FPS) or 30.0
    fotogramas, i = [], 0
    with crear_landmarker(modelo, video=True) as lm:
        while True:
            ok, bgr = captura.read()
            if not ok:
                break
            t = round(i * 1000 / fps)
            imagen, ancho, alto = _mp_image(bgr, espejo)
            fotogramas.append(fotograma_desde_resultado(lm.detect_for_video(imagen, t), t, ancho, alto))
            i += 1
    captura.release()
    return fotogramas


def repeticiones(carpeta_sena: Path):
    """Devuelve (nombre de repetición, lista de imágenes | ruta de vídeo)."""
    archivos = [p for p in carpeta_sena.iterdir() if p.is_file()]
    imagenes = [p for p in archivos if p.suffix.lower() in IMAGENES]
    if imagenes:
        orden = ordenar_imagenes(p.name for p in imagenes)
        yield "r1", [carpeta_sena / n for n in orden]
    for v in sorted(p for p in archivos if p.suffix.lower() in VIDEOS):
        yield v.stem, v
    for sub in sorted(p for p in carpeta_sena.iterdir() if p.is_dir()):
        imgs = [p for p in sub.iterdir() if p.is_file() and p.suffix.lower() in IMAGENES]
        if imgs:
            yield sub.name, [sub / n for n in ordenar_imagenes(p.name for p in imgs)]


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--raiz", type=Path, required=True)
    ap.add_argument("--vocabulario", type=Path, required=True)
    ap.add_argument("--salida", type=Path, required=True)
    ap.add_argument("--fuente", required=True, help="Nombre del conjunto de datos (se guarda en cada muestra)")
    ap.add_argument("--orden", choices=["persona-sena", "sena-persona"], default="persona-sena")
    ap.add_argument("--duracion-ms", type=int, default=1000, help="Duración asignada a una secuencia de imágenes sin tiempos")
    ap.add_argument("--espejo", action="store_true", help="Voltear horizontalmente (si el conjunto está grabado en espejo)")
    ap.add_argument("--modelo", type=Path, default=None, help="Por defecto, el de la app (vision/landmarks/src/main/assets)")
    ap.add_argument("--min-manos", type=float, default=0.5, help="Descartar muestras con menos fotogramas con manos")
    ap.add_argument("--rehacer", action="store_true", help="Volver a extraer las muestras que ya existen en --salida")
    args = ap.parse_args(argv)

    vocabulario = V.leer(args.vocabulario)
    modelo = ubicar_modelo(args.modelo)
    print(f"Modelo de MediaPipe: {modelo}")
    guardadas, existentes, omitidas, descartadas = Counter(), 0, Counter(), 0

    tareas = []
    for nivel1 in sorted(p for p in args.raiz.iterdir() if p.is_dir()):
        for nivel2 in sorted(p for p in nivel1.iterdir() if p.is_dir()):
            persona, carpeta = (nivel1.name, nivel2.name) if args.orden == "persona-sena" else (nivel2.name, nivel1.name)
            entrada = vocabulario.por_carpeta(carpeta)
            if entrada is None:
                omitidas[carpeta] += 1
                continue
            tareas.append((persona, entrada, nivel2))

    inicio = time.monotonic()
    with crear_landmarker(modelo, video=False) as landmarker:
        for n, (persona, entrada, carpeta) in enumerate(tareas, 1):
            for rep, contenido in repeticiones(carpeta):
                destino = args.salida / M.nombre_archivo(args.fuente, persona, entrada.id, rep)
                if destino.is_file() and not args.rehacer:
                    existentes += 1
                    continue
                fotogramas = (extraer_video(modelo, contenido, args.espejo) if isinstance(contenido, Path)
                              else extraer_imagenes(landmarker, contenido, args.duracion_ms, args.espejo))
                if not fotogramas:
                    continue
                muestra = M.Muestra(persona, entrada.id, rep, args.fuente, fotogramas)
                if muestra.proporcion_con_manos < args.min_manos:
                    descartadas += 1
                    continue
                M.guardar(muestra, args.salida)
                guardadas[entrada.id] += 1
            if n % 25 == 0 or n == len(tareas):
                transcurrido = time.monotonic() - inicio
                restante = transcurrido / n * (len(tareas) - n)
                print(f"  {n}/{len(tareas)} carpetas · {transcurrido / 60:.1f} min · quedan ~{restante / 60:.0f} min", flush=True)

    print(f"\nMuestras guardadas en {args.salida}: {sum(guardadas.values())}")
    if existentes:
        print(f"Ya extraídas antes (se dejaron como estaban): {existentes}")
    for ident, n in sorted(guardadas.items()):
        print(f"  {ident}: {n}")
    if descartadas:
        print(f"Descartadas por tener pocas manos detectadas: {descartadas}")
    if omitidas:
        print(f"Carpetas sin entrada en el vocabulario (omitidas): {', '.join(sorted(omitidas))}")
    return 0 if guardadas or existentes else 1


if __name__ == "__main__":
    sys.exit(main())
