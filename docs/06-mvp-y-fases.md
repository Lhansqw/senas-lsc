# 06 · MVP y plan de fases

## MVP

Una app que, **sin Internet**, reconoce en tiempo real un vocabulario pequeño de LSC, muestra cada
seña con su confianza, rechaza lo que no conoce y propone una frase cuando hay varias señas.

**Incluido**

- Cámara frontal con vista previa y puntos de las manos dibujados encima.
- Reconocedor propio (LiteRT) para:
  - **Dactilología estática**: las letras del alfabeto de LSC que no tienen movimiento.
  - **20–30 señas dinámicas aisladas** de conversación básica (saludos, cortesía, números).
- Rechazo de señas desconocidas y clase «sin seña».
- Secuencia de señas confirmadas, segmentada por pausas.
- Intérprete de reglas (siempre) e intérprete local con Gemma 4 E2B (opcional, si el teléfono lo
  permite), en un panel separado y marcado como propuesta.
- Editar, copiar y borrar el texto; historial local opcional.
- Pantalla «Vocabulario» con la lista exacta de señas reconocibles.
- Métricas reales del modelo publicadas en la app y en el repositorio.

**Fuera del MVP**

- Señado continuo sin pausas.
- Intérprete remoto (se evalúa en la fase 10).
- Otras lenguas de señas (la arquitectura lo permite: vocabulario y modelo por lengua).
- Síntesis de voz, cámara trasera, modo conversación entre dos personas.

### Vocabulario inicial

La lista definitiva se fija en la **fase 6**, al cruzar tres condiciones:

1. La seña está en un conjunto de datos con licencia válida (LSC70, LSC-54) **o** se graba en el
   proyecto.
2. Una persona competente en LSC valida la variante.
3. Hay suficientes personas distintas para separar entrenamiento y prueba.

Lista candidata (a validar): HOLA, GRACIAS, POR FAVOR, BUENOS DÍAS, BUENAS TARDES, BUENAS NOCHES,
PERDÓN, SÍ, NO, NOMBRE, YO, TÚ, CÓMO, BIEN, MAL, AYUDA, ADIÓS, números 1–10 y el alfabeto
estático.

## Plan de fases

Cada fase termina con un commit verificable: archivos completos, dependencias, comandos y una forma
concreta de comprobar que funciona.

> 🔶 **Fase 6:** el sistema de entrenamiento está hecho y probado (parte 1, doc. fase 6); falta
> entrenar con datos reales de LSC (parte 2). Las fases 7 a 10 se construyen contra un
> *contrato de modelo* fijo (doc. fase 7): el formato de las características de entrada, la salida
> y el archivo `modelo.json` que describe el modelo. Cuando se entrene, bastará con copiar el
> modelo y su descripción; mientras tanto la app indica que no hay modelo instalado y no muestra
> señas inventadas.

| Fase | Entregable | Cómo se comprueba |
|---|---|---|
| **1. Alcance** ✅ | Esta documentación: viabilidad, arquitectura, datos, modelos, entorno, MVP | Revisión del documento |
| **2. Proyecto Android** ✅ | Proyecto Gradle (Kotlin DSL) con catálogo de versiones, Compose y configuración base | Compila y abre una pantalla en el teléfono |
| **3. Estructura modular** ✅ | Módulos Kotlin puros del doc. 02 con sus contratos, filtro temporal, validador e intérprete por reglas, con pruebas JUnit | `./gradlew test` en verde |
| **4. Cámara** ✅ | CameraX con permiso explicado, vista previa y análisis con descarte de fotogramas | Vista previa fluida; contador de FPS de análisis |
| **5. MediaPipe** ✅ | Holistic Landmarker → `LandmarkFrame`, puntos dibujados sobre la vista previa | Puntos estables en tiempo real; latencia medida |
| **6. Datos y entrenamiento** 🔶 | `training/`: descarga, extracción de puntos, entrenamiento, evaluación por persona, exportación `.tflite`; herramienta de grabación | Informe de métricas reproducible |
| **7. LiteRT en Android** ✅ | `:recognizer:litert` contra el contrato del modelo, filtro temporal e interfaz | Pruebas del flujo con un modelo simulado; con el modelo real (tras la fase 6): misma salida que en Python para un mismo ejemplo |
| **8. Modelo de lenguaje** ✅ | `:interpreter:litertlm` con Gemma, contrato JSON y validador | Pruebas de que nunca usa señas no detectadas |
| **9. Interfaz de resultados** ✅ | Paneles de señas y propuesta, confianza, edición, copiar, historial, vocabulario | Pruebas de interfaz y revisión de accesibilidad (TalkBack, contraste, tamaños) |
| **10. Local frente a nube** | Selección de intérprete según dispositivo; evaluación del remoto con consentimiento | Comparativa de calidad, latencia y coste |
| **11. Optimización** | Cuantización, GPU, frecuencia de inferencia, comparación GRU frente a Transformer | Latencia p95 y batería medidas antes y después |
| **12. Versión instalable** | APK/AAB firmado, pruebas finales, ficha del modelo | Instalación limpia en un teléfono distinto al de desarrollo |
