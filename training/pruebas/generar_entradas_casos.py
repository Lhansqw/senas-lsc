"""Genera las ENTRADAS de los casos compartidos entre Python y Kotlin.

Los valores ESPERADOS los calcula Kotlin, que es la referencia (la app):
  1. python training/pruebas/generar_entradas_casos.py
  2. Ejecutar `GeneradorCasosCompartidos.main` (recognizer/api, fuentes de prueba): completa
     training/pruebas/casos_caracteristicas.json con las salidas de Kotlin.
  3. Las pruebas de ambos lados comparan contra ese archivo.

Solo hace falta regenerarlo si cambia el contrato (y entonces se sube su versión).
"""
import json
import random
from pathlib import Path

DESTINO = Path(__file__).with_name("casos_caracteristicas.json")


def punto(rnd: random.Random, cx: float, cy: float, dispersion: float) -> list[float]:
    return [round(cx + rnd.uniform(-dispersion, dispersion), 4), round(cy + rnd.uniform(-dispersion, dispersion), 4), round(rnd.uniform(-0.05, 0.05), 4)]


def mano(rnd: random.Random, cx: float, cy: float) -> list[list[float]]:
    return [punto(rnd, cx, cy, 0.06) for _ in range(21)]


def cuerpo(rnd: random.Random, desplazamiento: float = 0.0) -> list[list[float]]:
    base = [(0.5, 0.3), (0.4, 0.5), (0.6, 0.5), (0.35, 0.7), (0.65, 0.7), (0.3, 0.85), (0.7, 0.85)]
    return [punto(rnd, x + desplazamiento, y, 0.01) for x, y in base]


def rostro(rnd: random.Random) -> list[list[float]]:
    return [punto(rnd, 0.5, 0.28, 0.04) for _ in range(39)]


def caso(nombre: str, rnd: random.Random, tiempos: list[int], aspecto: float, con_cuerpo=True, con_rostro=True,
         izq_presente=lambda i: True, der_presente=lambda i: True, mano_degenerada=False) -> dict:
    fotogramas = []
    for i, t in enumerate(tiempos):
        izq = mano(rnd, 0.62 + 0.01 * i, 0.55 - 0.01 * i) if izq_presente(i) else None
        if mano_degenerada and izq is not None:
            izq = [izq[0]] * 21  # todos los puntos iguales: tamaño de mano 0
        fotogramas.append({
            "t": t,
            "aspecto": aspecto,
            "izquierda": izq,
            "derecha": mano(rnd, 0.38 - 0.008 * i, 0.6) if der_presente(i) else None,
            "cuerpo": cuerpo(rnd, 0.002 * i) if con_cuerpo else None,
            "rostro": rostro(rnd) if con_rostro else None,
        })
    return {"nombre": nombre, "duracion_ms": 1000, "instantes": 8, "fotogramas": fotogramas}


def main() -> None:
    rnd = random.Random(20261029)
    casos = [
        caso("completo_30fps_vertical", rnd, list(range(1000, 2001, 33)), 0.75),
        caso("sin_cuerpo_ni_rostro", rnd, list(range(5000, 6001, 50)), 1.0, con_cuerpo=False, con_rostro=False),
        caso("manos_intermitentes_tiempos_irregulares", rnd, [0, 40, 95, 180, 230, 410, 500, 620, 640, 830, 990, 1010], 0.75,
             izq_presente=lambda i: i % 3 != 1, der_presente=lambda i: i >= 4),
        caso("mano_degenerada_6fps", rnd, [3000, 3166, 3333, 3500, 3666, 3833, 4000], 0.5625, mano_degenerada=True),
    ]
    DESTINO.write_text(json.dumps({"version_caracteristicas": 1, "casos": casos}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"Entradas escritas en {DESTINO}")


if __name__ == "__main__":
    main()
