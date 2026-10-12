@echo off
chcp 65001 >nul
title Ingatlan Kereso - Web Dashboard

echo ==================================================
echo   Ingatlan Kereso - Web Dashboard Inditasa
echo ==================================================
echo.

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-ui.ps1"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Hiba tortent az inditas soran.
    pause
)
