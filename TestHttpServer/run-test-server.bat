@echo off
REM Transaction Rollback Test Server Runner
REM This script starts a test HTTP server that simulates failures to test JMS transaction rollback

echo Transaction Rollback Test Server
echo ==================================
echo.

REM Check if Python is available
python --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Python is not installed or not in PATH
    echo Please install Python 3.6+ and ensure it's in your PATH
    pause
    exit /b 1
)

echo Available test modes:
echo.
echo 1. Demo Mode - 30%% failure rate with 500ms delay
echo 2. High Failure - 70%% failure rate for stress testing  
echo 3. Custom Mode - Specify your own parameters
echo 4. Reliable Mode - No failures, for baseline testing
echo.

set /p choice="Select mode (1-4): "

if "%choice%"=="1" (
    echo Starting Demo Mode...
    python transaction-test-server.py --demo
) else if "%choice%"=="2" (
    echo Starting High Failure Mode...
    python transaction-test-server.py --fail-rate 0.7 --delay-ms 1000
) else if "%choice%"=="3" (
    set /p fail_rate="Enter failure rate (0.0-1.0): "
    set /p delay="Enter delay in ms (0-5000): "
    echo Starting Custom Mode...
    python transaction-test-server.py --fail-rate %fail_rate% --delay-ms %delay%
) else if "%choice%"=="4" (
    echo Starting Reliable Mode...
    python transaction-test-server.py --fail-rate 0.0 --delay-ms 100
) else (
    echo Invalid choice. Starting Demo Mode...
    python transaction-test-server.py --demo
)

echo.
echo Test server stopped.
pause
