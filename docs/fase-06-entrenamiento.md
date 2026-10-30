# Fase 6 · Datos y entrenamiento

> **Estado:** parte 1 ✅ (sistema de entrenamiento completo y probado sin datos reales).
> Parte 2 pendiente: descargar LSC70, entrenar y medir el modelo real.

## Qué hay

La carpeta [`training/`](../training/README.md) tiene todo el camino desde las imágenes o vídeos
hasta los archivos que carga la app:

| Paso | Script | Resultado |
|---|---|---|
| Extraer puntos | `extraer.py` | Una muestra `.npz` por repetición (MediaPipe Holistic, mismos puntos que la app) |
| Entrenar | `entrenar.py` | `modelo.keras` y `entrenamiento.json` con la partición por persona |
| Evaluar | `evaluar.py` | `metricas.json` e `informe.md` con personas no vistas; temperatura y umbral de energía |
| Exportar | `exportar.py` | `reconocedor_lsc.tflite` y `modelo.json`; con `--instalar`, los copia a la app |

## Modelo

Conv1D (2 capas) → GRU bidireccional → capa densa sin activación (logits). Unos 280 000 parámetros
y ~1,1 MB en `.tflite`, solo con operaciones nativas de LiteRT. Entrada `[1, 32, 352]`.

## Garantías que dan las pruebas

- **Mismas características en Python y Kotlin.** Cuatro casos compartidos
  (`training/pruebas/casos_caracteristicas.json`): Kotlin calcula los valores esperados y Python
  debe reproducirlos con error < 1e-5.
- **La app acepta el `modelo.json` de Python.** `ContratoGeneradoTest.kt` lee con `LectorContrato`
  un archivo escrito por `senas/contrato.py`.
- **El `.tflite` da lo mismo que Keras** (diferencia medida ~2e-7); `exportar.py` se niega a
  exportar si la diferencia supera 1e-3.
- **El flujo completo funciona** con datos sintéticos (`test_flujo_completo.py`). Esos datos no son
  señas: el modelo de la prueba no se instala nunca.

## Cómo comprobarlo

```bash
cd training
pip install -r requirements.txt
python -m pytest pruebas
```
y en la raíz, `gradlew.bat test` (incluye `CasosCompartidosTest` y `ContratoGeneradoTest`).

## Parte 2 (pendiente)

1. Descargar LSC70 en `training/data/LSC70/` ([DATOS.md](../training/DATOS.md)).
2. Elegir las señas del vocabulario inicial y validarlas con una persona competente en LSC.
3. Ejecutar los cuatro pasos y revisar `informe.md`.
4. `exportar.py --instalar` y probar en el teléfono.
