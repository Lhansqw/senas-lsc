from dataclasses import dataclass
from pathlib import Path

import pytest

from senas import subconjuntos as S
from senas.extraccion import fotograma_desde_resultado, ordenar_imagenes, tiempos_uniformes


@dataclass
class P:
    x: float
    y: float
    z: float = 0.0


@dataclass
class ResultadoFalso:
    """Mismos campos que HolisticLandmarkerResult de MediaPipe."""
    left_hand_landmarks: list
    right_hand_landmarks: list
    pose_landmarks: list
    face_landmarks: list


def puntos(n):
    return [P(i / 1000, i / 2000, -i / 3000) for i in range(n)]


def test_convierte_y_selecciona_los_subconjuntos():
    f = fotograma_desde_resultado(ResultadoFalso(puntos(21), [], puntos(33), puntos(478)), 120, 480, 640)

    assert f.timestamp_ms == 120
    assert f.relacion_aspecto == pytest.approx(0.75)
    assert f.mano_izquierda.shape == (21, 3)
    assert f.mano_derecha is None
    assert [round(x * 1000) for x in f.cuerpo[:, 0]] == S.CUERPO
    assert [round(x * 1000) for x in f.rostro[:, 0]] == S.ROSTRO


def test_listas_incompletas_son_no_detectado():
    f = fotograma_desde_resultado(ResultadoFalso(puntos(20), puntos(22), puntos(10), puntos(100)), 0, 100, 100)
    assert f.mano_izquierda is None and f.mano_derecha is None and f.cuerpo is None and f.rostro is None


def test_tiempos_uniformes_para_secuencias_de_imagenes():
    assert tiempos_uniformes(6, 1000) == [0, 200, 400, 600, 800, 1000]
    assert tiempos_uniformes(1, 1000) == [0]
    with pytest.raises(ValueError):
        tiempos_uniformes(0, 1000)


def test_orden_natural_de_las_imagenes():
    assert ordenar_imagenes(["f10.jpg", "f2.jpg", "F1.jpg"]) == ["F1.jpg", "f2.jpg", "f10.jpg"]


def test_estructura_de_repeticiones(tmp_path):
    import extraer

    (tmp_path / "s").mkdir()
    for n in ["3.jpg", "1.jpg", "2.jpg"]:
        (tmp_path / "s" / n).write_bytes(b"")
    (tmp_path / "s" / "toma2.mp4").write_bytes(b"")
    (tmp_path / "s" / "r3").mkdir()
    (tmp_path / "s" / "r3" / "a.png").write_bytes(b"")

    reps = list(extraer.repeticiones(tmp_path / "s"))
    assert reps[0][0] == "r1" and [p.name for p in reps[0][1]] == ["1.jpg", "2.jpg", "3.jpg"]
    assert reps[1] == ("toma2", tmp_path / "s" / "toma2.mp4")
    assert reps[2][0] == "r3"
