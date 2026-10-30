<#
.SYNOPSIS
    Entrena el reconocedor de señas con LSC70 y lo instala en la app, de principio a fin.

.DESCRIPTION
    1. Crea el entorno de Python (.venv) e instala las dependencias, si hace falta.
    2. Extrae los puntos de MediaPipe de LSC70AN (alfabeto y números) y LSC70W (palabras).
       Se puede interrumpir: al volver a ejecutarlo continúa donde iba.
    3. Entrena, evalúa con personas que el modelo no vio y exporta el .tflite y modelo.json.
    4. Copia el modelo a la app (recognizer/litert/src/main/assets).

    Ejecutar desde la carpeta training:
        cd C:\dev\senas-lsc\training
        powershell -ExecutionPolicy Bypass -File .\entrenar_lsc70.ps1

.PARAMETER Version
    Versión del modelo que se escribe en modelo.json.
.PARAMETER SoloExtraer
    Solo extraer los puntos (lo más lento), sin entrenar.
.PARAMETER SinInstalar
    No copiar el modelo a la app al terminar.
.PARAMETER Arquitectura
    gru (por defecto), conv o transformer. Para compararlas: comparar_lsc70.ps1.
.PARAMETER Cuantizacion
    ninguna (por defecto), float16 o int8. Mira antes outputs\...\optimizacion.md.
#>
param(
    [string]$Version = "0.1.0",
    [int]$Epocas = 80,
    [switch]$SoloExtraer,
    [switch]$SinInstalar,
    [ValidateSet("gru", "conv", "transformer")][string]$Arquitectura = "gru",
    [ValidateSet("ninguna", "float16", "int8")][string]$Cuantizacion = "ninguna"
)

$ErrorActionPreference = "Stop"
$env:PYTHONUTF8 = "1"
$env:TF_CPP_MIN_LOG_LEVEL = "2"
Set-Location $PSScriptRoot

$Datos = "data\LSC70"
$Puntos = "data\puntos"
$Salida = "outputs\lsc70-v$Version"
$Vocabulario = "vocabulario.lsc70.json"
$Cita = "LSC70 (Universidad del Cauca, 2024), CC BY 4.0, doi:10.17632/9ssyn8tff5.2"

function Paso($texto) { Write-Host "`n=== $texto ===" -ForegroundColor Cyan }
function Ejecutar {
    & $Python @args
    if ($LASTEXITCODE -ne 0) { throw "Falló: python $($args -join ' ')" }
}

foreach ($parte in "LSC70AN", "LSC70W") {
    if (-not (Test-Path "$Datos\$parte")) { throw "Falta $Datos\$parte. Descomprime LSC70 en training\data\LSC70 (ver DATOS.md)." }
}

Paso "1. Entorno de Python"
$Python = ".venv\Scripts\python.exe"
if (-not (Test-Path $Python)) {
    # Busca Python 3.13 o 3.12: primero con el lanzador «py», luego «python».
    $exe = $null; $extra = @()
    if (Get-Command py -ErrorAction SilentlyContinue) {
        foreach ($v in "3.13", "3.12") {
            & py "-$v" -c "import sys"
            if ($LASTEXITCODE -eq 0) { $exe = "py"; $extra = @("-$v"); break }
        }
    }
    if (-not $exe -and (Get-Command python -ErrorAction SilentlyContinue)) {
        $ver = & python -c "import sys; print(f'{sys.version_info[0]}.{sys.version_info[1]}')"
        if ($ver -in "3.12", "3.13") { $exe = "python" }
    }
    if (-not $exe) { throw "Necesitas Python 3.12 o 3.13 (https://www.python.org/downloads/). Marca 'Add python.exe to PATH' al instalar." }
    Write-Host "Creando .venv con $exe $extra"
    & $exe @extra -m venv .venv
    if ($LASTEXITCODE -ne 0) { throw "No se pudo crear .venv" }
}
Ejecutar -m pip install --disable-pip-version-check -q -r requirements.txt
Ejecutar -c "import tensorflow, mediapipe, ai_edge_litert; print('Dependencias listas: TensorFlow', tensorflow.__version__, '· MediaPipe', mediapipe.__version__)"

Paso "2. Extraer puntos (la primera vez tarda; se puede interrumpir y continuar)"
Ejecutar extraer.py --raiz "$Datos\LSC70AN" --vocabulario $Vocabulario --salida $Puntos --fuente LSC70AN
Ejecutar extraer.py --raiz "$Datos\LSC70W" --vocabulario $Vocabulario --salida $Puntos --fuente LSC70W
if ($SoloExtraer) { Write-Host "`nPuntos extraídos en $Puntos." -ForegroundColor Green; exit 0 }

Paso "3. Entrenar"
Ejecutar entrenar.py --muestras $Puntos --vocabulario $Vocabulario --salida $Salida --epocas $Epocas --arquitectura $Arquitectura

Paso "4. Evaluar con personas no vistas"
Ejecutar evaluar.py --entrenamiento $Salida --muestras $Puntos

Paso "5. Medir la cuantización"
Ejecutar optimizar.py --entrenamiento $Salida --muestras $Puntos

Paso "6. Exportar"
$exportar = @("exportar.py", "--entrenamiento", $Salida, "--muestras", $Puntos, "--vocabulario", $Vocabulario,
              "--nombre", "reconocedor-lsc70", "--version", $Version, "--datos", $Cita, "--cuantizacion", $Cuantizacion)
if (-not $SinInstalar) { $exportar += "--instalar" }
Ejecutar @exportar

Write-Host "`nListo. Revisa $Salida\informe.md antes de usar el modelo." -ForegroundColor Green
if (-not $SinInstalar) { Write-Host "Después, en C:\dev\senas-lsc: .\gradlew.bat installDebug" -ForegroundColor Green }
