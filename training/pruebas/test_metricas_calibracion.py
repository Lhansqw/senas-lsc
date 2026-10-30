import numpy as np
import pytest

from senas import metricas as M
from senas.calibracion import temperatura_optima, umbral_energia


def test_matriz_y_metricas_calculadas_a_mano():
    verdad = np.array([0, 0, 0, 1, 1, 2])
    pred = np.array([0, 0, 1, 1, 1, 0])
    m = M.matriz_confusion(verdad, pred, 3)
    np.testing.assert_array_equal(m, [[2, 1, 0], [0, 2, 0], [1, 0, 0]])

    c = M.por_clase(m)
    # Clase 0: precisión 2/3, recall 2/3. Clase 1: precisión 2/3, recall 1. Clase 2: 0.
    assert c[0].precision == pytest.approx(2 / 3) and c[0].recall == pytest.approx(2 / 3)
    assert c[1].precision == pytest.approx(2 / 3) and c[1].recall == pytest.approx(1.0)
    assert c[1].f1 == pytest.approx(0.8)
    assert c[2].f1 == 0.0 and c[2].soporte == 1
    assert M.exactitud(m) == pytest.approx(4 / 6)
    assert M.macro_f1(m) == pytest.approx((2 / 3 + 0.8 + 0) / 3)


def test_macro_f1_ignora_clases_sin_ejemplos():
    m = M.matriz_confusion(np.array([0, 1]), np.array([0, 1]), 3)
    assert M.macro_f1(m) == 1.0
    assert M.macro_f1(m, solo_con_soporte=False) == pytest.approx(2 / 3)


def test_softmax_y_energia_coinciden_con_la_app():
    logits = np.array([[0.5, 0.5, 0.5, 0.5], [0.0, 8.0, 0.0, 0.0]])
    p = M.softmax(logits)
    np.testing.assert_allclose(p.sum(axis=1), 1.0)
    # Mismos valores que PostProcesadorTest (Kotlin): −(0,5 + ln 4).
    assert M.energia(logits)[0] == pytest.approx(-(0.5 + np.log(4)))
    assert M.energia(logits)[1] < -7.9


def test_ece_perfecta_y_sobreconfiada():
    verdad = np.array([0, 1, 0, 1])
    perfecta = np.array([[1.0, 0.0], [0.0, 1.0], [1.0, 0.0], [0.0, 1.0]])
    assert M.ece(perfecta, verdad) == pytest.approx(0.0)
    # Siempre 90 % de confianza pero solo acierta la mitad: error 0,4.
    sobre = np.array([[0.9, 0.1]] * 4)
    assert M.ece(sobre, verdad) == pytest.approx(0.4)


def test_la_temperatura_corrige_un_modelo_sobreconfiado():
    rnd = np.random.default_rng(0)
    verdad = rnd.integers(0, 3, 600)
    # Logits muy grandes con un 30 % de errores: el modelo está demasiado seguro.
    pred = np.where(rnd.random(600) < 0.7, verdad, (verdad + 1) % 3)
    logits = np.full((600, 3), 0.0)
    logits[np.arange(600), pred] = 10.0

    t = temperatura_optima(logits, verdad)
    assert t > 2.0
    assert M.ece(M.softmax(logits, t), verdad) < M.ece(M.softmax(logits), verdad)


def test_umbral_de_energia_conserva_la_cobertura_pedida():
    rnd = np.random.default_rng(1)
    logits = rnd.normal(0, 1, (1000, 5))
    logits[:, 0] += 6
    u = umbral_energia(logits, 1.0, cobertura=0.95)
    assert np.mean(M.energia(logits) <= u) == pytest.approx(0.95, abs=0.01)
    with pytest.raises(ValueError):
        umbral_energia(logits, 1.0, cobertura=1.0)
