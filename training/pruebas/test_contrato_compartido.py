"""Python calcula exactamente lo mismo que la app (casos generados por Kotlin)."""
import json
from pathlib import Path

import numpy as np
import pytest

from senas import caracteristicas as C
from senas.fotograma import Fotograma
from senas.remuestreo import a_tiempo_uniforme

CASOS = json.loads(Path(__file__).with_name("casos_caracteristicas.json").read_text(encoding="utf-8"))


def _arr(valor):
    return None if valor is None else np.array(valor, dtype=np.float64)


def _fotogramas(caso):
    return [
        Fotograma(f["t"], _arr(f["izquierda"]), _arr(f["derecha"]), _arr(f["cuerpo"]), _arr(f["rostro"]), f["aspecto"])
        for f in caso["fotogramas"]
    ]


def test_version_de_los_casos():
    assert CASOS["version_caracteristicas"] == C.VERSION == 1
    assert C.POR_FOTOGRAMA == 352


@pytest.mark.parametrize("caso", CASOS["casos"], ids=lambda c: c["nombre"])
def test_reproduce_las_salidas_de_kotlin(caso):
    assert "esperado_caracteristicas" in caso, "Faltan las salidas de Kotlin: ejecuta GeneradorCasosCompartidos"
    ventana = a_tiempo_uniforme(_fotogramas(caso), caso["instantes"], caso["duracion_ms"])
    assert [f.timestamp_ms for f in ventana] == caso["esperado_timestamps"]

    obtenido = C.de_ventana(ventana)
    esperado = np.array(caso["esperado_caracteristicas"], dtype=np.float64)
    assert obtenido.shape == esperado.shape
    np.testing.assert_allclose(obtenido, esperado, rtol=1e-5, atol=1e-5)
