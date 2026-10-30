"""Evalúa el modelo con las personas de PRUEBA (que nunca vio) y lo calibra con las de validación.

Escribe en la carpeta del entrenamiento:
- `metricas.json`: exactitud, macro-F1, ECE, temperatura, umbral de energía, rechazo…
- `informe.md`: las mismas cifras, por seña, y la matriz de confusión, para revisarlas.

Todas las cifras salen de este script: no se deben escribir métricas a mano en ningún sitio.

Ejemplo:
    python evaluar.py --entrenamiento outputs/v1 --muestras data/puntos
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import numpy as np

from senas import metricas as Me
from senas.calibracion import temperatura_optima, umbral_energia
from senas.contrato import ETIQUETA_SIN_SENA


def calcular(logits_val, y_val, logits_prueba, y_prueba, etiquetas, cobertura=0.95) -> dict:
    """Toda la evaluación a partir de logits; separada de Keras para poder probarla."""
    t = temperatura_optima(logits_val, y_val)
    idx_sin = etiquetas.index(ETIQUETA_SIN_SENA) if ETIQUETA_SIN_SENA in etiquetas else None
    conocidas_val = logits_val if idx_sin is None else logits_val[y_val != idx_sin]
    umbral = umbral_energia(conocidas_val, t, cobertura) if len(conocidas_val) else None

    prob = Me.softmax(logits_prueba, t)
    pred = prob.argmax(axis=1)
    matriz = Me.matriz_confusion(y_prueba, pred, len(etiquetas))
    energias = Me.energia(logits_prueba, t)
    rechazada = energias > umbral if umbral is not None else np.zeros(len(pred), bool)
    if idx_sin is not None:
        rechazada |= pred == idx_sin

    resultado = {
        "muestras_prueba": int(len(y_prueba)),
        "exactitud": Me.exactitud(matriz),
        "macro_f1": Me.macro_f1(matriz),
        "ece_sin_calibrar": Me.ece(Me.softmax(logits_prueba), y_prueba),
        "ece": Me.ece(prob, y_prueba),
        "temperatura": t,
        "umbral_energia": umbral,
        "cobertura_objetivo": cobertura,
        "senas_conocidas_rechazadas": float(np.mean(rechazada[y_prueba != idx_sin])) if idx_sin is not None else float(np.mean(rechazada)),
    }
    if idx_sin is not None and np.any(y_prueba == idx_sin):
        resultado["rechazo_desconocidas"] = float(np.mean(rechazada[y_prueba == idx_sin]))
    resultado["por_sena"] = {etiquetas[i]: vars(c) for i, c in enumerate(Me.por_clase(matriz))}
    resultado["matriz_confusion"] = matriz.tolist()
    return resultado


def informe(r: dict, etiquetas: list[str], particion: dict) -> str:
    pct = lambda v: f"{v * 100:.1f} %"  # noqa: E731
    lineas = [
        "# Informe de evaluación", "",
        f"Personas de prueba (no vistas en el entrenamiento): {', '.join(particion['prueba'])}  ",
        f"Muestras de prueba: {r['muestras_prueba']}", "",
        "| Métrica | Valor |", "|---|---|",
        f"| Exactitud | {pct(r['exactitud'])} |",
        f"| F1 macro | {pct(r['macro_f1'])} |",
        f"| ECE sin calibrar → calibrado | {r['ece_sin_calibrar']:.3f} → {r['ece']:.3f} |",
        f"| Temperatura | {r['temperatura']:.3f} |",
        f"| Umbral de energía | {r['umbral_energia']:.3f} |" if r["umbral_energia"] is not None else "| Umbral de energía | — |",
        f"| Señas conocidas rechazadas | {pct(r['senas_conocidas_rechazadas'])} |",
    ]
    if "rechazo_desconocidas" in r:
        lineas.append(f"| Movimientos sin seña rechazados | {pct(r['rechazo_desconocidas'])} |")
    lineas += ["", "## Por seña", "", "| Seña | Precisión | Recall | F1 | Muestras |", "|---|---|---|---|---|"]
    for e, c in r["por_sena"].items():
        lineas.append(f"| {e} | {pct(c['precision'])} | {pct(c['recall'])} | {pct(c['f1'])} | {c['soporte']} |")
    lineas += ["", "## Matriz de confusión (filas: real · columnas: predicha)", "", "| | " + " | ".join(etiquetas) + " |",
               "|---" * (len(etiquetas) + 1) + "|"]
    for e, fila in zip(etiquetas, r["matriz_confusion"]):
        lineas.append(f"| **{e}** | " + " | ".join(str(v) for v in fila) + " |")
    return "\n".join(lineas) + "\n"


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrenamiento", type=Path, required=True, help="Carpeta creada por entrenar.py")
    ap.add_argument("--muestras", type=Path, required=True)
    ap.add_argument("--cobertura", type=float, default=0.95, help="Fracción de señas conocidas que deben pasar el umbral de energía")
    args = ap.parse_args(argv)

    import keras

    from senas import muestras as M
    from senas.conjunto import construir

    conf = json.loads((args.entrenamiento / "entrenamiento.json").read_text(encoding="utf-8"))
    etiquetas, particion = conf["etiquetas"], conf["particion"]
    modelo = keras.models.load_model(args.entrenamiento / "modelo.keras")
    muestras = [m for m in M.cargar_carpeta(args.muestras) if m.sena in etiquetas]

    def tensores(parte):
        sel = [m for m in muestras if m.persona in particion[parte]]
        return construir(sel, etiquetas, conf["fotogramas"], conf["duracion_ms"], usar_rostro=conf.get("usa_rostro", True))

    x_val, y_val = tensores("validacion")
    x_pr, y_pr = tensores("prueba")
    r = calcular(modelo.predict(x_val, verbose=0), y_val, modelo.predict(x_pr, verbose=0), y_pr, etiquetas, args.cobertura)

    (args.entrenamiento / "metricas.json").write_text(json.dumps(r, ensure_ascii=False, indent=2), encoding="utf-8")
    (args.entrenamiento / "informe.md").write_text(informe(r, etiquetas, particion), encoding="utf-8")
    print(f"Exactitud {r['exactitud'] * 100:.1f} % · F1 macro {r['macro_f1'] * 100:.1f} % (personas no vistas)")
    print(f"Informe en {args.entrenamiento / 'informe.md'}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
