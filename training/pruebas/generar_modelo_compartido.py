"""Genera `modelo_generado.json`: un modelo.json escrito por Python que la app debe aceptar.

`ContratoGeneradoTest.kt` lo lee con `LectorContrato`; `test_modelo_compartido.py` comprueba que el
archivo guardado sigue siendo lo que produce `contrato.construir`. Si cambia, regenerarlo con:
    python pruebas/generar_modelo_compartido.py
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from senas import contrato as Co  # noqa: E402
from senas import vocabulario as V  # noqa: E402

RUTA = Path(__file__).resolve().parent / "modelo_generado.json"
VOCABULARIO = Path(__file__).resolve().parents[1] / "vocabulario.ejemplo.json"


def construir() -> dict:
    return Co.construir(
        nombre="contrato-compartido", version="0.0.0", vocabulario=V.leer(VOCABULARIO),
        etiquetas=["sin_sena", "lsc.a", "lsc.como", "lsc.gracias", "lsc.hola", "lsc.n1"],
        fotogramas=32, duracion_ms=1000, temperatura=1.25, umbral_energia=-2.5,
        datos_entrenamiento=["SOLO_PRUEBA"], metricas={"exactitud": 0.5, "macro_f1": 0.5},
    )


if __name__ == "__main__":
    Co.guardar(construir(), RUTA)
    print(f"Escrito {RUTA}")
