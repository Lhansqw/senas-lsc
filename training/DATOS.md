# Datos para el entrenamiento

## LSC70 (punto de partida)

- Descarga: <https://data.mendeley.com/datasets/9ssyn8tff5/2> (DOI 10.17632/9ssyn8tff5.2).
- Licencia **CC BY 4.0**: se puede usar y redistribuir el modelo citando a los autores
  (Universidad del Cauca). La cita va en `datos_entrenamiento` de `modelo.json` y en la ficha del
  modelo.
- Contenido: 47 señas (alfabeto, números y palabras básicas) de 70 personas no expertas, en
  imágenes JPG (6 por transición).

Descomprímelo en `training/data/LSC70/`. `extraer.py` espera una carpeta por persona y, dentro, una
por seña (`--orden persona-sena`) o al revés (`--orden sena-persona`). Si las imágenes vienen sin
tiempos, se reparten de forma uniforme en `--duracion-ms`.

## vocabulario.json

Copia `vocabulario.ejemplo.json` como `vocabulario.json` y deja solo las señas que vas a entrenar.
En `carpetas` pon el nombre exacto de la carpeta de esa seña en el conjunto de datos. Cada seña debe
validarla una persona competente en LSC antes de llegar a la app: las de LSC70 las hicieron
personas no expertas.

## Clase «sin seña»

Para que la app rechace movimientos que no son señas, graba muestras de manos quietas, rascarse,
acomodarse el pelo, etc., y guárdalas con la etiqueta `sin_sena`. Sin ellas, el rechazo depende
solo del umbral de energía.

## Grabaciones propias

Si grabas vídeos propios: varias personas distintas (mínimo 5 por seña para poder separar la
prueba), cámara frontal, buena luz y el cuerpo visible hasta la cintura. Pide consentimiento por
escrito para usar los vídeos y no los subas al repositorio.
