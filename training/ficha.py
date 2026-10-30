"""Genera la ficha del modelo (fase 12) a partir de lo que escribieron entrenar, evaluar y exportar.

Todas las cifras salen de `entrenamiento.json`, `metricas.json`, `modelo.json` y, si existe,
`optimizacion.json`. Las limitaciones se escriben según lo que el modelo realmente tiene (por
ejemplo, si no hay clase «sin seña» o si no usa el rostro).

Ejemplo:
    python ficha.py --entrenamiento outputs/lsc70-v0.1.0 --salida ../docs/ficha-modelo.md
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def _leer(carpeta: Path, nombre: str) -> dict | list | None:
    ruta = carpeta / nombre
    return json.loads(ruta.read_text(encoding="utf-8")) if ruta.is_file() else None


def generar(conf: dict, met: dict, contrato: dict, opt: list | None = None, kb_tflite: float | None = None) -> str:
    pct = lambda v: f"{v * 100:.1f} %"  # noqa: E731
    sal = contrato["salida"]
    car = contrato["caracteristicas"]
    glosas = {v["id"]: v["glosa"] for v in contrato["vocabulario"]}
    part = conf["particion"]
    l = [
        f"# Ficha del modelo: {contrato['nombre']} {contrato['version']}", "",
        "Generada por `training/ficha.py` a partir de los archivos del entrenamiento. No editar a mano.", "",
        "## Para qué sirve", "",
        f"Reconoce {len(glosas)} señas aisladas de la Lengua de Señas Colombiana ({contrato['lengua'].upper()}) a partir de los",
        "puntos de MediaPipe de manos y cuerpo, en ventanas de "
        f"{contrato['ventana']['duracion_ms']} ms. Lo usa la app Señas LSC para mostrar las señas como texto.", "",
        "**No sirve para** traducir conversaciones en LSC, reconocer señas fuera de su vocabulario ni tomar",
        "decisiones sobre personas. Siempre muestra sus resultados como propuesta revisable.", "",
        "## Datos", "",
    ]
    l += [f"- {d}" for d in contrato["datos_entrenamiento"]]
    l += [
        f"- Personas: {len(part['entrenamiento'])} de entrenamiento, {len(part['validacion'])} de validación y "
        f"{len(part['prueba'])} de prueba. Ninguna persona está en dos grupos.", "",
        "## Modelo", "",
        f"- Arquitectura: {conf.get('arquitectura', 'gru')}"
        + (f", {conf['parametros']:,} parámetros".replace(",", " ") if conf.get("parametros") else ""),
        f"- Entrada: {contrato['ventana']['fotogramas']} instantes × {car['por_fotograma']} características "
        f"(versión {car['version']}); salida: logits de {len(sal['etiquetas'])} clases.",
        f"- Usa el rostro: {'sí' if car.get('usa_rostro', True) else 'no'}.",
        f"- Calibración: temperatura {sal['temperatura']:.3f}"
        + (f", umbral de energía {sal['umbral_energia']:.3f}" if sal["umbral_energia"] is not None else ", sin umbral de energía") + ".",
    ]
    if kb_tflite is not None:
        l.append(f"- Tamaño del archivo `.tflite`: {kb_tflite:.0f} KB.")
    l += [
        "", "## Resultados con personas que el modelo no vio", "",
        f"Personas de prueba: {', '.join(part['prueba'])} ({met['muestras_prueba']} muestras).", "",
        "| Métrica | Valor |", "|---|---|",
        f"| Exactitud | {pct(met['exactitud'])} |",
        f"| F1 macro | {pct(met['macro_f1'])} |",
        f"| Error de calibración (ECE) | {met['ece']:.3f} |",
        f"| Señas conocidas rechazadas por el umbral | {pct(met['senas_conocidas_rechazadas'])} |",
    ]
    if "rechazo_desconocidas" in met:
        l.append(f"| Movimientos sin seña rechazados | {pct(met['rechazo_desconocidas'])} |")
    por = sorted(met["por_sena"].items(), key=lambda kv: kv[1]["f1"])
    l += ["", "### Señas con peor resultado", "", "| Seña | F1 | Recall | Muestras |", "|---|---|---|---|"]
    for ident, c in por[:5]:
        l.append(f"| {glosas.get(ident, ident)} | {pct(c['f1'])} | {pct(c['recall'])} | {c['soporte']} |")
    if opt:
        l += ["", "### Versiones cuantizadas", "", "| Versión | Tamaño | Exactitud |", "|---|---|---|"]
        l += [f"| {f['version']} | {f['kb']:.0f} KB | {pct(f['exactitud'])} |" for f in opt]

    l += ["", "## Limitaciones conocidas", ""]
    if sal["etiqueta_sin_sena"] is None:
        l.append("- **No tiene clase «sin seña».** Cualquier movimiento de las manos puede reconocerse como alguna seña; "
                 "solo el umbral de energía y el filtro temporal de la app lo frenan.")
    if not car.get("usa_rostro", True):
        l.append("- **No usa el rostro**, así que no distingue señas que solo cambian por la expresión facial o la boca.")
    if any("LSC70" in d for d in contrato["datos_entrenamiento"]):
        l += [
            "- Entrenado con **fotos** (6 por seña) interpoladas, no con vídeo: en la cámara en vivo puede rendir peor.",
            "- Las personas de LSC70 no son expertas en LSC; las señas deben validarse con una persona competente.",
        ]
    l += [
        f"- Con {len(part['prueba'])} personas de prueba, las cifras tienen un margen de varios puntos.",
        "- No se ha medido por separado en personas zurdas, con poca luz ni con fondos complejos.",
        "", "## Licencia", "",
        "Los datos de entrenamiento se citan arriba con su licencia; el modelo se distribuye dentro de la app con esa",
        "misma atribución (pantalla «Acerca de»).",
    ]
    return "\n".join(l) + "\n"


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--entrenamiento", type=Path, required=True)
    ap.add_argument("--salida", type=Path, required=True)
    args = ap.parse_args(argv)
    conf, met, contrato = (_leer(args.entrenamiento, n) for n in ("entrenamiento.json", "metricas.json", "modelo.json"))
    if conf is None or met is None or contrato is None:
        print("Faltan archivos: ejecuta entrenar.py, evaluar.py y exportar.py antes.")
        return 1
    tflite = args.entrenamiento / "reconocedor_lsc.tflite"
    texto = generar(conf, met, contrato, _leer(args.entrenamiento, "optimizacion.json"),
                    tflite.stat().st_size / 1024 if tflite.is_file() else None)
    args.salida.parent.mkdir(parents=True, exist_ok=True)
    args.salida.write_text(texto, encoding="utf-8")
    print(f"Ficha escrita en {args.salida}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
