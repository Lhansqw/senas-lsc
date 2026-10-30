import json

import comparar


def _carpeta(raiz, arquitectura, f1, prueba=("p07", "p08")):
    c = raiz / arquitectura
    c.mkdir()
    (c / "entrenamiento.json").write_text(json.dumps({"arquitectura": arquitectura, "parametros": 1234,
                                                      "particion": {"prueba": list(prueba)}}), encoding="utf-8")
    (c / "metricas.json").write_text(json.dumps({"exactitud": f1, "macro_f1": f1, "ece": 0.03}), encoding="utf-8")
    (c / "optimizacion.json").write_text(json.dumps([{"version": "float32", "kb": 1100.0, "latencia_p95_ms": 0.8}]), encoding="utf-8")
    return c


def test_tabla_ordenada_por_f1_y_misma_prueba(tmp_path):
    carpetas = [_carpeta(tmp_path, "gru", 0.90), _carpeta(tmp_path, "conv", 0.92), _carpeta(tmp_path, "transformer", 0.85)]
    assert comparar.main([str(c) for c in carpetas] + ["--salida", str(tmp_path / "c.md")]) == 0
    texto = (tmp_path / "c.md").read_text(encoding="utf-8")
    filas = [l for l in texto.splitlines() if l.startswith("| ") and "%" in l]
    assert [f.split("|")[1].strip() for f in filas] == ["conv", "gru", "transformer"]
    assert "Mejor F1 macro: **conv**" in texto
    assert "mismas personas de prueba: p07, p08" in texto


def test_avisa_si_las_personas_de_prueba_no_coinciden(tmp_path):
    carpetas = [_carpeta(tmp_path, "gru", 0.9), _carpeta(tmp_path, "conv", 0.9, prueba=("p01",))]
    assert "no es justa" in comparar.tabla([comparar.leer(c) for c in carpetas])
