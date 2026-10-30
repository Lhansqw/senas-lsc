<#
.SYNOPSIS
    Crea la clave con la que se firma la versión de producción de Señas LSC (fase 12). Una sola vez.

.DESCRIPTION
    Genera un almacén de claves (.jks) FUERA del repositorio y escribe keystore.properties en la
    raíz del proyecto (que git ignora). Después, gradlew.bat assembleRelease firma el APK.

    IMPORTANTE: guarda una copia del .jks y de la contraseña en un lugar seguro. Si se pierden, no
    se podrá publicar una actualización de la app que se instale encima de la anterior.

        cd C:\dev\senas-lsc
        powershell -ExecutionPolicy Bypass -File .\scripts\crear_firma.ps1
#>
param(
    [string]$Destino = "$env:USERPROFILE\senas-lsc-firma\senas-lsc.jks",
    [string]$Alias = "senas-lsc",
    [string]$Nombre = "Lhansqw"
)

$ErrorActionPreference = "Stop"
$Raiz = Split-Path $PSScriptRoot -Parent
$Propiedades = Join-Path $Raiz "keystore.properties"

if (Test-Path $Destino) { throw "Ya existe $Destino. No se sobrescribe: si quieres otra clave, elige otro -Destino." }
if (Test-Path $Propiedades) { throw "Ya existe keystore.properties. Bórralo solo si de verdad quieres cambiar de clave." }

# keytool viene con el JDK de Android Studio.
$keytool = $null
$candidatos = @()
if ($env:JAVA_HOME) { $candidatos += Join-Path $env:JAVA_HOME "bin\keytool.exe" }
$candidatos += "$env:ProgramFiles\Android\Android Studio\jbr\bin\keytool.exe"
$candidatos += "$env:LOCALAPPDATA\Programs\Android Studio\jbr\bin\keytool.exe"
foreach ($c in $candidatos) { if (Test-Path $c) { $keytool = $c; break } }
if (-not $keytool) {
    $cmd = Get-Command keytool -ErrorAction SilentlyContinue
    if ($cmd) { $keytool = $cmd.Source }
}
if (-not $keytool) { throw "No se encontró keytool. Instala Android Studio o define JAVA_HOME." }

function Leer-Contrasena($texto) {
    $seguro = Read-Host $texto -AsSecureString
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

$clave = Leer-Contrasena "Contraseña para la clave (mínimo 8 caracteres)"
if ($clave.Length -lt 8) { throw "La contraseña debe tener al menos 8 caracteres." }
if ((Leer-Contrasena "Repite la contraseña") -ne $clave) { throw "Las contraseñas no coinciden." }

New-Item -ItemType Directory -Force -Path (Split-Path $Destino -Parent) | Out-Null
# PKCS12 (el formato por defecto) usa la misma contraseña para el almacén y la clave.
& $keytool -genkeypair -v -keystore $Destino -alias $Alias -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $clave -keypass $clave -dname "CN=$Nombre, O=Senas LSC, C=CO"
if ($LASTEXITCODE -ne 0) { throw "keytool no pudo crear la clave." }

# En .properties la barra invertida es un carácter de escape: se usan barras normales.
$ruta = $Destino -replace '\\', '/'
@(
    "# Firma de la versión de producción. NO subir a git (está en .gitignore).",
    "storeFile=$ruta",
    "storePassword=$clave",
    "keyAlias=$Alias",
    "keyPassword=$clave"
) | Set-Content -Path $Propiedades -Encoding ASCII

Write-Host "`nClave creada en $Destino" -ForegroundColor Green
Write-Host "keystore.properties escrito en $Propiedades" -ForegroundColor Green
Write-Host "Haz ahora una copia de seguridad de $Destino y de la contraseña." -ForegroundColor Yellow
