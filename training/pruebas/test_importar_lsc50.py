"""Los CSV de LSC50 se convierten en muestras recortadas a la seña."""
import json
import zipfile

import numpy as np

import importar_lsc50 as I
from senas import muestras as M
from senas.particion import por_persona

import sinteticos


def _csv(datos: np.ndarray) -> str:
    puntos = datos.shape[1]
    cabecera = "," + ",".join(f"landmark_{i}_{e}" for i in range(puntos) for e in "xyz")
    filas = [f"{k}," + ",".join(f"{v:.6f}" for v in fila.ravel()) for k, fila in enumerate(datos)]
    return "\n".join([cabecera, *filas]) + "\n"


def _clip(fotogramas=40, activos=range(15, 25)):
    """Persona quieta con las muñecas bajas; en `activos` sube la muñeca derecha."""
    pose = np.full((fotogramas, 33, 3), 0.5)
    pose[:, 11, :2] = (0.6, 0.4)    # hombro izquierdo (sin espejo: a la derecha de la imagen)
    pose[:, 12, :2] = (0.4, 0.4)
    pose[:, 23, :2] = (0.55, 0.8)
    pose[:, 24, :2] = (0.45, 0.8)
    pose[:, 15, :2] = (0.62, 0.85)
    pose[:, 16, :2] = (0.38, 0.85)
    pose[list(activos), 16, 1] = 0.3
    mano = np.zeros((fotogramas, 21, 3))
    mano[:, 9, 1] = 0.05            # muñeca → nudillo medio: 0,05 / hombros 0,2 = 0,25 (real)
    encogida = np.zeros((fotogramas, 21, 3))
    encogida[:, 9, 1] = 0.001       # lo que trae el CSV cuando MediaPipe no ve la mano
    return pose, encogida, mano


def _zip(ruta, clips: dict[str, tuple]):
    with zipfile.ZipFile(ruta, "w") as z:
        for nombre, (pose, izq, der) in clips.items():
            z.writestr(f"LANDMARKS/BODY_LANDMARKS/{nombre}", _csv(pose))
            z.writestr(f"LANDMARKS/HANDS_LANDMARKS/LEFT_HAND_LANDMARKS/{nombre}", _csv(izq))
            z.writestr(f"LANDMARKS/HANDS_LANDMARKS/RIGHT_HAND_LANDMARKS/{nombre}", _csv(der))


def test_recorta_a_la_parte_activa_y_la_reparte_en_un_segundo():
    pose, izq, der = _clip()
    m = I.muestra(pose, izq, der, "lsc.gracias", persona=2, repeticion=0)
    assert (m.persona, m.sena, m.repeticion, m.fuente) == ("LSC50_v2", "lsc.gracias", "r1", "LSC50")
    assert len(m.fotogramas) == 10 + 2 * I.MARGEN
    assert m.fotogramas[0].timestamp_ms == 0 and m.fotogramas[-1].timestamp_ms == 1000
    f = m.fotogramas[0]
    assert f.mano_izquierda is None and f.mano_derecha.shape == (21, 3)
    assert f.cuerpo.shape == (7, 3) and f.rostro is None


def test_clip_sin_seña_no_da_muestra():
    pose, izq, der = _clip(activos=[])
    assert I.muestra(pose, izq, der, "lsc.gracias", 0, 0) is None


def test_importar_zip_con_vocabulario_y_reanudar(tmp_path):
    _zip(tmp_path / "LANDMARKS.zip", {
        "0000_0001_0002.csv": _clip(),
        "0001_0001_0000.csv": _clip(),            # BUENOS DÍAS: fuera del vocabulario
        "0000_0003_0000.csv": _clip(activos=[]),  # sin seña
    })
    voc = tmp_path / "voc.json"
    voc.write_text(json.dumps({"lengua": "lsc", "senas": [
        {"id": "lsc.gracias", "glosa": "GRACIAS", "significado": "gracias", "tipo": "palabra", "carpetas": ["0000"]},
    ]}), encoding="utf-8")
    argumentos = ["--zip", str(tmp_path / "LANDMARKS.zip"), "--salida", str(tmp_path / "puntos"), "--vocabulario", str(voc)]

    assert I.main(argumentos) == 0
    muestras = M.cargar_carpeta(tmp_path / "puntos")
    assert [(m.persona, m.sena, m.repeticion) for m in muestras] == [("LSC50_v1", "lsc.gracias", "r3")]
    assert I.main(argumentos) == 0                # la segunda vez no vuelve a importar nada
    assert len(M.cargar_carpeta(tmp_path / "puntos")) == 1


def test_particion_separa_cada_conjunto_de_datos():
    grande = sinteticos.conjunto(personas=10)
    pequeno = [type(m)(f"LSC50_v{m.persona}", m.sena, m.repeticion, "LSC50", m.fotogramas)
               for m in sinteticos.conjunto(personas=5, semilla=3)]
    p = por_persona(grande + pequeno, semilla=1)
    # El conjunto pequeño también tiene personas de prueba y de validación…
    assert any(x.startswith("LSC50_") for x in p.prueba) and any(x.startswith("LSC50_") for x in p.validacion)
    # …y el reparto del grande es el mismo que si estuviera solo.
    solo = por_persona(grande, semilla=1)
    assert tuple(x for x in p.prueba if not x.startswith("LSC50_")) == solo.prueba
