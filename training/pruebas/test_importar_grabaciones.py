"""Las grabaciones que escribe la app (Kotlin) se importan como muestras de entrenamiento."""
import json
import shutil
from pathlib import Path

import numpy as np

import importar_grabaciones as I
from senas import muestras as M
from senas.conjunto import construir

EJEMPLO = Path(__file__).parent / "grabacion_ejemplo.json"   # escrito por FormatoGrabacion.kt


def test_lee_el_archivo_escrito_por_kotlin():
    m = I.leer(EJEMPLO)
    assert (m.persona, m.sena, m.repeticion, m.fuente) == ("app_prueba01", "lsc.hola", "r2", "GRABACION_APP")
    assert len(m.fotogramas) == 37
    f0 = m.fotogramas[0]
    assert f0.mano_izquierda is None                      # cada 5 fotogramas falta la izquierda
    assert f0.mano_derecha.shape == (21, 3) and f0.cuerpo.shape == (7, 3) and f0.rostro.shape == (39, 3)
    assert abs(f0.mano_derecha[1, 0] - 0.412) < 1e-6
    assert m.fotogramas[-1].rostro is None and m.fotogramas[-1].timestamp_ms == 36 * 33
    assert abs(f0.relacion_aspecto - 0.75) < 1e-9


def test_importar_carpeta_con_vocabulario(tmp_path):
    entrada = tmp_path / "grabaciones"
    entrada.mkdir()
    shutil.copy(EJEMPLO, entrada / EJEMPLO.name)
    datos = json.loads(EJEMPLO.read_text(encoding="utf-8"))
    (entrada / "sin.json").write_text(json.dumps(dict(datos, sena="sin_sena", repeticion=1)), encoding="utf-8")
    (entrada / "otra.json").write_text(json.dumps(dict(datos, sena="lsc.cafe")), encoding="utf-8")
    (entrada / "rota.json").write_text("{", encoding="utf-8")
    (entrada / "ajena.json").write_text(json.dumps({"formato": 9}), encoding="utf-8")
    voc = tmp_path / "voc.json"
    voc.write_text(json.dumps({"lengua": "lsc", "senas": [{"id": "lsc.hola", "glosa": "HOLA", "significado": "hola", "tipo": "palabra"}]}), encoding="utf-8")

    assert I.main(["--entrada", str(entrada), "--salida", str(tmp_path / "puntos"), "--vocabulario", str(voc)]) == 0
    muestras = M.cargar_carpeta(tmp_path / "puntos")
    assert sorted(m.sena for m in muestras) == ["lsc.hola", "sin_sena"]       # lsc.cafe fuera del vocabulario

    x, y = construir(muestras, ["sin_sena", "lsc.hola"], 32, 1000, usar_rostro=False)
    assert x.shape == (2, 32, 352) and np.isfinite(x).all()
    assert sorted(y.tolist()) == [0, 1]
