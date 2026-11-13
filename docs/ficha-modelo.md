# Ficha del modelo: reconocedor-lsc 0.3.0

Generada por `training/ficha.py` a partir de los archivos del entrenamiento. No editar a mano.

## Para qué sirve

Reconoce 91 señas aisladas de la Lengua de Señas Colombiana (LSC) a partir de los
puntos de MediaPipe de manos y cuerpo, en ventanas de 1000 ms. Lo usa la app Señas LSC para mostrar las señas como texto.

**No sirve para** traducir conversaciones en LSC, reconocer señas fuera de su vocabulario ni tomar
decisiones sobre personas. Siempre muestra sus resultados como propuesta revisable.

## Datos

- LSC70 (Universidad del Cauca, 2024), CC BY 4.0, doi:10.17632/9ssyn8tff5.2; LSC50 (Universidad de los Andes, 2024), CC BY 4.0, doi:10.6084/m9.figshare.27383016
- Personas: 53 de entrenamiento, 11 de validación y 11 de prueba. Ninguna persona está en dos grupos.

## Modelo

- Arquitectura: gru  289 947 parámetros
- Entrada: 32 instantes × 352 características (versión 1); salida: logits de 91 clases.
- Usa el rostro: no.
- Calibración: temperatura 2.593, umbral de energía -14.741.
- Tamaño del archivo `.tflite`: 324 KB.

## Resultados con personas que el modelo no vio

Personas de prueba: LSC50_v3, Per07, Per08, Per16, Per17, Per19, Per23, Per40, Per43, Per54, Per67 (657 muestras).

| Métrica | Valor |
|---|---|
| Exactitud | 86.3 % |
| F1 macro | 81.7 % |
| Error de calibración (ECE) | 0.075 |
| Señas conocidas rechazadas por el umbral | 0.2 % |

### Señas con peor resultado

| Seña | F1 | Recall | Muestras |
|---|---|---|---|
| CÓMO ESTÁS | 0.0 % | 0.0 % | 4 |
| MUJER | 0.0 % | 0.0 % | 4 |
| NIÑA | 0.0 % | 0.0 % | 4 |
| QUÉ | 0.0 % | 0.0 % | 4 |
| SENTIR | 0.0 % | 0.0 % | 4 |

### Versiones cuantizadas

| Versión | Tamaño | Exactitud |
|---|---|---|
| float32 | 1155 KB | 86.3 % |
| float16 | 593 KB | 86.5 % |
| int8 | 324 KB | 86.5 % |

## Limitaciones conocidas

- **No tiene clase «sin seña».** Cualquier movimiento de las manos puede reconocerse como alguna seña; solo el umbral de energía y el filtro temporal de la app lo frenan.
- **No usa el rostro**, así que no distingue señas que solo cambian por la expresión facial o la boca.
- Entrenado con **fotos** (6 por seña) interpoladas, no con vídeo: en la cámara en vivo puede rendir peor.
- Las personas de LSC70 no son expertas en LSC; las señas deben validarse con una persona competente.
- Con 11 personas de prueba, las cifras tienen un margen de varios puntos.
- No se ha medido por separado en personas zurdas, con poca luz ni con fondos complejos.

## Licencia

Los datos de entrenamiento se citan arriba con su licencia; el modelo se distribuye dentro de la app con esa
misma atribución (pantalla «Acerca de»).
