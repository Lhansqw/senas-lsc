import numpy as np
import pytest

from senas import caracteristicas as C
from senas.aumento import aumentar
from senas.conjunto import construir
from senas.particion import por_persona

import sinteticos


def test_particion_sin_personas_compartidas_y_reproducible():
    muestras = sinteticos.conjunto(personas=10)
    p = por_persona(muestras, semilla=1)

    grupos = [set(p.entrenamiento), set(p.validacion), set(p.prueba)]
    assert all(grupos)
    assert not (grupos[0] & grupos[1]) and not (grupos[0] & grupos[2]) and not (grupos[1] & grupos[2])
    assert set().union(*grupos) == {m.persona for m in muestras}
    assert p == por_persona(muestras, semilla=1)
    assert p.de(p.prueba[0]) == "prueba"


def test_particion_exige_personas_y_senas_en_entrenamiento():
    with pytest.raises(ValueError, match="3 personas"):
        por_persona(sinteticos.conjunto(personas=2))

    muestras = sinteticos.conjunto(personas=6)
    p = por_persona(muestras, semilla=3)
    # Una seña que solo hace una persona de prueba no puede entrenarse.
    solo_prueba = [m for m in muestras if m.persona == p.prueba[0]][:1]
    rara = [type(m)(m.persona, "lsc.rara", m.repeticion, m.fuente, m.fotogramas) for m in solo_prueba]
    with pytest.raises(ValueError, match="lsc.rara"):
        por_persona(muestras + rara, semilla=3)


def test_aumento_cambia_los_puntos_pero_conserva_la_estructura():
    m = sinteticos.conjunto(clases=1, personas=1, repeticiones=1)[0]
    rnd = np.random.default_rng(0)
    a = aumentar(m.fotogramas, rnd)

    assert len(a) == len(m.fotogramas)
    assert a[0].mano_izquierda is None and a[0].mano_derecha is not None
    assert not np.allclose(a[5].mano_derecha, m.fotogramas[5].mano_derecha)
    assert all(b.timestamp_ms >= x.timestamp_ms for x, b in zip(a, a[1:]))
    # El original no se modifica.
    assert m.fotogramas[5].mano_derecha is not a[5].mano_derecha


def test_aumento_sin_variacion_es_la_identidad():
    m = sinteticos.conjunto(clases=1, personas=1, repeticiones=1)[0]
    a = aumentar(m.fotogramas, np.random.default_rng(0), rotacion_grados=0, escala=0, desplazamiento=0, ruido=0, velocidad=0)
    for x, y in zip(m.fotogramas, a):
        np.testing.assert_allclose(x.mano_derecha, y.mano_derecha, atol=1e-12)
        assert x.timestamp_ms == y.timestamp_ms


def test_construir_da_tensores_con_la_forma_del_contrato():
    muestras = sinteticos.conjunto(clases=2, personas=3, repeticiones=1)
    etiquetas = ["sin_sena", "lsc.sintetica0", "lsc.sintetica1"]
    X, y = construir(muestras, etiquetas, fotogramas=16, duracion_ms=1000, copias_aumentadas=2, semilla=1)

    assert X.shape == (len(muestras) * 3, 16, C.POR_FOTOGRAMA)
    assert X.dtype == np.float32
    assert set(y.tolist()) == {1, 2}
    assert np.isfinite(X).all()

    with pytest.raises(ValueError, match="no está entre las etiquetas"):
        construir(muestras, ["lsc.sintetica0"], 16, 1000)
