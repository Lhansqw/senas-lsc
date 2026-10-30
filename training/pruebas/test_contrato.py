"""modelo.json se escribe con las mismas reglas que valida la app."""
import json

import pytest

from senas import contrato as Co
from senas.vocabulario import EntradaVocabulario, Vocabulario

VOC = Vocabulario("lsc", (
    EntradaVocabulario("lsc.hola", "HOLA", "hola", "palabra", ("Hola",)),
    EntradaVocabulario("lsc.como", "CÓMO", "cómo", "palabra", ("Como",), interrogativa=True),
    EntradaVocabulario("lsc.a", "A", "a", "letra", ("A",)),
))


def _contrato(**cambios):
    args = dict(nombre="prueba", version="0.0.1", vocabulario=VOC, etiquetas=["sin_sena", "lsc.como", "lsc.hola"],
                fotogramas=32, duracion_ms=1000, temperatura=1.4, umbral_energia=-3.2,
                datos_entrenamiento=["LSC70"], metricas={"exactitud": 0.9})
    args.update(cambios)
    return Co.construir(**args)


def test_contrato_valido():
    c = _contrato()
    assert c["formato"] == 1
    assert c["caracteristicas"]["por_fotograma"] == 352
    assert c["salida"]["etiqueta_sin_sena"] == "sin_sena"
    # Solo las señas que produce el modelo, sin las carpetas del conjunto de datos.
    assert [v["id"] for v in c["vocabulario"]] == ["lsc.como", "lsc.hola"]
    assert c["vocabulario"][0]["interrogativa"] is True
    assert "carpetas" not in c["vocabulario"][1]


def test_sin_clase_sin_sena_ni_umbral():
    c = _contrato(etiquetas=["lsc.a"], umbral_energia=None)
    assert c["salida"]["etiqueta_sin_sena"] is None
    assert c["salida"]["umbral_energia"] is None


@pytest.mark.parametrize("cambio, mensaje", [
    ({"etiquetas": ["lsc.hola", "lsc.adios"]}, "sin entrada de vocabulario"),
    ({"temperatura": 0.0}, "temperatura"),
    ({"fotogramas": 1}, "fotogramas"),
    ({"duracion_ms": 50}, "duración"),
    ({"etiquetas": ["sin_sena"]}, "ninguna seña"),
])
def test_rechaza_contratos_invalidos(cambio, mensaje):
    with pytest.raises(ValueError, match=mensaje):
        _contrato(**cambio)


def test_guardar_es_json_utf8(tmp_path):
    ruta = Co.guardar(_contrato(), tmp_path / "a" / "modelo.json")
    texto = ruta.read_text(encoding="utf-8")
    assert "CÓMO" in texto
    assert json.loads(texto)["nombre"] == "prueba"
