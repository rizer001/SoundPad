@echo off
title Soundpad
cd /d "%~dp0"

:: Use bundled JRE if present, otherwise system Java
if exist "runtime\bin\java.exe" (
    set JAVA=runtime\bin\java.exe
) else (
    set JAVA=java
)

echo Starting Soundpad...
"%JAVA%" -Dfile.encoding=UTF-8 -jar Soundpad.jar
if errorlevel 1 (
    echo.
    echo [ERROR] Soundpad exited with an error.
    pause
)
