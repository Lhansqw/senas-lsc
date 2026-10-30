"""La evaluación se calcula solo a partir de logits, sin Keras."""
import numpy as np

import evaluar


def _logits(y, clases, separacion, rnd):
    l = rnd.normal(0, 1, (len(y), clases))
    l[np.arange(len(y)), y] += separacion
    return l


def test_calcular_con_clase_sin_sena():
    rnd = np.random.default_rng(3)
    etiquetas = ["sin_sena", "lsc.a", "lsc.b"]
    y_val = np.repeat([0, 1, 2], 40)
    y_pr = np.repeat([0, 1, 2], 30)
    r = evaluar.calcular(_logits(y_val, 3, 4.0, rnd), y_val, _logits(y_pr, 3, 4.0, rnd), y_pr, etiquetas)

    assert r["muestras_prueba"] == 90
    assert 0.8 < r["exactitud"] <= 1.0
    assert r["temperatura"] > 0
    assert r["umbral_energia"] is not None
    assert "rechazo_desconocidas" in r and r["rechazo_desconocidas"] > 0.7
    assert set(r["por_sena"]) == set(etiquetas)
    assert np.array(r["matriz_confusion"]).sum() == 90


def test_calcular_sin_clase_sin_sena_y_cobertura():
    rnd = np.random.default_rng(4)
    etiquetas = ["lsc.a", "lsc.b"]
    y = np.repeat([0, 1], 200)
    r = evaluar.calcular(_logits(y, 2, 3.0, rnd), y, _logits(y, 2, 3.0, rnd), y, etiquetas, cobertura=0.9)
    assert "rechazo_desconocidas" not in r
    # Con la misma distribución, el umbral deja pasar cerca del 90 % de las señas conocidas.
    assert abs(r["senas_conocidas_rechazadas"] - 0.10) < 0.06


def test_informe_contiene_las_cifras():
    rnd = np.random.default_rng(5)
    etiquetas = ["lsc.a", "lsc.b"]
    y = np.repeat([0, 1], 20)
    r = evaluar.calcular(_logits(y, 2, 5.0, rnd), y, _logits(y, 2, 5.0, rnd), y, etiquetas)
    texto = evaluar.informe(r, etiquetas, {"prueba": ["p07"]})
    assert "p07" in texto
    assert f"{r['exactitud'] * 100:.1f} %" in texto
    assert "| **lsc.b** |" in texto
