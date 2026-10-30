"""Informe de optimización y comprobación de paridad del .tflite cuantizado."""
import numpy as np

import exportar
import optimizar

CONF = {"arquitectura": "gru", "parametros": 1000, "particion": {"prueba": ["p07"]}}


def _fila(version, kb, exactitud):
    return {"version": version, "kb": kb, "exactitud": exactitud, "macro_f1": exactitud,
            "misma_sena_que_float32": 1.0, "latencia_p50_ms": 1.0, "latencia_p95_ms": 1.5}


def test_recomienda_la_mas_pequena_que_no_pierde_mas_de_un_punto():
    texto = optimizar.informe([_fila("float32", 1000, 0.90), _fila("float16", 500, 0.899), _fila("int8", 270, 0.895)], CONF)
    assert "Recomendación: **int8**" in texto and "--cuantizacion int8" in texto
    texto = optimizar.informe([_fila("float32", 1000, 0.90), _fila("float16", 500, 0.899), _fila("int8", 270, 0.85)], CONF)
    assert "Recomendación: **float16**" in texto
    texto = optimizar.informe([_fila("float32", 1000, 0.90), _fila("float16", 500, 0.80), _fila("int8", 270, 0.70)], CONF)
    assert "mantener la versión sin cuantizar" in texto
    assert "| float16 | 500 KB | 80.0 % |" in texto


def test_paridad_estricta_sin_cuantizar_y_por_sena_si_esta_cuantizado():
    keras = np.array([[3.0, 1.0], [0.0, 2.0]] * 10)
    assert exportar.comprobar_paridad(keras, keras + 1e-5, "ninguna")[0]
    assert not exportar.comprobar_paridad(keras, keras + 0.05, "ninguna")[0]
    assert exportar.comprobar_paridad(keras, keras + 0.05, "int8")[0]          # misma seña siempre
    cambiada = keras.copy()
    cambiada[0] = [0.0, 5.0]
    cambiada[2] = [0.0, 5.0]                                                  # 2 de 20 cambian de seña
    valido, detalle = exportar.comprobar_paridad(keras, cambiada, "int8")
    assert not valido and "90 %" in detalle


def test_latencias_devuelve_una_por_repeticion():
    from senas import modelo as Mo
    m = Mo.construir(8, 2, filtros=8, arquitectura="conv")
    x = np.zeros((2, 8, 352), np.float32)
    t = optimizar.latencias_ms(Mo.a_tflite(m, 8), x, repeticiones=15, calentamiento=2)
    assert len(t) == 15 and (t > 0).all()
