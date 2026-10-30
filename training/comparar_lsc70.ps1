<#
.SYNOPSIS
    Fase 11: entrena las tres arquitecturas con LSC70 y las compara (GRU, convolucional, Transformer).

.DESCRIPTION
    Usa los puntos ya extraídos por entrenar_lsc70.ps1 (data\puntos) y el mismo entorno .venv.
    Para cada arquitectura: entrena, evalúa con las mismas personas de prueba y mide la
    cuantización. Al final escribe outputs\comparacion\comparacion.md. No instala nada en la app.

        cd C:\dev\senas-lsc\training
        powershell -ExecutionPolicy Bypass -File .\comparar_lsc70.ps1
#>
param(
    [string[]]$Arquitecturas = @("gru", "conv", "transformer"),
    [int]$Epocas = 80
)

$ErrorActionPreference = "Stop"
$env:PYTHONUTF8 = "1"
$env:TF_CPP_MIN_LOG_LEVEL = "2"
Set-Location $PSScriptRoot

$Python = ".venv\Scripts\python.exe"
$Puntos = "data\puntos"
$Vocabulario = "vocabulario.lsc70.json"
$Raiz = "outputs\comparacion"

if (-not (Test-Path $Python)) { throw "Falta .venv. Ejecuta primero entrenar_lsc70.ps1." }
if (-not (Test-Path $Puntos)) { throw "Falta $Puntos. Ejecuta primero entrenar_lsc70.ps1 (o con -SoloExtraer)." }

function Ejecutar {
    & $Python @args
    if ($LASTEXITCODE -ne 0) { throw "Falló: python $($args -join ' ')" }
}

$carpetas = @()
foreach ($a in $Arquitecturas) {
    $salida = "$Raiz\$a"
    Write-Host "`n=== $a ===" -ForegroundColor Cyan
    # Misma semilla en todas: misma partición de personas, comparación justa.
    Ejecutar entrenar.py --muestras $Puntos --vocabulario $Vocabulario --salida $salida --arquitectura $a --epocas $Epocas --semilla 42
    Ejecutar evaluar.py --entrenamiento $salida --muestras $Puntos
    Ejecutar optimizar.py --entrenamiento $salida --muestras $Puntos
    $carpetas += $salida
}

Write-Host "`n=== Comparación ===" -ForegroundColor Cyan
Ejecutar comparar.py @carpetas --salida "$Raiz\comparacion.md"
Write-Host "Tabla en $Raiz\comparacion.md. Para instalar una: python exportar.py --entrenamiento $Raiz\<arquitectura> ..." -ForegroundColor Green
