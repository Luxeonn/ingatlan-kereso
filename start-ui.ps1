# Ingatlan Kereső - Web Dashboard Indító Script
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

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

# Maven bináris felderítése
$mvn = "mvn"
if (-not (Get-Command "mvn" -ErrorAction SilentlyContinue)) {
    if (Test-Path "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd") {
        $mvn = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    }
}

Write-Host "-> Java környezet: $env:JAVA_HOME" -ForegroundColor Gray
Write-Host "-> Maven parancs: $mvn" -ForegroundColor Gray
Write-Host "-> Alkalmazás indítása és böngésző megnyitása..." -ForegroundColor Green
Write-Host ""

& $mvn compile exec:java $args
