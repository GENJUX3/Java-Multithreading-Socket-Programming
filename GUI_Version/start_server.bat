@echo off
javac ChatServerApp.java
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)
java ChatServerApp
pause
