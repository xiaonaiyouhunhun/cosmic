@echo off
setlocal EnableExtensions
chcp 65001 >nul

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

powershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%SCRIPT_DIR%\init-codex.ps1" -ProjectRoot "%SCRIPT_DIR%"
set "EXIT_CODE=%ERRORLEVEL%"

if not "%INIT_CODEX_NO_PAUSE%"=="1" (
    echo.
    pause
)

exit /b %EXIT_CODE%
