"""Arquitectura del reconocedor y conversión a LiteRT.

Conv1D temporal + GRU bidireccional (doc. 04): pocos parámetros, rápida en el teléfono y adecuada
para conjuntos de datos pequeños. La última capa no tiene activación: el modelo devuelve
**logits**, como exige el contrato (la app aplica la temperatura y el softmax).
"""
from __future__ import annotations

import os
import tempfile
from pathlib import Path

os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL", "2")

import keras  # noqa: E402
import numpy as np  # noqa: E402
import tensorflow as tf  # noqa: E402

from . import caracteristicas as C  # noqa: E402


def construir(fotogramas: int, clases: int, filtros: int = 96, unidades_gru: int = 64, abandono: float = 0.3) -> keras.Model:
    entrada = keras.Input(shape=(fotogramas, C.POR_FOTOGRAMA), name="caracteristicas")
    x = keras.layers.Conv1D(filtros, 5, padding="same", activation="relu")(entrada)
    x = keras.layers.BatchNormalization()(x)
    x = keras.layers.Dropout(abandono)(x)
    x = keras.layers.Conv1D(filtros, 5, padding="same", activation="relu")(x)
    x = keras.layers.BatchNormalization()(x)
    x = keras.layers.Bidirectional(keras.layers.GRU(unidades_gru))(x)
    x = keras.layers.Dropout(abandono)(x)
    salida = keras.layers.Dense(clases, name="logits")(x)
    modelo = keras.Model(entrada, salida, name="reconocedor_lsc")
    modelo.compile(
        optimizer=keras.optimizers.Adam(1e-3),
        loss=keras.losses.SparseCategoricalCrossentropy(from_logits=True),
        metrics=["accuracy"],
    )
    return modelo


def a_tflite(modelo: keras.Model, fotogramas: int) -> bytes:
    """Convierte a LiteRT con entrada fija `[1, fotogramas, 352]` y solo operaciones nativas.

    Se exporta primero como SavedModel con esa firma, para que los pesos queden congelados en el
    archivo (con funciones concretas sueltas quedaban como variables sin inicializar).
    """
    with tempfile.TemporaryDirectory() as carpeta:
        exportador = keras.export.ExportArchive()
        exportador.track(modelo)
        exportador.add_endpoint(
            name="serve",
            fn=lambda x: modelo(x, training=False),
            input_signature=[tf.TensorSpec([1, fotogramas, C.POR_FOTOGRAMA], tf.float32, name="caracteristicas")],
        )
        exportador.write_out(carpeta, verbose=False)
        conversor = tf.lite.TFLiteConverter.from_saved_model(carpeta)
        conversor.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS]
        return conversor.convert()


def ejecutar_tflite(contenido: bytes, x: np.ndarray) -> np.ndarray:
    """Ejecuta el `.tflite` con el intérprete de LiteRT, una muestra cada vez (como en la app)."""
    from ai_edge_litert.interpreter import Interpreter

    interprete = Interpreter(model_content=contenido)
    interprete.allocate_tensors()
    entrada = interprete.get_input_details()[0]
    salida = interprete.get_output_details()[0]
    resultados = []
    for muestra in x:
        interprete.set_tensor(entrada["index"], muestra[None].astype(np.float32))
        interprete.invoke()
        resultados.append(interprete.get_tensor(salida["index"])[0].copy())
    return np.stack(resultados)


def guardar_tflite(contenido: bytes, ruta: Path) -> Path:
    ruta.parent.mkdir(parents=True, exist_ok=True)
    ruta.write_bytes(contenido)
    return ruta
