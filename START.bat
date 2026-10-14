@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Ingatlan Kereso - Web Dashboard

echo ==============================================================================
echo                      INGATLAN KERESŐ - WEB DASHBOARD
echo ==============================================================================
echo.

rem 1. Java 21 környezet felderítése
if not defined JAVA_HOME (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\jbr" (
        set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\jbr"
    ) else if exist "C:\Program Files\Java\jdk-21" (
        set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    )
)

if defined JAVA_HOME (
    set "PATH=%JAVA_HOME%\bin;%PATH%"
    echo [OK] Java környezet: %JAVA_HOME%
) else (
    where java >nul 2>&1
    if errorlevel 1 (
        echo [HIBA] Nem található Java futtatókörnyezet [Java 21 LTS szükséges]!
        echo Kérlek telepítsd a Java 21-et vagy állítsd be a JAVA_HOME változót.
        pause
        exit /b 1
    )
    echo [OK] Rendszer Java használata.
)

rem 2. Maven bináris felderítése
set "MVN_CMD=mvn"
where mvn >nul 2>&1
if errorlevel 1 (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd" (
        set "MVN_CMD=C:\Program Files\JetBrains\IntelliJ IDEA 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    ) else (
        echo [HIBA] Nem található Apache Maven a rendszerben!
        pause
        exit /b 1
    )
)
echo [OK] Maven parancs: %MVN_CMD%
echo.

rem 3. Alkalmazás indítása
echo ==============================================================================
echo Indítás... Nyomj [Ctrl + C]-t a kiszolgáló leállításához.
echo Web Dashboard elérhetőség: http://localhost:8080
echo ==============================================================================
echo.

if exist "target\ingatlan-1.0.jar" (
    java -jar target\ingatlan-1.0.jar %*
) else (
    "%MVN_CMD%" compile exec:java %*
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [HIBA] Hiba történt a futtatás során [Hibakód: %ERRORLEVEL%].
    pause
)
