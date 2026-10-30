"""Densificación de las secuencias de pocas fotos (LSC70) a la frecuencia de la app."""
import numpy as np

from senas import caracteristicas as C
from senas.conjunto import preparar, ventana
from senas.fotograma import Fotograma
from senas.interpolacion import densificar, intervalo_mediano_ms, necesita_densificar

MANO = np.zeros((21, 3))
CUERPO = np.array([[0.5, 0.3, 0], [0.4, 0.5, 0], [0.6, 0.5, 0], [0.35, 0.7, 0], [0.65, 0.7, 0], [0.3, 0.85, 0], [0.7, 0.85, 0]])


def _seis_fotos(con_mano=lambda i: True):
    """Mano derecha que avanza en línea recta: 6 fotos en 1000 ms, como LSC70."""
    return [Fotograma(t, None, MANO + [0.4 + 0.05 * i, 0.5, 0] if con_mano(i) else None, CUERPO, None, 4 / 3)
            for i, t in enumerate([0, 200, 400, 600, 800, 1000])]


def test_detecta_cuando_hay_pocos_fotogramas():
    fotos = _seis_fotos()
    assert intervalo_mediano_ms(fotos) == 200
    assert necesita_densificar(fotos, 20)
    denso = densificar(fotos, 30)
    assert not necesita_densificar(denso, 20)


def test_densificar_a_30_fps_interpola_linealmente():
    denso = densificar(_seis_fotos(), 30)
    assert len(denso) == 31
    assert denso[0].timestamp_ms == 0 and denso[-1].timestamp_ms == 1000
    # A los 100 ms la mano está a medio camino entre la foto 0 (x=0,40) y la 1 (x=0,45).
    medio = min(denso, key=lambda f: abs(f.timestamp_ms - 100))
    esperado = 0.40 + 0.05 * medio.timestamp_ms / 200
    assert abs(medio.mano_derecha[0, 0] - esperado) < 1e-9
    np.testing.assert_allclose(denso[-1].mano_derecha, _seis_fotos()[-1].mano_derecha)


def test_parte_detectada_en_una_sola_foto_se_copia_de_la_mas_cercana():
    fotos = _seis_fotos(con_mano=lambda i: i != 3)
    denso = densificar(fotos, 30)
    for f in denso:
        if 500 < f.timestamp_ms < 600:   # más cerca de la foto 3 (600 ms), que no tiene mano
            assert f.mano_derecha is None
        if 400 < f.timestamp_ms < 500:   # más cerca de la foto 2, que sí la tiene
            assert f.mano_derecha is not None


def test_velocidades_sin_picos_tras_densificar():
    """Movimiento uniforme: la velocidad debe ser casi constante, como la mediría la app."""
    fotos = _seis_fotos()
    sin = ventana(fotos, 32, 1000)[1:, -2]     # velocidad x de la mano derecha
    con = ventana(preparar(fotos), 32, 1000)[1:, -2]
    assert np.count_nonzero(np.abs(sin) < 1e-6) > 20          # sin densificar: casi todo ceros…
    assert np.max(np.abs(sin)) > 3 * np.median(np.abs(con))  # …y picos
    # Densificado: velocidad casi constante. Como en la app a 30 fps, 32 instantes sobre 31
    # fotogramas repiten uno (velocidad 0 en ese instante): se admite uno así.
    movimiento = np.abs(con[np.abs(con) > 1e-6])
    assert len(movimiento) >= len(con) - 1
    assert movimiento.max() < 1.1 * np.median(movimiento)


def test_una_muestra_a_30_fps_no_se_toca():
    fotos = [Fotograma(t, None, MANO, CUERPO, None, 0.75) for t in range(0, 1001, 33)]
    assert preparar(fotos) is fotos
    assert ventana(preparar(fotos), 32, 1000).shape == (32, C.POR_FOTOGRAMA)
