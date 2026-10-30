"""Las arquitecturas devuelven logits con la forma del contrato y el .tflite reproduce a Keras."""
import numpy as np
import pytest

from senas import caracteristicas as C
from senas import modelo as Mo

X = np.random.default_rng(0).normal(size=(3, 16, C.POR_FOTOGRAMA)).astype(np.float32)


@pytest.fixture(scope="module", params=Mo.ARQUITECTURAS)
def modelo(request):
    return Mo.construir(fotogramas=16, clases=3, filtros=16, unidades_gru=8, arquitectura=request.param)


def test_forma_de_entrada_y_salida(modelo):
    assert modelo.input_shape == (None, 16, C.POR_FOTOGRAMA)
    assert modelo.output_shape == (None, 3)


def test_la_ultima_capa_no_tiene_activacion(modelo):
    # El contrato exige logits: la app aplica la temperatura y el softmax.
    assert modelo.get_layer("logits").activation.__name__ == "linear"


def test_tflite_reproduce_a_keras(modelo, tmp_path):
    contenido = Mo.a_tflite(modelo, 16)
    np.testing.assert_allclose(Mo.ejecutar_tflite(contenido, X), modelo.predict(X, verbose=0), atol=1e-4)

    from ai_edge_litert.interpreter import Interpreter
    interprete = Interpreter(model_content=contenido)
    assert list(interprete.get_input_details()[0]["shape"]) == [1, 16, C.POR_FOTOGRAMA]
    assert list(interprete.get_output_details()[0]["shape"]) == [1, 3]
    assert Mo.guardar_tflite(contenido, tmp_path / "x" / "m.tflite").stat().st_size == len(contenido)


def test_se_guarda_y_se_recarga(modelo, tmp_path):
    import keras
    modelo.save(tmp_path / "m.keras")
    recargado = keras.models.load_model(tmp_path / "m.keras")
    np.testing.assert_allclose(recargado.predict(X, verbose=0), modelo.predict(X, verbose=0), atol=1e-5)


def test_solo_la_gru_necesita_bucles():
    """La GRU se convierte en WHILE (solo CPU); las otras dos no, y pueden ir a la GPU."""
    from ai_edge_litert.interpreter import Interpreter
    for arquitectura in Mo.ARQUITECTURAS:
        m = Mo.construir(16, 3, filtros=16, unidades_gru=8, arquitectura=arquitectura)
        interprete = Interpreter(model_content=Mo.a_tflite(m, 16))
        ops = {o["op_name"] for o in interprete._get_ops_details()}
        assert ("WHILE" in ops) == (arquitectura == "gru"), (arquitectura, ops)


def test_cuantizar_reduce_el_tamano_y_conserva_la_entrada_float32():
    m = Mo.construir(16, 3, filtros=32, arquitectura="conv")
    tamanos = {q: len(Mo.a_tflite(m, 16, q)) for q in Mo.CUANTIZACIONES}
    assert tamanos["int8"] < tamanos["float16"] < tamanos["ninguna"]
    assert tamanos["float16"] < 0.6 * tamanos["ninguna"]
    from ai_edge_litert.interpreter import Interpreter
    for q in ("float16", "int8"):
        interprete = Interpreter(model_content=Mo.a_tflite(m, 16, q))
        assert interprete.get_input_details()[0]["dtype"] == np.float32   # el contrato con la app no cambia
        assert interprete.get_output_details()[0]["dtype"] == np.float32
    salida = Mo.ejecutar_tflite(Mo.a_tflite(m, 16, "float16"), X)
    np.testing.assert_allclose(salida, m.predict(X, verbose=0), atol=5e-3)


def test_opciones_desconocidas():
    with pytest.raises(ValueError, match="Arquitectura"):
        Mo.construir(16, 3, arquitectura="lstm")
    with pytest.raises(ValueError, match="Cuantización"):
        Mo.a_tflite(Mo.construir(16, 3, filtros=8, arquitectura="conv"), 16, "int4")
