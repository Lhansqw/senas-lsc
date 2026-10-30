# Fase 6 · Datos y entrenamiento

> **Estado:** parte 1 ✅ (sistema de entrenamiento completo y probado sin datos reales).
> Parte 2 en curso: LSC70 descargado y comprobado; falta ejecutar el entrenamiento en el PC.

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

## Parte 2

Hecho:

1. ✅ LSC70 descargado en `training/data/LSC70/` y revisada su estructura ([DATOS.md](../training/DATOS.md)).
2. ✅ Vocabulario inicial: 42 señas (`vocabulario.lsc70.json`, hoja `vocabulario_lsc_inicial.xlsx`).
3. ✅ MediaPipe probado con fotos reales de LSC70 y el mismo modelo que la app: detecta el cuerpo y
   la mano en todas; el rostro nunca (caras difuminadas).

Dos ajustes que salieron de esa prueba:

- **Velocidades.** 6 fotos por seña frente a ~30 fotogramas por segundo en la app: sin interpolar,
  la velocidad salía 0 en casi todos los instantes y con picos en el resto. Ahora se interpola a
  30 fps antes de calcular la ventana (`senas/interpolacion.py`).
- **Rostro.** Un modelo entrenado sin caras recibiría caras en la app. `modelo.json` tiene ahora
  `caracteristicas.usa_rostro` y, si es `false`, la app pone a cero esos puntos (Kotlin y Python
  comprueban que da lo mismo que no detectar la cara).

Falta:

4. Ejecutar `training\entrenar_lsc70.ps1` en el PC y revisar `informe.md`.
5. Validar las señas con una persona competente en LSC y decidir Ñ, MIL, MILLÓN, GUSTAR y LICOR.
6. Probar el modelo en el teléfono.
