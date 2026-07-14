@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

echo ============================================
echo   INK SMS Gateway - Build ^& Package
echo ============================================
echo.

set "PROJECT_ROOT=%~dp0"
set "BACKEND_DIR=%PROJECT_ROOT%backend"
set "FRONT_DIR=%PROJECT_ROOT%front"
set "DEPLOY_DIR=%PROJECT_ROOT%deploy"
:: Generate timestamp via PowerShell (avoids locale issues)
for /f "delims=" %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set "TIMESTAMP=%%i"
set "PACKAGE_NAME=ink-sms-gateway-%TIMESTAMP%"

:: --------------------------------------------------
:: Step 1: Build Backend
:: --------------------------------------------------
echo [1/4] Building backend with Maven...
echo.
cd /d "%BACKEND_DIR%"
call mvn clean package -DskipTests -q
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven build failed!
    pause
    exit /b 1
)
echo [OK] Backend build completed.
echo.

:: --------------------------------------------------
:: Step 2: Build Frontend
:: --------------------------------------------------
echo [2/4] Building frontend...
echo.
cd /d "%FRONT_DIR%"
if not exist node_modules (
    echo Installing npm dependencies...
    call npm install
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] npm install failed!
        pause
        exit /b 1
    )
)
call npm run build
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Frontend build failed!
    pause
    exit /b 1
)
echo [OK] Frontend build completed.
echo.

:: --------------------------------------------------
:: Step 3: Assemble deploy package
:: --------------------------------------------------
echo [3/4] Assembling deploy package...
echo.

:: Clean previous deploy dir
if exist "%DEPLOY_DIR%\%PACKAGE_NAME%" rd /s /q "%DEPLOY_DIR%\%PACKAGE_NAME%"
mkdir "%DEPLOY_DIR%\%PACKAGE_NAME%"
mkdir "%DEPLOY_DIR%\%PACKAGE_NAME%\lib"
mkdir "%DEPLOY_DIR%\%PACKAGE_NAME%\web"
mkdir "%DEPLOY_DIR%\%PACKAGE_NAME%\conf"
mkdir "%DEPLOY_DIR%\%PACKAGE_NAME%\logs"
mkdir "%DEPLOY_DIR%\%PACKAGE_NAME%\bin"

:: Copy backend jars
copy "%BACKEND_DIR%\gateway\target\gateway-1.0.0-SNAPSHOT.jar"       "%DEPLOY_DIR%\%PACKAGE_NAME%\lib\gateway.jar"       >nul 2>&1
copy "%BACKEND_DIR%\user-service\target\user-service-1.0.0-SNAPSHOT.jar" "%DEPLOY_DIR%\%PACKAGE_NAME%\lib\user-service.jar" >nul 2>&1
copy "%BACKEND_DIR%\admin-service\target\admin-service-1.0.0-SNAPSHOT.jar" "%DEPLOY_DIR%\%PACKAGE_NAME%\lib\admin-service.jar" >nul 2>&1
copy "%BACKEND_DIR%\api\target\api-1.0.0-SNAPSHOT.jar"               "%DEPLOY_DIR%\%PACKAGE_NAME%\lib\api.jar"           >nul 2>&1

:: Copy application yml files (for reference/override on server)
for %%s in (gateway user-service admin-service api) do (
  copy "%BACKEND_DIR%\%%s\src\main\resources\application.yml"      "%DEPLOY_DIR%\%PACKAGE_NAME%\conf\%%s-application.yml"       >nul 2>&1
  copy "%BACKEND_DIR%\%%s\src\main\resources\application-prod.yml" "%DEPLOY_DIR%\%PACKAGE_NAME%\conf\%%s-application-prod.yml" >nul 2>&1
)

:: Copy frontend dist
xcopy "%FRONT_DIR%\dist\*" "%DEPLOY_DIR%\%PACKAGE_NAME%\web\" /s /e /q /y >nul 2>&1

:: Copy deploy.sh
copy "%PROJECT_ROOT%deploy.sh" "%DEPLOY_DIR%\%PACKAGE_NAME%\bin\deploy.sh" >nul 2>&1

:: Copy nginx.conf
copy "%PROJECT_ROOT%nginx.conf" "%DEPLOY_DIR%\%PACKAGE_NAME%\conf\nginx.conf" >nul 2>&1

echo [OK] Deploy package assembled at:
echo      %DEPLOY_DIR%\%PACKAGE_NAME%
echo.

:: --------------------------------------------------
:: Step 4: Create tar.gz archive
:: --------------------------------------------------
echo [4/4] Creating archive...
cd /d "%DEPLOY_DIR%"

:: Use tar (available on Windows 10+)
tar -czf "%PACKAGE_NAME%.tar.gz" "%PACKAGE_NAME%"
if %ERRORLEVEL% neq 0 (
    echo [WARN] tar failed, trying PowerShell Compress-Archive...
    powershell -Command "Compress-Archive -Path '%PACKAGE_NAME%\*' -DestinationPath '%PACKAGE_NAME%.zip' -Force"
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Archive creation failed!
        pause
        exit /b 1
    )
    echo [OK] Created: %DEPLOY_DIR%\%PACKAGE_NAME%.zip
) else (
    echo [OK] Created: %DEPLOY_DIR%\%PACKAGE_NAME%.tar.gz
)

echo.
echo ============================================
echo   Build ^& Package Complete!
echo ============================================
echo.
echo Package location:
echo   %DEPLOY_DIR%\%PACKAGE_NAME%.tar.gz
echo.
echo Deploy to server:
echo   scp %DEPLOY_DIR%\%PACKAGE_NAME%.tar.gz user@server:/opt/ink-sms/
echo.
echo On server:
echo   cd /opt/ink-sms
echo   tar -xzf %PACKAGE_NAME%.tar.gz
echo   cd %PACKAGE_NAME%
echo   chmod +x bin/deploy.sh
echo   ./bin/deploy.sh start
echo.
pause
