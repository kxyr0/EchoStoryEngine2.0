@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy_prism.ps1" %*
exit /b %ERRORLEVEL%
