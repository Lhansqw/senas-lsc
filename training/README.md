# Entrenamiento del reconocedor de señas

Herramientas en Python que convierten un conjunto de datos de LSC en el modelo que usa la app:
`reconocedor_lsc.tflite` + `modelo.json`.

```
vídeos o imágenes ──extraer.py──▶ puntos (.npz) ──entrenar.py──▶ modelo.keras
                                                   evaluar.py ──▶ metricas.json + informe.md
                                                  exportar.py ──▶ reconocedor_lsc.tflite + modelo.json
```

Las características se calculan **igual que en la app** (`senas/caracteristicas.py` es una copia
fiel de `Caracteristicas.kt`). Lo garantizan los casos compartidos de `pruebas/`: si alguna de las
dos versiones cambia, las pruebas de Kotlin o de Python fallan.

## Preparación (una vez)

Necesitas Python 3.11–3.13.

```bash
cd training
python -m venv .venv
.venv\Scripts\activate          # Windows  (en Linux/macOS: source .venv/bin/activate)
pip install -r requirements.txt
python -m pytest pruebas        # todo debe salir en verde
```

## Pasos

1. **Datos.** Descarga el conjunto de datos y prepara `vocabulario.json` (ver [DATOS.md](DATOS.md)).
2. **Extraer puntos** (descarga sola el modelo de MediaPipe la primera vez):
   ```bash
   python extraer.py --raiz data/LSC70 --vocabulario vocabulario.json --salida data/puntos --fuente LSC70
   ```
3. **Entrenar** (separa las personas: las de prueba no se usan para entrenar):
   ```bash
   python entrenar.py --muestras data/puntos --vocabulario vocabulario.json --salida outputs/v1
   ```
4. **Evaluar** con las personas de prueba y calibrar la confianza:
   ```bash
   python evaluar.py --entrenamiento outputs/v1 --muestras data/puntos
   ```
   Revisa `outputs/v1/informe.md`: exactitud y F1 por seña, matriz de confusión y rechazo.
5. **Exportar e instalar en la app:**
   ```bash
   python exportar.py --entrenamiento outputs/v1 --muestras data/puntos --vocabulario vocabulario.json --version 0.1.0 --instalar
   ```
   Copia los dos archivos a `recognizer/litert/src/main/assets/`. Después: `gradlew.bat installDebug`.

## Reglas

- **No se inventan métricas.** Todas las cifras de `modelo.json` e `informe.md` salen de
  `evaluar.py`; la app las muestra tal cual.
- **Evaluación por persona.** Una misma persona nunca está a la vez en entrenamiento y en prueba;
  así la cifra se parece a lo que pasará con alguien que la app no conoce.
- **Los datos sintéticos de `pruebas/sinteticos.py` son solo para probar el código.** Un modelo
  entrenado con ellos no se instala nunca en la app.
- `data/`, `outputs/` y `modelos/` no se suben al repositorio.

## Archivos

| Archivo | Qué hace |
|---|---|
| `senas/subconjuntos.py`, `fotograma.py` | Puntos que se guardan de MediaPipe (versión 1) |
| `senas/remuestreo.py`, `caracteristicas.py` | Ventana de N instantes y 352 características por fotograma |
| `senas/extraccion.py`, `extraer.py` | De imágenes o vídeos a muestras `.npz` |
| `senas/muestras.py`, `vocabulario.py` | Formato de las muestras y del vocabulario |
| `senas/particion.py`, `aumento.py`, `conjunto.py` | Partición por persona, aumento de datos y tensores |
| `senas/modelo.py` | Arquitectura Conv1D + GRU bidireccional y conversión a LiteRT |
| `senas/metricas.py`, `calibracion.py` | Métricas, temperatura y umbral de energía |
| `senas/contrato.py` | Escritura y validación de `modelo.json` |
| `entrenar.py`, `evaluar.py`, `exportar.py` | Los tres pasos del entrenamiento |
