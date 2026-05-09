@echo off
javac Server.java ClientHandler.java
if %errorlevel% neq 0 (
    echo Compilation failed.
    pause
    exit /b %errorlevel%
)
java Server
pause
