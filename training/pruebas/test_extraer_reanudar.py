"""extraer.py recorre la estructura de LSC70, salta lo ya extraído y usa un solo landmarker."""
import contextlib
import json

import extraer
import sinteticos
from senas import muestras as M


def _lsc70_en_miniatura(raiz):
    """Per01/Per02 × A/HOLA/OTRA, con 6 «fotos» vacías cada una (no se leen: se simula MediaPipe)."""
    for persona in ("Per01", "Per02"):
        for sena in ("A", "HOLA", "OTRA"):
            carpeta = raiz / persona / sena
            carpeta.mkdir(parents=True)
            for i in range(6):
                (carpeta / f"{persona}_{sena}_{i}.jpg").write_bytes(b"")


def test_extraer_y_reanudar(tmp_path, monkeypatch, capsys):
    _lsc70_en_miniatura(tmp_path / "LSC70AN")
    voc = tmp_path / "voc.json"
    voc.write_text(json.dumps({"lengua": "lsc", "senas": [
        {"id": "lsc.a", "glosa": "A", "significado": "a", "tipo": "letra", "carpetas": ["A"]},
        {"id": "lsc.hola", "glosa": "HOLA", "significado": "hola", "tipo": "palabra", "carpetas": ["HOLA"]}]}), encoding="utf-8")

    llamadas = {"landmarkers": 0, "muestras": 0}

    @contextlib.contextmanager
    def landmarker_falso(modelo, video):
        llamadas["landmarkers"] += 1
        yield object()

    def imagenes_falsas(lm, imagenes, duracion_ms, espejo):
        llamadas["muestras"] += 1
        assert [p.name[-5] for p in imagenes] == list("012345")   # en orden
        return sinteticos.muestra(1, 0, 0, __import__("numpy").random.default_rng(0), fotogramas=6).fotogramas

    monkeypatch.setattr(extraer, "ubicar_modelo", lambda indicado: tmp_path / "modelo.task")
    monkeypatch.setattr(extraer, "crear_landmarker", landmarker_falso)
    monkeypatch.setattr(extraer, "extraer_imagenes", imagenes_falsas)
    args = ["--raiz", str(tmp_path / "LSC70AN"), "--vocabulario", str(voc), "--salida", str(tmp_path / "puntos"), "--fuente", "LSC70AN"]

    assert extraer.main(args) == 0
    guardadas = M.cargar_carpeta(tmp_path / "puntos")
    assert sorted((m.persona, m.sena) for m in guardadas) == [("Per01", "lsc.a"), ("Per01", "lsc.hola"), ("Per02", "lsc.a"), ("Per02", "lsc.hola")]
    assert {m.fuente for m in guardadas} == {"LSC70AN"}
    assert llamadas == {"landmarkers": 1, "muestras": 4}
    assert "OTRA" in capsys.readouterr().out    # carpeta fuera del vocabulario: se informa

    # Segunda ejecución: no vuelve a procesar nada.
    assert extraer.main(args) == 0
    assert llamadas["muestras"] == 4
    assert "Ya extraídas antes" in capsys.readouterr().out

    assert extraer.main(args + ["--rehacer"]) == 0
    assert llamadas["muestras"] == 8


def test_usa_el_modelo_de_la_app_si_existe(tmp_path, monkeypatch):
    app = tmp_path / "app.task"
    app.write_bytes(b"PK" + b"0" * 2_000_000)
    monkeypatch.setattr(extraer, "MODELO_APP", app)
    assert extraer.ubicar_modelo(None) == app
