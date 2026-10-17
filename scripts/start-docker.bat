@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul
title Ingatlan Kereso - Docker Compose Launcher

cd /d "%~dp0.."

echo ==============================================================================
echo                 INGATLAN KERESO - DOCKER COMPOSE LAUNCHER
echo ==============================================================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
    echo [HIBA] A Docker nincs telepitve vagy nem talalhato a PATH-ban!
    echo Kerlek telepitsd es inditsd el a Docker Desktop alkalmazast.
    echo.
    pause
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo [HIBA] A Docker Daemon nem fut!
    echo Kerlek inditsd el a Docker Desktop alkalmazast es vard meg amig befejezi az inicializalast.
    echo.
    pause
    exit /b 1
)

echo [1/4] Korabbi monolitikus 'ingatlan-kereso' kontener eltavolitasa (ha letezik)...
docker rm -f ingatlan-kereso >nul 2>&1

echo.
echo [2/4] Docker Compose szolgalatasok elokeszitese es inditasa...
docker compose pull
if errorlevel 1 (
    echo [INFO] GHCR image-ek letoltese sikertelen vagy lokalis build szukseges. Helyi epites...
    docker compose up -d --build
) else (
    docker compose up -d
)

if errorlevel 1 (
    echo.
    echo [HIBA] Nem sikerult elinditani a Docker Compose szolgaltatasokat!
    echo Ellenorizd a hibauzeneteket felette, vagy futtasd a 'docker compose logs' parancsot.
    pause
    exit /b 1
)

echo.
echo [3/4] Varakozas a rendszer (PostgreSQL, Backend, Nginx Frontend) feleledesere...
set /a ATTEMPTS=0
:WAIT_LOOP
set /a ATTEMPTS+=1
if !ATTEMPTS! GTR 30 (
    echo [FIGYELEM] Az inditas hosszabb idot vesz igenybe a vartnal.
    goto OPEN_BROWSER
)

curl.exe -s -f http://localhost:8080/ >nul 2>&1
if errorlevel 1 (
    ping 127.0.0.1 -n 2 >nul
    goto WAIT_LOOP
)

:OPEN_BROWSER
echo.
echo [4/4] SIKER! A rendszer uzemkesz.
echo ==============================================================================
echo Web Dashboard:  http://localhost:8080
echo REST API:       http://localhost:8080/api/status
echo ==============================================================================
echo.
echo Dashboard megnyitasa az alapertelmezett bongeszoben...
start http://localhost:8080
ping 127.0.0.1 -n 3 >nul
