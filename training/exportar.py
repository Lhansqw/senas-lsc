"""Exporta el modelo evaluado a LiteRT y escribe su `modelo.json`.

1. Convierte a `reconocedor_lsc.tflite` (entrada `[1, fotogramas, 352]`, salida en logits).
2. Comprueba con el intérprete de LiteRT que da los mismos logits que Keras.
3. Escribe `modelo.json` con el vocabulario, la ventana, la calibración y las métricas medidas por
   `evaluar.py` (nunca cifras escritas a mano).
4. Con `--instalar`, copia ambos archivos a `recognizer/litert/src/main/assets/` para la app.

Ejemplo:
    python exportar.py --entrenamiento outputs/v1 --muestras data/puntos --vocabulario vocabulario.json \
        --nombre reconocedor-lsc --version 0.1.0 --instalar
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from pathlib import Path

import numpy as np

from senas import contrato as Co
from senas import vocabulario as V

ASSETS_APP = Path(__file__).resolve().parents[1] / "recognizer" / "litert" / "src" / "main" / "assets"
TOLERANCIA = 1e-3


def metricas_para_contrato(m: dict) -> dict[str, float]:
    """Solo las métricas que la app sabe mostrar."""
    claves = ["exactitud", "macro_f1", "rechazo_desconocidas"]
    return {k: m[k] for k in claves if k in m}


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrenamiento", type=Path, required=True)
    ap.add_argument("--muestras", type=Path, required=True, help="Para comprobar el .tflite con muestras reales")
    ap.add_argument("--vocabulario", type=Path, required=True)
    ap.add_argument("--nombre", default="reconocedor-lsc")
    ap.add_argument("--version", required=True)
    ap.add_argument("--instalar", action="store_true", help="Copiar el modelo a los assets de la app")
    args = ap.parse_args(argv)

    import keras

    from senas import modelo as Mo
    from senas import muestras as M
    from senas.conjunto import construir

    conf = json.loads((args.entrenamiento / "entrenamiento.json").read_text(encoding="utf-8"))
    ruta_metricas = args.entrenamiento / "metricas.json"
    if not ruta_metricas.is_file():
        print("Falta metricas.json: ejecuta primero evaluar.py.")
        return 1
    metricas = json.loads(ruta_metricas.read_text(encoding="utf-8"))
    etiquetas = conf["etiquetas"]

    modelo = keras.models.load_model(args.entrenamiento / "modelo.keras")
    contenido = Mo.a_tflite(modelo, conf["fotogramas"])

    muestras = [m for m in M.cargar_carpeta(args.muestras) if m.sena in etiquetas][:32]
    x, _ = construir(muestras, etiquetas, conf["fotogramas"], conf["duracion_ms"], usar_rostro=conf.get("usa_rostro", True))
    diferencia = float(np.abs(modelo.predict(x, verbose=0) - Mo.ejecutar_tflite(contenido, x)).max())
    if diferencia > TOLERANCIA:
        print(f"El .tflite no reproduce al modelo Keras (diferencia {diferencia:.2e}).")
        return 1

    contrato = Co.construir(
        nombre=args.nombre,
        version=args.version,
        vocabulario=V.leer(args.vocabulario),
        etiquetas=etiquetas,
        fotogramas=conf["fotogramas"],
        duracion_ms=conf["duracion_ms"],
        temperatura=metricas["temperatura"],
        umbral_energia=metricas["umbral_energia"],
        datos_entrenamiento=conf["fuentes"],
        metricas=metricas_para_contrato(metricas),
        usa_rostro=conf.get("usa_rostro", True),
    )
    ruta_tflite = Mo.guardar_tflite(contenido, args.entrenamiento / "reconocedor_lsc.tflite")
    ruta_json = Co.guardar(contrato, args.entrenamiento / "modelo.json")
    print(f"Exportado: {ruta_tflite} ({len(contenido) // 1024} KB) y {ruta_json}. Diferencia con Keras: {diferencia:.2e}")

    if args.instalar:
        ASSETS_APP.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ruta_tflite, ASSETS_APP / ruta_tflite.name)
        shutil.copy2(ruta_json, ASSETS_APP / ruta_json.name)
        print(f"Instalado en {ASSETS_APP}. Recompila la app (gradlew.bat installDebug).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
