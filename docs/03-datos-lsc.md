# 03 · Datos de LSC

## Conjuntos de datos públicos encontrados

| Conjunto | Contenido | Personas | Formato | Licencia | Uso en el proyecto |
|---|---|---|---|---|---|
| **LSC70** (U. del Cauca, 2024) — DOI [10.17632/9ssyn8tff5.2](https://data.mendeley.com/datasets/9ssyn8tff5/2) | 47 señas (alfabeto, números y palabras de conversación básica: saludos, presentaciones); 35 208 imágenes | 70 voluntarios **no expertos** | JPG; transiciones de 6 fotogramas; partes LSC70AN (cuerpo), LSC70ANH (mano, 120×120) y LSC70W (palabras) | **CC BY 4.0** (verificada en Mendeley Data) | Dactilología y primeras palabras. Muchas personas: bueno para generalizar |
| **LSC-54** (U. de La Sabana, 2025) — *Data in Brief* | 11 colores, 33 frases de cortesía, números 1–10 | 22 participantes, 3–5 repeticiones | Series temporales de coordenadas MediaPipe (rostro, torso, manos) en JSON; aumentado ×45 | Por verificar en el repositorio del artículo | Vocabulario dinámico del MVP. Ya viene en puntos de referencia |
| **LSC50** (U. de los Andes, 2024) — *Scientific Data*, DOI [10.6084/m9.figshare.27383016](https://figshare.com/articles/dataset/LSC50_Colombian_Sign_Language_Video_and_Inertial_Measurement_Dataset/27383016) | Elementos del léxico de LSC; vídeo + sensores inerciales + puntos del cuerpo | Personas nativas y no nativas | Vídeo, IMU, puntos; 9,9 GB | Por verificar (la página de Figshare no la muestra en texto) | Vídeo con señantes nativos: el mejor candidato para **evaluar** |
| **CoL-SLTD** (UIS) | 39 frases, 114 palabras, 90 glosas; 1020 vídeos | Intérpretes | Vídeo 448×448 a 30 fps en estudio con croma | Por verificar; acceso probablemente bajo solicitud | Fase futura de secuencias (frases con gramática) |

Observaciones importantes:

- **Ninguno basta solo.** Son pequeños y fueron grabados en condiciones distintas. La estrategia es
  unificarlos por vocabulario: misma etiqueta para la misma seña, aunque venga de dos fuentes.
- **LSC70 son imágenes, no vídeo.** Las 6 imágenes por transición sirven para señas estáticas y
  para un modelo temporal muy corto; no capturan toda la trayectoria de señas largas.
- **LSC-54 ya trae coordenadas**, pero de una versión de MediaPipe que hay que confirmar. Si
  difiere de la que use la app, se re-extrae a partir de vídeo propio o se valida la diferencia.
- Las personas voluntarias de LSC70 **no son expertas**: antes de usar una seña se revisa una
  muestra con una persona competente en LSC.
- Todo uso cita a las autoras y autores según la licencia; se mantiene un archivo
  `training/DATOS.md` con origen, licencia y fecha de descarga de cada conjunto.

## Decisiones de licencia

- Solo se entrena con conjuntos cuya licencia permita el uso (CC BY 4.0 o equivalente) o con
  autorización escrita. Mientras una licencia esté «por verificar», el conjunto no entra al
  entrenamiento.
- Los datos no se suben al repositorio: se descargan con un script y quedan en
  `training/data/` (excluida en `.gitignore`).
- El modelo entrenado sí se versiona (es pequeño), con su ficha: datos usados, métricas, fecha.

## Protocolo de grabación propia

Hará falta para completar el vocabulario y, sobre todo, para tener un **conjunto de prueba con
personas que el modelo nunca vio**.

1. **Vocabulario validado.** Una persona competente en LSC (intérprete certificado o persona sorda
   señante; contactos posibles: INSOR, FENASCOL, programas universitarios de LSC) revisa la lista
   y graba un vídeo de referencia de cada seña con su variante.
2. **Consentimiento informado.** Documento firmado que explique: qué se graba (vídeo de rostro,
   manos y torso), para qué, cuánto tiempo se guarda, quién accede y cómo retirar los datos.
   Los datos de rostro son **datos sensibles** según la Ley 1581 de 2012.
3. **Grabación.** Herramienta propia (fase 6) que muestra la seña a imitar, cuenta atrás y graba
   entre 3 y 5 repeticiones. Variación deliberada: iluminación, fondo, distancia, ropa, mano
   dominante. Teléfono en vertical, cámara frontal, como en el uso real.
4. **Etiquetado.** Cada clip lleva: `id` de seña, persona (código anónimo), repetición, condición,
   mano dominante, variante. Una segunda persona revisa los clips y descarta los mal ejecutados.
5. **Clase «sin seña».** Se graban también movimientos que **no** son señas (rascarse, hablar
   gesticulando, manos en reposo) para enseñar al modelo a rechazarlos.
6. **Almacenamiento.** Vídeos originales fuera del repositorio y cifrados; el entrenamiento usa solo
   los puntos de referencia extraídos.
7. **Partición por persona.** Entrenamiento, validación y prueba no comparten personas. Es la única
   forma honesta de medir si el sistema funcionará con alguien nuevo.

## Evaluación

Se calcula y publica, siempre sobre personas no vistas:

- Exactitud global y por seña; precisión, recall y F1 por seña y macro-F1.
- Matriz de confusión (para ver qué señas se confunden entre sí).
- Rechazo de desconocidas: tasa de falsos positivos con la clase «sin seña» y con señas fuera del
  vocabulario; curva de cobertura frente a exactitud según el umbral.
- Calibración: error de calibración esperado (ECE) de la confianza mostrada.
- Latencia por etapa en un teléfono real (p50 y p95), uso de RAM y consumo durante 10 minutos.
- Calidad del intérprete: porcentaje de propuestas que usan solo señas detectadas (debe ser 100 %),
  y valoración humana de una muestra de frases.

Ninguna métrica se escribe en la documentación o en la app sin el script que la reproduce.
