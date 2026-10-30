"""Reúne en una tabla los resultados de varios entrenamientos (fase 11: GRU frente a Transformer…).

Lee `entrenamiento.json`, `metricas.json` y, si existe, `optimizacion.json` de cada carpeta y
escribe `comparacion.md`. Todas las cifras salen de esos archivos.

Ejemplo:
    python comparar.py outputs/comparacion/gru outputs/comparacion/conv outputs/comparacion/transformer \
        --salida outputs/comparacion/comparacion.md
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def leer(carpeta: Path) -> dict:
    conf = json.loads((carpeta / "entrenamiento.json").read_text(encoding="utf-8"))
    met = json.loads((carpeta / "metricas.json").read_text(encoding="utf-8"))
    ruta_opt = carpeta / "optimizacion.json"
    opt = json.loads(ruta_opt.read_text(encoding="utf-8")) if ruta_opt.is_file() else []
    float32 = next((f for f in opt if f["version"] == "float32"), None)
    return {
        "carpeta": carpeta.name,
        "arquitectura": conf.get("arquitectura", "gru"),
        "parametros": conf.get("parametros"),
        "prueba": tuple(conf["particion"]["prueba"]),
        "exactitud": met["exactitud"],
        "macro_f1": met["macro_f1"],
        "ece": met["ece"],
        "kb": float32["kb"] if float32 else None,
        "p95": float32["latencia_p95_ms"] if float32 else None,
    }


def tabla(filas: list[dict]) -> str:
    lineas = [
        "# Comparación de arquitecturas", "",
        "| Arquitectura | Parámetros | Exactitud | F1 macro | ECE | Tamaño (float32) | Latencia p95 (PC, 1 hilo) |",
        "|---|---|---|---|---|---|---|",
    ]
    for f in sorted(filas, key=lambda f: -f["macro_f1"]):
        kb = f"{f['kb']:.0f} KB" if f["kb"] is not None else "—"
        p95 = f"{f['p95']:.2f} ms" if f["p95"] is not None else "—"
        lineas.append(f"| {f['arquitectura']} | {f['parametros'] or '—'} | {f['exactitud'] * 100:.1f} % | "
                      f"{f['macro_f1'] * 100:.1f} % | {f['ece']:.3f} | {kb} | {p95} |")
    lineas.append("")
    if len({f["prueba"] for f in filas}) == 1:
        lineas.append(f"Todas se evaluaron con las mismas personas de prueba: {', '.join(filas[0]['prueba'])}.")
    else:
        lineas.append("**Atención:** no todas se evaluaron con las mismas personas de prueba; la comparación no es justa.")
    mejor = max(filas, key=lambda f: f["macro_f1"])
    lineas.append(f"Mejor F1 macro: **{mejor['arquitectura']}**. Diferencias de 1–2 puntos con 10 personas de prueba "
                  "están dentro del ruido: a igualdad, elegir la más rápida.")
    return "\n".join(lineas) + "\n"


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("carpetas", type=Path, nargs="+")
    ap.add_argument("--salida", type=Path, required=True)
    args = ap.parse_args(argv)
    filas = [leer(c) for c in args.carpetas]
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    args.salida.write_text(tabla(filas), encoding="utf-8")
    print(tabla(filas))
    return 0


if __name__ == "__main__":
    sys.exit(main())
