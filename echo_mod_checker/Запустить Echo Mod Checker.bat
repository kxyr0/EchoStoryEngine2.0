@echo off
chcp 65001 >nul
setlocal
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-EchoModChecker.ps1"
if not errorlevel 1 exit /b 0
echo Echo Mod Checker failed to start. See the console output above.
pause
exit /b 1
