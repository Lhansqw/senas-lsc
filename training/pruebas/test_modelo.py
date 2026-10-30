"""La arquitectura devuelve logits con la forma del contrato y el .tflite reproduce a Keras."""
import numpy as np
import pytest

from senas import caracteristicas as C
from senas import modelo as Mo


@pytest.fixture(scope="module")
def modelo():
    return Mo.construir(fotogramas=16, clases=3, filtros=16, unidades_gru=8)


def test_forma_de_entrada_y_salida(modelo):
    assert modelo.input_shape == (None, 16, C.POR_FOTOGRAMA)
    assert modelo.output_shape == (None, 3)


def test_la_ultima_capa_no_tiene_activacion(modelo):
    # El contrato exige logits: la app aplica la temperatura y el softmax.
    assert modelo.get_layer("logits").activation.__name__ == "linear"


def test_tflite_reproduce_a_keras(modelo, tmp_path):
    contenido = Mo.a_tflite(modelo, 16)
    x = np.random.default_rng(0).normal(size=(3, 16, C.POR_FOTOGRAMA)).astype(np.float32)
    esperado = modelo.predict(x, verbose=0)
    obtenido = Mo.ejecutar_tflite(contenido, x)
    np.testing.assert_allclose(obtenido, esperado, atol=1e-4)

    from ai_edge_litert.interpreter import Interpreter
    interprete = Interpreter(model_content=contenido)
    assert list(interprete.get_input_details()[0]["shape"]) == [1, 16, C.POR_FOTOGRAMA]
    assert list(interprete.get_output_details()[0]["shape"]) == [1, 3]
    assert Mo.guardar_tflite(contenido, tmp_path / "x" / "m.tflite").stat().st_size == len(contenido)
