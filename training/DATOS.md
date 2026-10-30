# Datos para el entrenamiento

## LSC70 (punto de partida)

- Descarga: <https://data.mendeley.com/datasets/9ssyn8tff5/2> (DOI 10.17632/9ssyn8tff5.2).
- Licencia **CC BY 4.0**: se puede usar y redistribuir el modelo citando a los autores
  (Universidad del Cauca). La cita va en `datos_entrenamiento` de `modelo.json` y en la ficha del
  modelo.
- Contenido: 47 señas (alfabeto, números y palabras básicas) de 70 personas no expertas, en
  imágenes JPG (6 por transición).

Descomprímelo en `training/data/LSC70/`. Queda así (comprobado con la descarga real):

```
data/LSC70/
├── LSC70AN/Per01 … Per70/<seña>/PerNN_<seña>_0.jpg … _5.jpg   alfabeto y números (A…Z, NN, 1, 4…10, MIL, MILLON)
├── LSC70ANH/Per01 … Per70/…                                   recortes de la mano (120×120): no se usan
└── LSC70W/Per01 … Per70/<palabra>/PerNN_<palabra>_0.jpg … _5.jpg   palabras (HOLA, NOMBRE, YO, BUENAS…)
```

- Cada seña son **6 fotos de 640×480** de cuerpo entero, en orden. `extraer.py` las reparte en
  1000 ms y el entrenamiento las interpola a 30 fps (como la cámara de la app) antes de calcular las
  velocidades.
- **Las caras están difuminadas**: MediaPipe detecta el cuerpo y las manos, pero nunca el rostro.
  Por eso `entrenar.py` entrena sin rostro y `modelo.json` lleva `"usa_rostro": false`; la app
  ignora entonces la cara aunque la detecte.
- `LSC70ANH` no se usa: Holistic necesita ver el cuerpo para encontrar las manos.
- Las carpetas `PerNN` se toman como la misma persona en `LSC70AN` y `LSC70W`, así que una persona
  de prueba no aparece en el entrenamiento en ninguna de las dos partes.

## vocabulario.json

Para LSC70 ya está preparado `vocabulario.lsc70.json` (42 señas: 8 palabras, el alfabeto y los
números, según la hoja `vocabulario_lsc_inicial.xlsx`). Para otro conjunto de datos, copia
`vocabulario.ejemplo.json` como `vocabulario.json` y deja solo las señas que vas a entrenar.
En `carpetas` pon el nombre exacto de la carpeta de esa seña en el conjunto de datos. Cada seña debe
validarla una persona competente en LSC antes de llegar a la app: las de LSC70 las hicieron
personas no expertas.

## Clase «sin seña»

Para que la app rechace movimientos que no son señas, graba muestras de manos quietas, rascarse,
acomodarse el pelo, etc., y guárdalas con la etiqueta `sin_sena`. Sin ellas, el rechazo depende
solo del umbral de energía.

## Grabaciones con la app (recomendado)

La app tiene una herramienta de grabación (inicio → «Grabar muestras para entrenar»). Graba
**solo los puntos** de manos y cuerpo, igual que los ve el reconocedor, nunca imágenes:

1. Escribe un alias (por ejemplo `nik`, `p02`; no el nombre real) y elige la seña.
2. Pulsa «Grabar»: 3 s para prepararse y 1,2 s de grabación. Repite 10–20 veces por seña.
3. Copia la carpeta del teléfono al PC, en `training\data\grabaciones\`:
   - por USB: `Este equipo\<teléfono>\Almacenamiento interno\Android\data\com.lhansqw.senaslsc\files\grabaciones`
     (si Windows no muestra `Android\data`, usa la opción siguiente);
   - con adb: `adb pull /sdcard/Android/data/com.lhansqw.senaslsc/files/grabaciones training/data/`
4. Vuelve a ejecutar `entrenar_lsc70.ps1`: importa las grabaciones automáticamente.

Qué grabar primero:

- **«SIN SEÑA»**: manos quietas, rascarse, tocarse el pelo, gesticular al hablar, coger el
  teléfono… Es lo que permite al modelo decir «esto no es una seña» (ahora no tiene esa clase).
- Las señas con peor resultado en la [ficha del modelo](../docs/ficha-modelo.md): S, 1, 6, H, G.
- Varias personas distintas: el modelo se evalúa con personas que no vio, y cada alias cuenta
  como una persona.

Las grabaciones se borran al desinstalar la app. Pide permiso a cada persona que grabes, aunque
solo se guarden puntos.

## Vídeos propios

Si grabas vídeos: varias personas distintas (mínimo 5 por seña para poder separar la prueba),
cámara frontal, buena luz y el cuerpo visible hasta la cintura. `extraer.py` acepta vídeos. Pide
consentimiento por escrito y no los subas al repositorio.
