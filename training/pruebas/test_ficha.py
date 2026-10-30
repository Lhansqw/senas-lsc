"""La ficha del modelo usa solo las cifras de los archivos y adapta las limitaciones."""
import json

import ficha
import generar_modelo_compartido as G

CONF = {"arquitectura": "gru", "parametros": 283626,
        "particion": {"entrenamiento": ["p1", "p2", "p3"], "validacion": ["p4"], "prueba": ["p5", "p6"]}}
MET = {"muestras_prueba": 40, "exactitud": 0.9071, "macro_f1": 0.9058, "ece": 0.029, "senas_conocidas_rechazadas": 0.014,
       "por_sena": {"lsc.a": {"precision": 1, "recall": 0.9, "f1": 0.947, "soporte": 10},
                    "lsc.hola": {"precision": 1, "recall": 1, "f1": 1.0, "soporte": 10},
                    "lsc.como": {"precision": 0.5, "recall": 0.4, "f1": 0.444, "soporte": 10}}}


def _contrato(**cambios):
    c = G.construir()
    c["datos_entrenamiento"] = ["LSC70 (Universidad del Cauca, 2024), CC BY 4.0"]
    c.update(cambios)
    return c


def test_cifras_y_peores_senas():
    texto = ficha.generar(CONF, MET, _contrato(), kb_tflite=1129.4)
    assert "| Exactitud | 90.7 % |" in texto and "| F1 macro | 90.6 % |" in texto
    assert "283 626 parámetros" in texto
    assert "Personas de prueba: p5, p6 (40 muestras)" in texto
    assert "3 de entrenamiento, 1 de validación y 2 de prueba" in texto
    filas = [l for l in texto.split("### Señas con peor resultado")[1].splitlines() if l.startswith("| ") and "%" in l]
    assert filas[0].startswith("| CÓMO |")          # la peor primero, con su glosa
    assert "1129 KB" in texto
    assert "CC BY 4.0" in texto


def test_limitaciones_segun_el_modelo():
    con_sin_sena = ficha.generar(CONF, MET, _contrato())
    assert "No tiene clase «sin seña»" not in con_sin_sena            # el contrato compartido sí la tiene
    assert "No usa el rostro" in con_sin_sena                          # y no usa el rostro
    assert "fotos" in con_sin_sena                                     # datos de LSC70

    c = _contrato()
    c["salida"] = dict(c["salida"], etiqueta_sin_sena=None, etiquetas=[e for e in c["salida"]["etiquetas"] if e != "sin_sena"])
    c["caracteristicas"] = dict(c["caracteristicas"], usa_rostro=True)
    texto = ficha.generar(CONF, MET, c)
    assert "No tiene clase «sin seña»" in texto and "No usa el rostro" not in texto


def test_main_falla_sin_archivos_y_escribe_con_ellos(tmp_path):
    assert ficha.main(["--entrenamiento", str(tmp_path), "--salida", str(tmp_path / "f.md")]) == 1
    for nombre, datos in (("entrenamiento.json", CONF), ("metricas.json", MET), ("modelo.json", _contrato())):
        (tmp_path / nombre).write_text(json.dumps(datos, ensure_ascii=False), encoding="utf-8")
    assert ficha.main(["--entrenamiento", str(tmp_path), "--salida", str(tmp_path / "docs" / "f.md")]) == 0
    assert (tmp_path / "docs" / "f.md").read_text(encoding="utf-8").startswith("# Ficha del modelo: contrato-compartido")
