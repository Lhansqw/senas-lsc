# Modelo de reconocimiento de señas

Este módulo carga el modelo entrenado desde `src/main/assets/`. Hacen falta **dos archivos**:

| Archivo | Qué es |
|---|---|
| `src/main/assets/reconocedor_lsc.tflite` | El modelo exportado a LiteRT (fase 6) |
| `src/main/assets/modelo.json` | Su descripción: ventana, etiquetas, calibración y vocabulario |

Mientras no estén, la app funciona (cámara y MediaPipe) y muestra «Reconocedor de señas no
instalado». No se incluye ningún modelo de ejemplo: un modelo sin entrenar con datos reales de LSC
daría señas inventadas.

## Contrato

- **Entrada:** `float32[1, fotogramas, 352]`. Son las características de
  `recognizer/api/.../Caracteristicas.kt` (versión 1), calculadas sobre la ventana remuestreada
  con `sequence/.../Remuestreo.kt`.
- **Salida:** `float32[1, número de etiquetas]` con **logits**, sin softmax, en el orden de
  `salida.etiquetas`.
- **`modelo.json`:** formato 1. Hay un ejemplo comentado en `docs/modelo.ejemplo.json` y la
  descripción de cada campo en `docs/fase-07-reconocedor-litert.md`.

Al arrancar, la app comprueba:
- que la versión de las características y la de los subconjuntos de puntos coinciden con las suyas;
- que cada etiqueta tiene su entrada de vocabulario;
- que la salida del modelo tiene tantos valores como etiquetas.

Si algo no cuadra, muestra el error en vez de usar el modelo.
