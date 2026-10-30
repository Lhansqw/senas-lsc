<#
.SYNOPSIS
    Genera la versión instalable de Señas LSC (fase 12): pruebas, APK y AAB firmados, comprobados.

.DESCRIPTION
    1. Comprueba que hay firma (keystore.properties) y modelo de señas en la app.
    2. Ejecuta todas las pruebas.
    3. Compila el APK (para instalar directamente) y el AAB (para Google Play) de producción.
    4. Verifica la firma con apksigner y copia los archivos a dist\ con su SHA-256.

        cd C:\dev\senas-lsc
        powershell -ExecutionPolicy Bypass -File .\scripts\version_instalable.ps1

.PARAMETER SinR8
    Compilar sin R8 (para descartar que R8 sea la causa de un fallo).
.PARAMETER SinPruebas
    Saltar las pruebas (no recomendado).
#>
param(
    [switch]$SinR8,
    [switch]$SinPruebas
)

$ErrorActionPreference = "Stop"
$Raiz = Split-Path $PSScriptRoot -Parent
Set-Location $Raiz

function Paso($texto) { Write-Host "`n=== $texto ===" -ForegroundColor Cyan }
function Gradle {
    & .\gradlew.bat @args
    if ($LASTEXITCODE -ne 0) { throw "Falló: gradlew $($args -join ' ')" }
}
function Propiedad($archivo, $clave) {
    $linea = Get-Content $archivo | Where-Object { $_ -match "^\s*$([regex]::Escape($clave))\s*=" } | Select-Object -First 1
    if ($linea) { return ($linea -split "=", 2)[1].Trim() }
    return $null
}

Paso "1. Comprobaciones"
if (-not (Test-Path "keystore.properties")) {
    throw "Falta keystore.properties. Crea la firma una vez con: powershell -ExecutionPolicy Bypass -File .\scripts\crear_firma.ps1"
}
$assets = "recognizer\litert\src\main\assets"
foreach ($f in "reconocedor_lsc.tflite", "modelo.json") {
    if (-not (Test-Path "$assets\$f")) {
        throw "Falta $assets\$f. Entrena e instala el modelo con training\entrenar_lsc70.ps1."
    }
}
$version = Propiedad "gradle.properties" "senas.versionName"
$codigo = Propiedad "gradle.properties" "senas.versionCode"
Write-Host "Versión $version (versionCode $codigo)"
Write-Host "Modelo de señas: $((Get-Content "$assets\modelo.json" -Raw | ConvertFrom-Json).nombre) $((Get-Content "$assets\modelo.json" -Raw | ConvertFrom-Json).version)"

if (-not $SinPruebas) {
    Paso "2. Pruebas"
    Gradle test
}

Paso "3. Compilar APK y AAB de producción"
$extra = @()
if ($SinR8) { $extra += "-Psenas.r8=false" }
Gradle assembleRelease bundleRelease @extra

$apk = "app\build\outputs\apk\release\app-release.apk"
$aab = "app\build\outputs\bundle\release\app-release.aab"
if (-not (Test-Path $apk)) { throw "No se generó $apk (¿falta la firma?)." }

Paso "4. Verificar la firma"
$sdk = (Propiedad "local.properties" "sdk.dir")
if ($sdk) { $sdk = $sdk -replace '\\:', ':' -replace '\\\\', '\' }
if (-not $sdk) { $sdk = $env:ANDROID_HOME }
$apksigner = Get-ChildItem "$sdk\build-tools" -Directory -ErrorAction SilentlyContinue |
    Sort-Object { [version]($_.Name -replace '[^0-9.]', '') } -Descending |
    ForEach-Object { Join-Path $_.FullName "apksigner.bat" } |
    Where-Object { Test-Path $_ } | Select-Object -First 1
if ($apksigner) {
    & $apksigner verify --print-certs $apk
    if ($LASTEXITCODE -ne 0) { throw "La firma del APK no es válida." }
} else {
    Write-Host "No se encontró apksigner en $sdk\build-tools: se omite la verificación." -ForegroundColor Yellow
}

Paso "5. Copiar a dist\"
New-Item -ItemType Directory -Force -Path dist | Out-Null
Remove-Item "dist\SHA256SUMS.txt" -ErrorAction SilentlyContinue
$destApk = "dist\senas-lsc-$version.apk"
$destAab = "dist\senas-lsc-$version.aab"
Copy-Item $apk $destApk -Force
if (Test-Path $aab) { Copy-Item $aab $destAab -Force }
foreach ($f in $destApk, $destAab) {
    if (Test-Path $f) {
        $mb = [math]::Round((Get-Item $f).Length / 1MB, 1)
        $hash = (Get-FileHash $f -Algorithm SHA256).Hash.ToLower()
        Write-Host ("{0}  {1} MB  SHA-256 {2}" -f $f, $mb, $hash)
        "$hash  $(Split-Path $f -Leaf)" | Add-Content "dist\SHA256SUMS.txt"
    }
}
Write-Host "`nListo. Instala en un teléfono conectado con: adb install -r $destApk" -ForegroundColor Green
Write-Host "Después sigue la lista de comprobación de docs\fase-12-version-instalable.md." -ForegroundColor Green
