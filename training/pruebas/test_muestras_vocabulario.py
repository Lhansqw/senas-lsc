import json

import numpy as np
import pytest

from senas import muestras as M
from senas import vocabulario as V
from senas.fotograma import Fotograma


def _fotograma(t, con_mano=True):
    mano = np.full((21, 3), 0.5) if con_mano else None
    return Fotograma(t, mano, None, np.full((7, 3), 0.4), None, 0.75)


def test_guardar_y_cargar_conserva_todo(tmp_path):
    m = M.Muestra("p01", "lsc.hola", "r1", "LSC70", [_fotograma(0), _fotograma(200, con_mano=False), _fotograma(400)])
    ruta = M.guardar(m, tmp_path)
    leida = M.cargar(ruta)

    assert (leida.persona, leida.sena, leida.repeticion, leida.fuente) == ("p01", "lsc.hola", "r1", "LSC70")
    assert [f.timestamp_ms for f in leida.fotogramas] == [0, 200, 400]
    assert leida.fotogramas[1].mano_izquierda is None
    assert leida.fotogramas[0].rostro is None
    np.testing.assert_allclose(leida.fotogramas[0].mano_izquierda, 0.5)
    assert leida.fotogramas[0].relacion_aspecto == pytest.approx(0.75)
    assert m.proporcion_con_manos == pytest.approx(2 / 3)


def test_nombre_de_archivo_seguro():
    m = M.Muestra("persona 1/á", "lsc.hola", "r 1", "LSC70", [_fotograma(0)])
    assert "/" not in m.nombre_archivo and " " not in m.nombre_archivo
    assert m.nombre_archivo.endswith(".npz")


def test_muestra_invalida():
    with pytest.raises(ValueError):
        M.Muestra("p", "lsc.a", "r", "x", [])
    with pytest.raises(ValueError):
        M.Muestra("p", "lsc.a", "r", "x", [_fotograma(100), _fotograma(50)])


def test_carpeta_vacia(tmp_path):
    with pytest.raises(FileNotFoundError):
        M.cargar_carpeta(tmp_path)


def _vocabulario(tmp_path, senas, lengua="lsc"):
    ruta = tmp_path / "vocabulario.json"
    ruta.write_text(json.dumps({"lengua": lengua, "senas": senas}), encoding="utf-8")
    return ruta


def test_vocabulario_de_ejemplo_es_valido():
    from pathlib import Path
    v = V.leer(Path(__file__).resolve().parents[1] / "vocabulario.ejemplo.json")
    assert v.lengua == "lsc"
    assert v.por_carpeta("hola").id == "lsc.hola"     # sin distinguir mayúsculas
    assert v.por_id("lsc.como").interrogativa
    assert v.por_carpeta("Inexistente") is None
    assert "carpetas" not in v.por_id("lsc.hola").a_json()


@pytest.mark.parametrize("senas,mensaje", [
    ([{"id": "HOLA", "glosa": "H", "significado": "h", "tipo": "palabra"}], "id no válido"),
    ([{"id": "asl.hello", "glosa": "H", "significado": "h", "tipo": "palabra"}], "lengua"),
    ([{"id": "lsc.a", "glosa": "A", "significado": "a", "tipo": "verbo"}], "tipo"),
    ([{"id": "lsc.a", "glosa": "A", "significado": "a", "tipo": "letra"}] * 2, "repetido"),
    ([{"id": "lsc.a", "glosa": "A", "significado": "a", "tipo": "letra", "carpetas": ["X"]},
      {"id": "lsc.b", "glosa": "B", "significado": "b", "tipo": "letra", "carpetas": ["x"]}], "carpeta"),
    ([], "vacío"),
])
def test_vocabulario_invalido(tmp_path, senas, mensaje):
    with pytest.raises(ValueError, match=mensaje):
        V.leer(_vocabulario(tmp_path, senas))
