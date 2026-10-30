"""Compara el modelo sin cuantizar con sus versiones float16 e int8 (fase 11).

Para cada versión convierte a `.tflite` y mide, con las personas de PRUEBA:
- exactitud y F1 macro ejecutando el propio `.tflite` (no el modelo Keras);
- en cuántas muestras elige la misma seña que la versión sin cuantizar;
- tamaño del archivo;
- latencia por ventana en este ordenador con 1 hilo (p50 y p95). Sirve para comparar versiones
  entre sí; la latencia real se mide en el teléfono (pantalla de la cámara, ver doc. fase 11).

Escribe `optimizacion.json` y `optimizacion.md` en la carpeta del entrenamiento.

Ejemplo:
    python optimizar.py --entrenamiento outputs/lsc70-v0.1.0 --muestras data/puntos
"""
from __future__ import annotations

import argparse
import json
import sys
import time
from pathlib import Path

import numpy as np

from senas import metricas as Me


def latencias_ms(contenido: bytes, x: np.ndarray, repeticiones: int = 200, calentamiento: int = 20) -> np.ndarray:
    """Tiempo de cada `invoke` con una ventana, un hilo, como lo hace la app."""
    from ai_edge_litert.interpreter import Interpreter

    interprete = Interpreter(model_content=contenido, num_threads=1)
    interprete.allocate_tensors()
    entrada = interprete.get_input_details()[0]["index"]
    tiempos = []
    for i in range(calentamiento + repeticiones):
        interprete.set_tensor(entrada, x[i % len(x)][None])
        inicio = time.perf_counter()
        interprete.invoke()
        if i >= calentamiento:
            tiempos.append((time.perf_counter() - inicio) * 1000)
    return np.array(tiempos)


def resumir(nombre: str, contenido: bytes, logits: np.ndarray, y: np.ndarray, referencia: np.ndarray | None,
            tiempos: np.ndarray, clases: int) -> dict:
    matriz = Me.matriz_confusion(y, logits.argmax(1), clases)
    return {
        "version": nombre,
        "kb": round(len(contenido) / 1024, 1),
        "exactitud": Me.exactitud(matriz),
        "macro_f1": Me.macro_f1(matriz),
        "misma_sena_que_float32": 1.0 if referencia is None else float(np.mean(logits.argmax(1) == referencia.argmax(1))),
        "latencia_p50_ms": float(np.percentile(tiempos, 50)),
        "latencia_p95_ms": float(np.percentile(tiempos, 95)),
    }


def informe(filas: list[dict], conf: dict) -> str:
    lineas = [
        "# Optimización del modelo", "",
        f"Arquitectura: {conf.get('arquitectura', 'gru')} · {conf.get('parametros', '?')} parámetros · "
        f"personas de prueba: {', '.join(conf['particion']['prueba'])}", "",
        "La latencia es de este ordenador con 1 hilo: sirve para comparar las versiones entre sí, no para",
        "saber cuánto tarda en el teléfono.", "",
        "| Versión | Tamaño | Exactitud | F1 macro | Misma seña que float32 | Latencia p50 | Latencia p95 |",
        "|---|---|---|---|---|---|---|",
    ]
    for f in filas:
        lineas.append(f"| {f['version']} | {f['kb']:.0f} KB | {f['exactitud'] * 100:.1f} % | {f['macro_f1'] * 100:.1f} % | "
                      f"{f['misma_sena_que_float32'] * 100:.1f} % | {f['latencia_p50_ms']:.2f} ms | {f['latencia_p95_ms']:.2f} ms |")
    base = filas[0]
    candidatas = [f for f in filas[1:] if base["exactitud"] - f["exactitud"] <= 0.01]
    lineas.append("")
    if candidatas:
        mejor = min(candidatas, key=lambda f: f["kb"])
        lineas.append(f"Recomendación: **{mejor['version']}** (pierde como mucho 1 punto de exactitud y ocupa "
                      f"{mejor['kb'] / base['kb'] * 100:.0f} % de la versión sin cuantizar). "
                      f"Exportar con `--cuantizacion {mejor['version']}`.")
    else:
        lineas.append("Recomendación: mantener la versión sin cuantizar (las demás pierden más de 1 punto de exactitud).")
    return "\n".join(lineas) + "\n"


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrenamiento", type=Path, required=True)
    ap.add_argument("--muestras", type=Path, required=True)
    ap.add_argument("--repeticiones", type=int, default=200, help="Ejecuciones para medir la latencia")
    args = ap.parse_args(argv)

    import keras

    from senas import modelo as Mo
    from senas import muestras as M
    from senas.conjunto import construir

    conf = json.loads((args.entrenamiento / "entrenamiento.json").read_text(encoding="utf-8"))
    etiquetas = conf["etiquetas"]
    prueba = [m for m in M.cargar_carpeta(args.muestras) if m.sena in etiquetas and m.persona in conf["particion"]["prueba"]]
    if not prueba:
        print("No hay muestras de las personas de prueba en --muestras.")
        return 1
    x, y = construir(prueba, etiquetas, conf["fotogramas"], conf["duracion_ms"], usar_rostro=conf.get("usa_rostro", True))
    modelo = keras.models.load_model(args.entrenamiento / "modelo.keras")

    filas, referencia = [], None
    for cuantizacion in Mo.CUANTIZACIONES:
        contenido = Mo.a_tflite(modelo, conf["fotogramas"], cuantizacion)
        logits = Mo.ejecutar_tflite(contenido, x)
        nombre = "float32" if cuantizacion == "ninguna" else cuantizacion
        filas.append(resumir(nombre, contenido, logits, y, referencia, latencias_ms(contenido, x, args.repeticiones), len(etiquetas)))
        if referencia is None:
            referencia = logits
        f = filas[-1]
        print(f"{nombre:8} {f['kb']:7.0f} KB · exactitud {f['exactitud'] * 100:5.1f} % · p95 {f['latencia_p95_ms']:.2f} ms")

    (args.entrenamiento / "optimizacion.json").write_text(json.dumps(filas, ensure_ascii=False, indent=2), encoding="utf-8")
    (args.entrenamiento / "optimizacion.md").write_text(informe(filas, conf), encoding="utf-8")
    print(f"Informe en {args.entrenamiento / 'optimizacion.md'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
