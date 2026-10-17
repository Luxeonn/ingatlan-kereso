# Ingatlan Kereső - Web Dashboard Indító Script
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# Valtsunk a projekt gyokerkonyvtaraba
Set-Location -Path "$PSScriptRoot\.."

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "  Ingatlan Kereső - Web Dashboard Indítása        " -ForegroundColor Yellow
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Java környezet felderítése
if (-not $env:JAVA_HOME) {
    if (Test-Path "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\jbr") {
        $env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\jbr"
    } elseif (Test-Path "C:\Program Files\Java\jdk-21") {
        $env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
    }
}

# Gradle Wrapper felderítése
$gradlew = ".\gradlew.bat"
if (-not (Test-Path $gradlew)) {
    Write-Host "[HIBA] Nem található gradlew.bat a gyökérkönyvtárban!" -ForegroundColor Red
    exit 1
}

Write-Host "-> Java környezet: $env:JAVA_HOME" -ForegroundColor Gray
Write-Host "-> Gradle parancs: $gradlew" -ForegroundColor Gray
Write-Host "-> Alkalmazás indítása és böngésző megnyitása..." -ForegroundColor Green
Write-Host ""

if (Test-Path "build\libs\ingatlan-1.0.jar") {
    & java -jar build\libs\ingatlan-1.0.jar $args
} else {
    & $gradlew run --args="$args"
}
