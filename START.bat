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

rem 2. Gradle Wrapper felderítése
set "GRADLEW_CMD=.\gradlew.bat"
if not exist "gradlew.bat" (
    echo [HIBA] Nem található gradlew.bat a gyökérkönyvtárban!
    pause
    exit /b 1
)
echo [OK] Gradle parancs: %GRADLEW_CMD%
echo.

rem 3. Alkalmazás indítása
echo ==============================================================================
echo Indítás... Nyomj [Ctrl + C]-t a kiszolgáló leállításához.
echo Web Dashboard elérhetőség: http://localhost:8080
echo ==============================================================================
echo.

if exist "build\libs\ingatlan-1.0.jar" (
    java -jar build\libs\ingatlan-1.0.jar %*
) else (
    call %GRADLEW_CMD% run %*
)

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [HIBA] Hiba történt a futtatás során [Hibakód: %ERRORLEVEL%].
    pause
)
