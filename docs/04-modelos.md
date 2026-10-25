# 04 · Modelos de IA

## A. Reconocedor visual

### Entrada: puntos de referencia, no píxeles

Se entrena sobre **coordenadas de MediaPipe**, no sobre imágenes:

- Con pocos datos, un modelo sobre puntos generaliza mucho mejor que una CNN sobre píxeles: ignora
  fondo, ropa e iluminación.
- Es pequeño (cientos de KB) y rápido en cualquier teléfono.
- Es más privado: después de la extracción no queda imagen.
- Los trabajos sobre LSC con MediaPipe + redes temporales (LSTM/GRU) obtienen los mejores
  resultados publicados con estos volúmenes de datos.

Extractor: **MediaPipe Holistic Landmarker** (`com.google.mediapipe:tasks-vision`), que devuelve
manos, cuerpo y rostro en una sola llamada. Alternativa si resulta lento en gama media: Hand
Landmarker + Pose Landmarker (lite), sin rostro. Se decide con mediciones en la fase 5.

### Características por fotograma

1. Manos: 21 puntos × 2 manos × (x, y, z). Mano ausente → ceros + indicador de presencia.
2. Torso superior: hombros, codos, muñecas, nariz (para la posición de las manos respecto al
   cuerpo).
3. Rostro: subconjunto pequeño (cejas, ojos, boca) para las expresiones no manuales relevantes.
4. **Normalización**: origen en el punto medio de los hombros y escala por el ancho de hombros, así
   la seña no depende de la distancia a la cámara. Las manos se describen además en coordenadas
   relativas a la muñeca (forma de la mano).
5. Velocidades: diferencia entre fotogramas consecutivos (trayectoria).

### Arquitectura

| Tarea | Modelo | Por qué |
|---|---|---|
| Dactilología estática | MLP sobre un fotograma (o media de 5) | No hay movimiento que modelar |
| Señas dinámicas aisladas | **Conv1D temporal + GRU bidireccional** sobre ventanas de 30–48 fotogramas | Pocos parámetros, rápido, bueno con pocos datos |
| Alternativa a evaluar | Transformer ligero (2–4 capas) | Mejor con más datos; se compara en la fase 11 |
| Secuencias (futuro) | Segmentación por pausas + reconocedor aislado; más adelante CTC | El señado continuo necesita datos que aún no existen |

**Rechazo de desconocidas** (requisito, no extra):

- Clase explícita «sin seña» entrenada con movimientos que no son señas.
- Umbral sobre la probabilidad calibrada (escalado de temperatura con el conjunto de validación).
- Puntuación de energía (*energy score*) como segunda señal de entrada fuera de distribución.
- Confirmación temporal: la misma seña debe ganar en varias ventanas seguidas.

**Entrenamiento**: Python con Keras 3 (backend TensorFlow), que exporta de forma directa a
`.tflite`, el formato que ejecuta LiteRT. Cuantización de rango dinámico o int8 si no pierde
precisión. Aumento de datos sobre coordenadas: rotación y escala leves, espejo (cambio de mano
dominante, solo en señas simétricas), ruido, variación de velocidad.

**Ejecución en Android**: LiteRT (`com.google.ai.edge.litert:litert`, versión 2.3.0 según la
documentación oficial a octubre de 2026), CPU por defecto y GPU opcional.

## B. Intérprete de texto

### Opciones comparadas

| Opción | Sin Internet | Requisitos | Ventajas | Inconvenientes |
|---|---|---|---|---|
| **Reglas** (determinista) | Sí | Ninguno | Nunca inventa; instantáneo; sirve en cualquier teléfono | Frases poco naturales («YO NOMBRE N-I-K» → «Yo, nombre: Nik») |
| **Gemma 4 E2B con LiteRT-LM** | Sí | Modelo de unos 2,6 GB; teléfono de gama alta con bastante RAM; API 26+ | Privado; buen español; salida estructurada | Descarga grande; no apto para gama baja |
| **ML Kit GenAI (Gemini Nano en AICore)** | Sí | Solo en dispositivos compatibles | El modelo ya está en el sistema; no hay descarga | Disponibilidad limitada a ciertos teléfonos |
| **Proveedor remoto** (API de un LLM) | No | Internet y clave de API | Mejor calidad en frases complejas | Coste, latencia, privacidad: solo texto, solo con consentimiento |

### Decisión

1. **Por defecto: intérprete de reglas.** La app funciona completa sin modelo de lenguaje.
2. **Opcional local: Gemma 4 E2B mediante LiteRT-LM** (`com.google.ai.edge.litertlm:litertlm-android`),
   descargado bajo demanda solo si el teléfono cumple los requisitos. Es la opción que cumple el
   requisito de IA integrada sin sacrificar privacidad.
3. **Remoto: se evalúa en la fase 10** y queda desactivado por defecto.

Los tamaños y requisitos se vuelven a comprobar en la fase 8 contra la documentación oficial y un
teléfono real antes de fijar la versión.

### Cómo se usa el modelo de lenguaje

- Recibe solo la secuencia de `SeñaConfirmada` (glosa, significado, confianza) y, si existe, la
  frase anterior como contexto.
- Instrucciones fijas: no añadir señas, conservar el significado, marcar la incertidumbre, preguntar
  si es ambiguo, responder solo con el JSON del contrato (doc. 02).
- Su salida pasa por un validador antes de mostrarse; si falla, se usa el intérprete de reglas.
- Se ejecuta solo cuando hay 2 o más señas y la secuencia está cerrada (pausa o botón), nunca en
  cada fotograma.

## Qué funciona sin Internet

| Componente | Sin Internet |
|---|---|
| Cámara, MediaPipe, reconocedor LiteRT, filtro temporal | Sí, siempre |
| Intérprete de reglas | Sí, siempre |
| Intérprete local (Gemma) | Sí, una vez descargado el modelo |
| Historial | Sí (local) |
| Intérprete remoto | No |
| Descarga inicial del modelo de lenguaje | No (una sola vez) |
