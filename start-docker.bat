@echo off
setlocal
chcp 65001 >nul
title Ingatlan Kereso - Docker Launcher

echo ==============================================================================
echo                 INGATLAN KERESO - DOCKER LAUNCHER
echo ==============================================================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
    echo [HIBA] A Docker nincs telepitve vagy nem fut!
    echo Kerlek inditsd el a Docker Desktop alkalmazast.
    echo.
    pause
    exit /b 1
)

echo [1/4] Legfrissebb GHCR Docker image ellenorzese es frissitese...
docker pull ghcr.io/luxeonn/ingatlan-kereso:latest

echo.
echo [2/4] Korabbi 'ingatlan-kereso' kontener ellenorzese es eltavolitasa...
docker rm -f ingatlan-kereso >nul 2>&1

echo.
echo [3/4] Kontener inditasa (Nev: ingatlan-kereso, Port: 8080:8080)...
docker run -d ^
  --name ingatlan-kereso ^
  -p 8080:8080 ^
  -v ingatlan_data:/app/data ^
  -v ingatlan_logs:/app/logs ^
  --restart unless-stopped ^
  ghcr.io/luxeonn/ingatlan-kereso:latest

if errorlevel 1 (
    echo.
    echo [HIBA] Nem sikerult elinditani a Docker kontenert!
    pause
    exit /b 1
)

echo.
echo [4/4] Siker! A kontener 'ingatlan-kereso' neven azonnal elerheto.
echo Web Dashboard: http://localhost:8080
echo.
echo Dashboard megnyitasa a bongeszoben...
start http://localhost:8080
ping 127.0.0.1 -n 3 >nul
