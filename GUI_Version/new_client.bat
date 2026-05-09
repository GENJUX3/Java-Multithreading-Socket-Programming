@echo off
javac ChatClientApp.java
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)
java ChatClientApp
pause
