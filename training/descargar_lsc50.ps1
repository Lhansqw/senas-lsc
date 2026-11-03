<#
.SYNOPSIS
    Descarga LSC50 (Universidad de los Andes) desde Figshare archivo por archivo, con reanudación.

.DESCRIPTION
    «Download all» en el navegador genera un único .zip de 9,9 GB que falla si la conexión se corta.
    Este script pide a la API pública de Figshare la lista de archivos y descarga cada uno con
    curl.exe (incluido en Windows 10 y 11), que continúa donde se quedó. Al terminar comprueba el
    MD5 de cada archivo con el que publica Figshare.

        cd C:\dev\senas-lsc\training
        powershell -ExecutionPolicy Bypass -File .\descargar_lsc50.ps1 -SoloListar   # ver licencia y archivos
        powershell -ExecutionPolicy Bypass -File .\descargar_lsc50.ps1               # descargar todo
        powershell -ExecutionPolicy Bypass -File .\descargar_lsc50.ps1 -Filtro "csv" # solo los que contengan «csv»

.PARAMETER Filtro
    Solo descarga los archivos cuyo nombre contiene este texto.
.PARAMETER SoloListar
    Muestra la licencia y la lista de archivos sin descargar nada.
#>
param(
    [string]$Filtro = "",
    [switch]$SoloListar,
    [string]$Destino = "data\LSC50"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
# Windows PowerShell 5.1 no activa TLS 1.2 por defecto.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

$Articulo = 27383016
$api = "https://api.figshare.com/v2/articles/$Articulo"

Write-Host "Consultando Figshare…" -ForegroundColor Cyan
$info = Invoke-RestMethod -Uri $api -UseBasicParsing
$archivos = Invoke-RestMethod -Uri "$api/files?page_size=1000" -UseBasicParsing

Write-Host "`n$($info.title)"
Write-Host "Licencia: $($info.license.name)  ($($info.license.url))" -ForegroundColor Yellow
Write-Host "DOI: $($info.doi)`n"

$seleccion = @($archivos | Where-Object { $Filtro -eq "" -or $_.name -like "*$Filtro*" })
$total = ($seleccion | Measure-Object -Property size -Sum).Sum
foreach ($a in $seleccion) {
    Write-Host ("  {0,-60} {1,10:N1} MB" -f $a.name, ($a.size / 1MB))
}
Write-Host ("`n{0} archivos, {1:N2} GB" -f $seleccion.Count, ($total / 1GB))
if ($SoloListar) { exit 0 }

$unidad = (Get-Location).Drive
if ($unidad.Free -lt $total * 1.1) {
    throw ("No hay espacio suficiente: hacen falta {0:N1} GB y hay {1:N1} GB libres." -f ($total * 1.1 / 1GB), ($unidad.Free / 1GB))
}

New-Item -ItemType Directory -Force -Path $Destino | Out-Null
$fallidos = @()
$n = 0
foreach ($a in $seleccion) {
    $n++
    $ruta = Join-Path $Destino $a.name
    if ((Test-Path $ruta) -and (Get-Item $ruta).Length -eq $a.size) {
        Write-Host "[$n/$($seleccion.Count)] Ya estaba: $($a.name)"
        continue
    }
    Write-Host "[$n/$($seleccion.Count)] Descargando $($a.name)…" -ForegroundColor Cyan
    $intentos = 0
    do {
        $intentos++
        # -C - continúa una descarga a medias; --retry reintenta cortes de red.
        & curl.exe -L --fail --retry 5 --retry-delay 5 -C - -o $ruta $a.download_url
        $ok = ($LASTEXITCODE -eq 0) -and (Test-Path $ruta) -and ((Get-Item $ruta).Length -eq $a.size)
    } while (-not $ok -and $intentos -lt 3)
    if (-not $ok) { $fallidos += $a.name; continue }

    $md5 = if ($a.computed_md5) { $a.computed_md5 } else { $a.supplied_md5 }
    if ($md5) {
        $calculado = (Get-FileHash $ruta -Algorithm MD5).Hash.ToLower()
        if ($calculado -ne $md5.ToLower()) {
            Write-Host "  MD5 distinto: se borra y habrá que repetir." -ForegroundColor Red
            Remove-Item $ruta
            $fallidos += $a.name
        }
    }
}

if ($fallidos.Count -gt 0) {
    Write-Host "`nNo se pudieron descargar: $($fallidos -join ', '). Vuelve a ejecutar el script: continúa donde quedó." -ForegroundColor Red
    exit 1
}
Write-Host "`nListo: archivos en $Destino. Si hay .zip, descomprímelos ahí mismo." -ForegroundColor Green
