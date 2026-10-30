# Fase 11 · Optimización

> **Estado:** herramientas hechas y probadas. Faltan las mediciones en tu PC (arquitecturas y
> cuantización con LSC70) y en el teléfono (latencia y batería, antes y después).

La regla de esta fase: **no se optimiza nada sin medirlo antes y después.** Por eso casi todo lo
que se añadió son instrumentos de medida; las cifras las ponen ellos, no este documento.

## Qué hay

### En el entrenamiento (`training/`)

| Herramienta | Qué mide o cambia |
|---|---|
| `entrenar.py --arquitectura gru\|conv\|transformer` | Tres modelos para comparar. Los tres se convierten a LiteRT solo con operaciones nativas |
| `optimizar.py` | Para float32, float16 e int8: tamaño, exactitud **ejecutando el `.tflite`** con las personas de prueba, coincidencia con float32 y latencia p50/p95 en el PC |
| `exportar.py --cuantizacion float16\|int8` | Exporta la versión elegida; comprueba que elige la misma seña que Keras en ≥ 95 % de las muestras |
| `comparar.py` y `comparar_lsc70.ps1` | Entrena las tres arquitecturas con la misma partición de personas y escribe una tabla |

Lo que ya se sabe sin datos reales (modelos sin entrenar, 32 instantes, 42 señas):

| Arquitectura | Parámetros | float32 | float16 | int8 | ¿Bucle `WHILE`? |
|---|---|---|---|---|---|
| gru | 283 626 | 1121 KB | 573 KB | 308 KB | Sí → solo CPU |
| conv | 266 634 | 1039 KB | 522 KB | 267 KB | No → apta para GPU |
| transformer | 190 602 | 742 KB | 382 KB | 217 KB | No → apta para GPU |

La exactitud de cada una solo se sabe entrenando con LSC70 (ver abajo).

### En la app

- **Latencia p50 y p95** en el panel de la cámara, por separado:
  - «Análisis»: preparar la imagen + MediaPipe, últimos 300 fotogramas (~10 s).
  - «Modelo de señas»: cada ejecución del reconocedor LiteRT, últimas 120 ventanas.
  El p95 es lo que se nota (los tirones); la media los esconde.
- **Modo reposo**: si MediaPipe no ve manos durante 2 s, se analizan ~5 fotogramas por segundo en
  vez de ~30, y al aparecer una mano se vuelve a analizarlos todos (se nota como mucho 200 ms
  tarde). El panel muestra «Reposo» y los FPS bajan. MediaPipe es lo que más gasta, y casi
  siempre hay ratos sin señar.
- El modelo de señas ya se ejecutaba solo cada 250 ms y solo con manos en la ventana.

### GPU

No se activa todavía. La GRU (la arquitectura actual) se convierte en un bucle `WHILE` que la GPU
de LiteRT no ejecuta, y el modelo tarda poco frente a MediaPipe. Si la comparación muestra que
`conv` es igual de exacta, se pasa a ella y entonces tiene sentido probar la GPU, midiendo en el
teléfono que da las mismas señas que la CPU.

## Cómo medir

### 1. Arquitecturas y cuantización (PC)

```powershell
cd C:\dev\senas-lsc\training
git pull
powershell -ExecutionPolicy Bypass -File .\comparar_lsc70.ps1
```

Escribe `outputs\comparacion\comparacion.md` y, en cada carpeta, `optimizacion.md`. Con 10
personas de prueba, diferencias de 1–2 puntos son ruido: a igualdad, gana la más rápida.

### 2. Latencia en el teléfono

1. `.\gradlew.bat installDebug` y abre la cámara.
2. Señas durante ~30 s y anota las dos líneas de p50/p95.
3. Repite con el modelo nuevo (otra arquitectura o cuantización) en las mismas condiciones.

### 3. Batería (antes y después del modo reposo)

Con el teléfono conectado por USB y la depuración activada:

```powershell
adb shell dumpsys batterystats --reset
# desconecta el cable, deja la app en la cámara 10 minutos sin señar, vuelve a conectar
adb shell dumpsys batterystats --charged com.lhansqw.senaslsc > bateria.txt
```

Busca en `bateria.txt` la línea `Estimated power use` de `com.lhansqw.senaslsc`. Para comparar con
el comportamiento anterior, instala la versión previa del commit (antes de «modo reposo») y repite
con el mismo brillo y la misma luz.

## Archivos

- `training/senas/modelo.py`: arquitecturas y cuantización.
- `training/optimizar.py`, `training/comparar.py`, `training/comparar_lsc70.ps1`.
- `core/model/.../rendimiento/EstadisticaLatencia.kt`: percentiles.
- `camera/.../MedidorRendimiento.kt` (p50/p95) y `camera/.../ReguladorAnalisis.kt` (reposo).
- `app/.../CoordinadorReconocimiento.kt`: mide el modelo de señas.
