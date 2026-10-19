@echo off
setlocal
cd /d "%~dp0.."

where docker >nul 2>&1
if errorlevel 1 (
    echo Docker was not found in PATH. Install Docker Desktop and try again.
    exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
    echo Docker Desktop is not running. Start it and try again.
    exit /b 1
)

echo.
echo Select how to start Ingatlan Kereső:
echo   1. Build images from this local project and start them
echo   2. Pull the latest GitHub images and start them
echo.
set /p MODE="Choose 1 or 2: "

if "%MODE%"=="1" goto local_build
if "%MODE%"=="2" goto ghcr_latest
echo Invalid choice.
exit /b 1

:local_build
docker compose up -d --build
if errorlevel 1 exit /b 1
goto show_app

:ghcr_latest
docker compose pull backend frontend
if errorlevel 1 exit /b 1
docker compose up -d --no-build
if errorlevel 1 exit /b 1

:show_app
docker compose ps
echo.
echo Dashboard: http://localhost:8080
start "" http://localhost:8080
endlocal
