@echo off
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-EchoModChecker.ps1" -Console
pause
exit /b %errorlevel%
