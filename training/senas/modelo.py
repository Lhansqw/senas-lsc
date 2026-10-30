"""Arquitecturas del reconocedor y conversión a LiteRT.

Tres arquitecturas para comparar en la fase 11 (todas devuelven **logits**, sin activación final,
como exige el contrato: la app aplica la temperatura y el softmax):

- `gru` (por defecto): Conv1D temporal + GRU bidireccional (doc. 04). La GRU se convierte en un
  bucle (`WHILE`) que LiteRT solo ejecuta en CPU.
- `conv`: solo convoluciones 1D dilatadas y promedio temporal. Sin bucles: más rápida y apta para
  GPU, a cambio de ver el orden temporal de forma más local.
- `transformer`: proyección, posición aprendida y 2 bloques de atención. Más parámetros; suele
  necesitar más datos que los otros dos.
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


ARQUITECTURAS = ("gru", "conv", "transformer")


@keras.saving.register_keras_serializable(package="senas")
class PosicionAprendida(keras.layers.Layer):
    """Suma un vector aprendido por instante de la ventana (el Transformer no sabe el orden)."""

    def build(self, forma):
        self.posiciones = self.add_weight(shape=(forma[1], forma[2]), initializer="zeros", name="posiciones")

    def call(self, x):
        return x + self.posiciones


def _cuerpo_gru(x, filtros, unidades_gru, abandono):
    x = keras.layers.Conv1D(filtros, 5, padding="same", activation="relu")(x)
    x = keras.layers.BatchNormalization()(x)
    x = keras.layers.Dropout(abandono)(x)
    x = keras.layers.Conv1D(filtros, 5, padding="same", activation="relu")(x)
    x = keras.layers.BatchNormalization()(x)
    x = keras.layers.Bidirectional(keras.layers.GRU(unidades_gru))(x)
    return keras.layers.Dropout(abandono)(x)


def _cuerpo_conv(x, filtros, abandono):
    for dilatacion in (1, 2, 4):
        x = keras.layers.Conv1D(filtros, 5, padding="same", dilation_rate=dilatacion, activation="relu")(x)
        x = keras.layers.BatchNormalization()(x)
        x = keras.layers.Dropout(abandono)(x)
    return keras.layers.GlobalAveragePooling1D()(x)


def _cuerpo_transformer(x, dimension, abandono, bloques=2, cabezas=4):
    x = keras.layers.Dense(dimension)(x)
    x = PosicionAprendida()(x)
    for _ in range(bloques):
        atencion = keras.layers.MultiHeadAttention(cabezas, dimension // cabezas, dropout=abandono)(x, x)
        x = keras.layers.LayerNormalization()(x + atencion)
        avance = keras.layers.Dense(dimension * 2, activation="relu")(x)
        avance = keras.layers.Dense(dimension)(avance)
        x = keras.layers.LayerNormalization()(x + keras.layers.Dropout(abandono)(avance))
    return keras.layers.Dropout(abandono)(keras.layers.GlobalAveragePooling1D()(x))


def construir(fotogramas: int, clases: int, filtros: int = 96, unidades_gru: int = 64, abandono: float = 0.3,
              arquitectura: str = "gru") -> keras.Model:
    if arquitectura not in ARQUITECTURAS:
        raise ValueError(f"Arquitectura desconocida: {arquitectura} (opciones: {', '.join(ARQUITECTURAS)})")
    entrada = keras.Input(shape=(fotogramas, C.POR_FOTOGRAMA), name="caracteristicas")
    if arquitectura == "gru":
        x = _cuerpo_gru(entrada, filtros, unidades_gru, abandono)
    elif arquitectura == "conv":
        x = _cuerpo_conv(entrada, filtros, abandono)
    else:
        x = _cuerpo_transformer(entrada, filtros if filtros % 4 == 0 else 96, abandono)
    salida = keras.layers.Dense(clases, name="logits")(x)
    modelo = keras.Model(entrada, salida, name=f"reconocedor_lsc_{arquitectura}")
    modelo.compile(
        optimizer=keras.optimizers.Adam(1e-3),
        loss=keras.losses.SparseCategoricalCrossentropy(from_logits=True),
        metrics=["accuracy"],
    )
    return modelo


CUANTIZACIONES = ("ninguna", "float16", "int8")


def a_tflite(modelo: keras.Model, fotogramas: int, cuantizacion: str = "ninguna") -> bytes:
    """Convierte a LiteRT con entrada fija `[1, fotogramas, 352]` y solo operaciones nativas.

    Se exporta primero como SavedModel con esa firma, para que los pesos queden congelados en el
    archivo (con funciones concretas sueltas quedaban como variables sin inicializar).

    `cuantizacion` reduce el tamaño de los pesos (la entrada y la salida siguen en float32, así que
    el contrato con la app no cambia):
    - `float16`: pesos a la mitad de tamaño; casi sin pérdida; la GPU los usa directamente.
    - `int8`: cuantización dinámica de rango, pesos a la cuarta parte; en CPU suele ser la más
      rápida, pero puede perder algo de exactitud: medirla siempre con `optimizar.py`.
    """
    if cuantizacion not in CUANTIZACIONES:
        raise ValueError(f"Cuantización desconocida: {cuantizacion} (opciones: {', '.join(CUANTIZACIONES)})")
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
        if cuantizacion != "ninguna":
            conversor.optimizations = [tf.lite.Optimize.DEFAULT]
        if cuantizacion == "float16":
            conversor.target_spec.supported_types = [tf.float16]
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
