"""Entrenar → evaluar → exportar con datos SINTÉTICOS: comprueba que las piezas encajan.

El modelo resultante solo sirve para esta prueba: los datos no son señas de LSC.
"""
import json

import pytest

import entrenar
import evaluar
import exportar
import optimizar
import sinteticos
from senas import contrato as Co
from senas import muestras as M


@pytest.fixture(scope="module")
def carpeta(tmp_path_factory):
    raiz = tmp_path_factory.mktemp("flujo")
    for m in sinteticos.conjunto(clases=3, personas=8, repeticiones=2):
        M.guardar(m, raiz / "puntos")
    voc = {"lengua": "lsc", "senas": [
        {"id": f"lsc.sintetica{c}", "glosa": f"S{c}", "significado": f"s{c}", "tipo": "palabra"} for c in range(3)]}
    (raiz / "vocabulario.json").write_text(json.dumps(voc), encoding="utf-8")
    return raiz


def test_flujo_completo(carpeta, monkeypatch):
    puntos, voc, salida = carpeta / "puntos", carpeta / "vocabulario.json", carpeta / "salida"
    assert entrenar.main(["--muestras", str(puntos), "--vocabulario", str(voc), "--salida", str(salida),
                          "--fotogramas", "16", "--epocas", "15", "--copias-aumentadas", "1", "--validacion", "0.25", "--prueba", "0.25"]) == 0
    conf = json.loads((salida / "entrenamiento.json").read_text(encoding="utf-8"))
    assert conf["fuentes"] == ["SINTETICO"]
    assert conf["usa_rostro"] is False   # las muestras sintéticas no tienen cara, como LSC70
    assert not set(conf["particion"]["prueba"]) & set(conf["particion"]["entrenamiento"])

    assert evaluar.main(["--entrenamiento", str(salida), "--muestras", str(puntos)]) == 0
    metricas = json.loads((salida / "metricas.json").read_text(encoding="utf-8"))
    assert metricas["exactitud"] > 0.6   # movimientos muy distintos entre sí
    assert (salida / "informe.md").is_file()

    assert optimizar.main(["--entrenamiento", str(salida), "--muestras", str(puntos), "--repeticiones", "20"]) == 0
    filas = json.loads((salida / "optimizacion.json").read_text(encoding="utf-8"))
    assert [f["version"] for f in filas] == ["float32", "float16", "int8"]
    assert filas[0]["exactitud"] == pytest.approx(metricas["exactitud"], abs=0.02)   # el .tflite rinde como Keras
    assert filas[2]["kb"] < filas[1]["kb"] < filas[0]["kb"]
    assert (salida / "optimizacion.md").is_file()

    # Nunca se instala un modelo sintético en la app.
    monkeypatch.setattr(exportar, "ASSETS_APP", carpeta / "no_usar")
    assert exportar.main(["--entrenamiento", str(salida), "--muestras", str(puntos), "--vocabulario", str(voc),
                          "--nombre", "prueba-sintetica", "--version", "0.0.0", "--datos", "Datos sintéticos (solo pruebas)"]) == 0
    contrato = json.loads((salida / "modelo.json").read_text(encoding="utf-8"))
    assert Co.validar(contrato) == []
    assert contrato["ventana"]["fotogramas"] == 16
    assert contrato["caracteristicas"]["usa_rostro"] is False
    assert contrato["datos_entrenamiento"] == ["Datos sintéticos (solo pruebas)"]
    assert contrato["metricas"]["exactitud"] == pytest.approx(metricas["exactitud"])
    assert contrato["salida"]["temperatura"] == pytest.approx(metricas["temperatura"])
    assert (salida / "reconocedor_lsc.tflite").stat().st_size > 10_000
    assert not (carpeta / "no_usar").exists()

    # Exportar cuantizado: archivo más pequeño, mismo contrato.
    assert exportar.main(["--entrenamiento", str(salida), "--muestras", str(puntos), "--vocabulario", str(voc),
                          "--version", "0.0.0", "--cuantizacion", "float16"]) == 0
    assert (salida / "reconocedor_lsc.tflite").stat().st_size < 0.6 * filas[0]["kb"] * 1024


def test_exportar_sin_evaluar_falla(tmp_path):
    (tmp_path / "entrenamiento.json").write_text(json.dumps({"etiquetas": []}), encoding="utf-8")
    assert exportar.main(["--entrenamiento", str(tmp_path), "--muestras", str(tmp_path),
                          "--vocabulario", str(tmp_path / "v.json"), "--version", "0"]) == 1
