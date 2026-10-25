# 01 · Viabilidad

## Conclusión

El proyecto es **viable** si se define como *reconocimiento de un vocabulario limitado de señas
aisladas de LSC*, con una ampliación progresiva hacia secuencias cortas. **No es viable**, con los
datos públicos que existen hoy, prometer una traducción general de la LSC: eso sigue siendo un
problema de investigación abierto incluso para lenguas con muchos más datos, como la ASL o la DGS.

| Capacidad | Viabilidad | Comentario |
|---|---|---|
| Detección de manos, cuerpo y rostro en tiempo real | Alta | MediaPipe ya lo resuelve en el dispositivo |
| Dactilología estática (letras sin movimiento) | Alta | Hay datos públicos de LSC (LSC70AN/ANH) |
| Señas aisladas con movimiento, vocabulario de 20–50 | Media-alta | Datos públicos pequeños; requiere evaluación con personas no vistas |
| Secuencias de varias señas con pausas | Media | Se resuelve segmentando por pausas, no con reconocimiento continuo |
| Señado continuo natural, sin pausas | Baja | Requiere corpus anotados que no existen públicamente para LSC |
| Gramática de la LSC (expresiones no manuales, espacio) | Baja | Se registra como límite explícito |
| Frase propuesta a partir de señas confirmadas | Alta | Un modelo de lenguaje pequeño basta si no se le pide inventar |
| Todo sin Internet | Alta (reconocedor) / Media (modelo de lenguaje) | El modelo de lenguaje local exige un teléfono de gama alta |

## Por qué la detección de manos no es reconocimiento de señas

MediaPipe devuelve **coordenadas** de 21 puntos por mano, del cuerpo y del rostro. No sabe qué
seña se está haciendo. El significado sale de un **segundo modelo**, entrenado con ejemplos de LSC
etiquetados, que aprende la forma de la mano, su orientación, su posición respecto al cuerpo y su
trayectoria en el tiempo. Ese modelo es el núcleo de la IA del proyecto.

## Riesgos y mitigación

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Pocos datos de LSC y de pocas personas | El modelo memoriza a las personas del entrenamiento | Evaluación con personas excluidas del entrenamiento; aumento de datos sobre puntos de referencia; recogida propia de datos (doc. 03) |
| Datos grabados por personas no expertas | Señas mal ejecutadas aprendidas como correctas | Validar el vocabulario y una muestra de los datos con una persona competente en LSC |
| Variación regional de la LSC | Señas correctas no reconocidas | Vocabulario inicial con una sola variante documentada; registrar la variante en las etiquetas |
| Falsos positivos | Texto incorrecto presentado como traducción | Rechazo de señas desconocidas, umbral calibrado, clase «sin seña», confirmación temporal |
| El modelo de lenguaje inventa contenido | Frases especulativas presentadas como traducción | Panel separado, salida estructurada validada, prohibición de usar señas no detectadas |
| Rendimiento en teléfonos modestos | Retrasos, calor, batería | Descarte de fotogramas, modelos cuantizados, modelo de lenguaje opcional y bajo demanda |
| Diferencia entre puntos de entrenamiento y del teléfono | Caída de precisión en producción | Extraer los puntos de **todos** los datos con la misma versión de MediaPipe que usa la app |
| Privacidad de datos biométricos (rostro, manos) | Incumplimiento de la Ley 1581 de 2012 | Procesamiento local; no se guardan imágenes ni vídeos; consentimiento explícito para cualquier envío |

## Qué se promete y qué no

Se promete:

- Reconocer las señas del vocabulario publicado en la app, con su confianza.
- Decir «no reconocida» en lugar de adivinar.
- Separar siempre lo que reconoció el modelo visual de lo que propone el modelo de lenguaje.
- Publicar las métricas reales obtenidas con personas no vistas en el entrenamiento.

No se promete:

- Traducir cualquier conversación en LSC.
- Reconocer señado continuo rápido, expresiones no manuales con valor gramatical ni el uso del
  espacio sígnico en la primera versión.
- Funcionar igual en todos los teléfonos, iluminaciones y fondos.
