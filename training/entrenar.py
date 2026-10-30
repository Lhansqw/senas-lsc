"""Entrena el reconocedor de señas con las muestras extraídas.

Separa las personas en entrenamiento, validación y prueba, entrena con aumento de datos y guarda
en `--salida`: el modelo Keras, las etiquetas, la partición y la configuración. La evaluación con
las personas de prueba la hace `evaluar.py` (no se mira la prueba mientras se entrena).

Ejemplo:
    python entrenar.py --muestras data/puntos --vocabulario vocabulario.json --salida outputs/v1
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path

from senas import muestras as M
from senas import vocabulario as V
from senas.contrato import ETIQUETA_SIN_SENA
from senas.particion import por_persona


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--muestras", type=Path, required=True)
    ap.add_argument("--vocabulario", type=Path, required=True)
    ap.add_argument("--salida", type=Path, required=True)
    ap.add_argument("--fotogramas", type=int, default=32)
    ap.add_argument("--duracion-ms", type=int, default=1000)
    ap.add_argument("--epocas", type=int, default=80)
    ap.add_argument("--lote", type=int, default=32)
    ap.add_argument("--copias-aumentadas", type=int, default=4)
    ap.add_argument("--semilla", type=int, default=42)
    ap.add_argument("--validacion", type=float, default=0.15)
    ap.add_argument("--prueba", type=float, default=0.15)
    ap.add_argument("--rostro", choices=["auto", "si", "no"], default="auto",
                    help="Usar los puntos del rostro. «auto»: solo si se detectó la cara en al menos el 50 %% de los fotogramas")
    args = ap.parse_args(argv)

    import keras
    import numpy as np

    from senas import modelo as Mo
    from senas.conjunto import construir, proporcion_con_rostro

    keras.utils.set_random_seed(args.semilla)
    vocabulario = V.leer(args.vocabulario)
    muestras = [m for m in M.cargar_carpeta(args.muestras) if vocabulario.por_id(m.sena) or m.sena == ETIQUETA_SIN_SENA]
    if not muestras:
        print("Ninguna muestra corresponde al vocabulario.")
        return 1

    particion = por_persona(muestras, args.validacion, args.prueba, args.semilla)
    senas = sorted({m.sena for m in muestras} - {ETIQUETA_SIN_SENA})
    etiquetas = ([ETIQUETA_SIN_SENA] if any(m.sena == ETIQUETA_SIN_SENA for m in muestras) else []) + senas

    def de(parte):
        return [m for m in muestras if particion.de(m.persona) == parte]

    entrenamiento, validacion = de("entrenamiento"), de("validacion")
    print(f"Personas: {len(particion.entrenamiento)} entrenamiento, {len(particion.validacion)} validación, {len(particion.prueba)} prueba")
    print(f"Muestras: {len(entrenamiento)} entrenamiento, {len(validacion)} validación, {len(de('prueba'))} prueba")
    print(f"Etiquetas ({len(etiquetas)}): {', '.join(etiquetas)}")
    con_rostro = proporcion_con_rostro(entrenamiento)
    usar_rostro = args.rostro == "si" or (args.rostro == "auto" and con_rostro >= 0.5)
    print(f"Rostro detectado en el {con_rostro * 100:.0f} % de los fotogramas → {'se usa' if usar_rostro else 'no se usa'}")

    x_ent, y_ent = construir(entrenamiento, etiquetas, args.fotogramas, args.duracion_ms, args.copias_aumentadas, args.semilla, usar_rostro)
    x_val, y_val = construir(validacion, etiquetas, args.fotogramas, args.duracion_ms, usar_rostro=usar_rostro)

    # Pesos por clase: que una seña con más muestras no domine el entrenamiento.
    conteo = Counter(y_ent.tolist())
    pesos = {c: len(y_ent) / (len(conteo) * n) for c, n in conteo.items()}

    modelo = Mo.construir(args.fotogramas, len(etiquetas))
    historial = modelo.fit(
        x_ent, y_ent,
        validation_data=(x_val, y_val),
        epochs=args.epocas,
        batch_size=args.lote,
        class_weight=pesos,
        callbacks=[keras.callbacks.EarlyStopping(monitor="val_loss", patience=12, restore_best_weights=True)],
        verbose=2,
    )

    args.salida.mkdir(parents=True, exist_ok=True)
    modelo.save(args.salida / "modelo.keras")
    configuracion = {
        "etiquetas": etiquetas,
        "fotogramas": args.fotogramas,
        "duracion_ms": args.duracion_ms,
        "usa_rostro": usar_rostro,
        "semilla": args.semilla,
        "particion": particion.a_json(),
        "fuentes": sorted({m.fuente for m in muestras}),
        "epocas_entrenadas": len(historial.history["loss"]),
        "mejor_val_loss": float(np.min(historial.history["val_loss"])),
    }
    (args.salida / "entrenamiento.json").write_text(json.dumps(configuracion, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"\nModelo guardado en {args.salida}. Siguiente paso: python evaluar.py --entrenamiento {args.salida} ...")
    return 0


if __name__ == "__main__":
    sys.exit(main())
