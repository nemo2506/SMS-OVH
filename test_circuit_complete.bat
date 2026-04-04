@echo off
REM Script de test complet du circuit SMS/MMS
REM Teste à la fois le circuit local et l'API distante

setlocal enabledelayedexpansion

title MS-OVH-SMS Circuit Test Suite

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║   TEST COMPLET CIRCUIT SMS/MMS - MS-OVH-SMS v1.0          ║
echo ║   Local Device + API REST                                 ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM Configuration
set PROJECT_PATH=D:\MISESERVICE\apps\MS-OVH-SMS
set API_URL=http://localhost:8080
set BEARER_TOKEN=YOUR_TOKEN_HERE
set TEST_PHONE=+33612345678
set TEST_MESSAGE=Test SMS Circuit

set TESTS_PASSED=0
set TESTS_FAILED=0

REM ═════════════════════════════════════════════════════════════
REM SECTION 1: Tests Locaux
REM ═════════════════════════════════════════════════════════════

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║  SECTION 1: TESTS LOCAUX (Device Android)                 ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM Test 1: Compilation
echo Test 1: Compilation du projet...
cd /d "%PROJECT_PATH%"
call gradlew build -q 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [✓ PASS] Compilation APK
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] Compilation APK
    set /a TESTS_FAILED+=1
)
echo.

REM Test 2: Tests unitaires
echo Test 2: Exécution des tests unitaires...
call gradlew test -q 2>nul
if %ERRORLEVEL% EQU 0 (
    echo [✓ PASS] Tests Unitaires Locaux
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] Tests Unitaires Locaux
    set /a TESTS_FAILED+=1
)
echo.

REM Test 3: Vérifier la présence des fichiers clés
echo Test 3: Vérification des fichiers clés...
set FILE_CHECK=1
if not exist "%PROJECT_PATH%\app\src\main\java\com\miseservice\smsovh\util\SmsHelper.kt" set FILE_CHECK=0
if not exist "%PROJECT_PATH%\app\src\main\java\com\miseservice\smsovh\util\OvhSmsConfig.kt" set FILE_CHECK=0
if not exist "%PROJECT_PATH%\app\src\main\java\com\miseservice\smsovh\service\SmsRestServer.kt" set FILE_CHECK=0

if %FILE_CHECK% EQU 1 (
    echo [✓ PASS] Fichiers Source Clés
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] Fichiers Source Clés
    set /a TESTS_FAILED+=1
)
echo.

REM ═════════════════════════════════════════════════════════════
REM SECTION 2: Tests API REST
REM ═════════════════════════════════════════════════════════════

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║  SECTION 2: TESTS API REST DISTANTE                        ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

REM Test 4: Health Check
echo Test 4: Health Check ^(%API_URL%/api/health^)...
for /f %%i in ('powershell -Command "(Invoke-WebRequest -Uri '%API_URL%/api/health' -Headers @{'Authorization'='Bearer %BEARER_TOKEN%'} -TimeoutSec 5 -UseBasicParsing).StatusCode" 2^>nul') do set HTTP_CODE=%%i

if "%HTTP_CODE%"=="200" (
    echo [✓ PASS] Health Check
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] Health Check ^(HTTP %HTTP_CODE%^)
    set /a TESTS_FAILED+=1
)
echo.

REM Test 5: Auth - No Token
echo Test 5: Authentification - Pas de token...
for /f %%i in ('powershell -Command "(Invoke-WebRequest -Uri '%API_URL%/api/health' -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue).StatusCode" 2^>nul') do set HTTP_CODE=%%i

if "%HTTP_CODE%"=="401" (
    echo [✓ PASS] Auth - No Token
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] Auth - No Token ^(attendu 401, reçu %HTTP_CODE%^)
    set /a TESTS_FAILED+=1
)
echo.

REM Test 6: Send SMS
echo Test 6: Envoi SMS via API...
for /f %%i in ('powershell -Command "(Invoke-WebRequest -Uri '%API_URL%/api/send-sms' -Method POST -Headers @{'Authorization'='Bearer %BEARER_TOKEN%'; 'Content-Type'='application/json'} -Body '{\"senderId\":\"TestScript\",\"destinataire\":\"%TEST_PHONE%\",\"text\":\"%TEST_MESSAGE%\"}' -TimeoutSec 5 -UseBasicParsing).StatusCode" 2^>nul') do set HTTP_CODE=%%i

if "%HTTP_CODE%"=="200" (
    echo [✓ PASS] Send SMS API
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] Send SMS API ^(HTTP %HTTP_CODE%^)
    set /a TESTS_FAILED+=1
)
echo.

REM Test 7: 404 Error
echo Test 7: Test 404 endpoint inexistant...
for /f %%i in ('powershell -Command "(Invoke-WebRequest -Uri '%API_URL%/api/invalid-endpoint' -Headers @{'Authorization'='Bearer %BEARER_TOKEN%'} -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue).StatusCode" 2^>nul') do set HTTP_CODE=%%i

if "%HTTP_CODE%"=="404" (
    echo [✓ PASS] 404 Not Found
    set /a TESTS_PASSED+=1
) else (
    echo [✗ FAIL] 404 Not Found ^(attendu 404, reçu %HTTP_CODE%^)
    set /a TESTS_FAILED+=1
)
echo.

REM ═════════════════════════════════════════════════════════════
REM RÉSUMÉ FINAL
REM ═════════════════════════════════════════════════════════════

echo.
echo ╔════════════════════════════════════════════════════════════╗
echo ║  RÉSUMÉ DES TESTS                                          ║
echo ╚════════════════════════════════════════════════════════════╝
echo.

set /a TOTAL_TESTS=%TESTS_PASSED%+%TESTS_FAILED%
if %TOTAL_TESTS% GTR 0 (
    set /a SUCCESS_PERCENT=%TESTS_PASSED%*100/%TOTAL_TESTS%
) else (
    set SUCCESS_PERCENT=0
)

echo Total Tests: %TOTAL_TESTS%
echo [✓ PASS] Réussis: %TESTS_PASSED%
echo [✗ FAIL] Échoués: %TESTS_FAILED%
echo Taux de réussite: %SUCCESS_PERCENT%%%
echo.

if %TESTS_FAILED% EQU 0 (
    echo ╔════════════════════════════════════════════════════════════╗
    echo ║  🎉 TOUS LES TESTS RÉUSSIS! 🎉                           ║
    echo ║  Circuit SMS/MMS VALIDÉ EN LOCAL ET À DISTANCE            ║
    echo ╚════════════════════════════════════════════════════════════╝
    exit /b 0
) else (
    echo ╔════════════════════════════════════════════════════════════╗
    echo ║  ⚠️  CERTAINS TESTS ONT ÉCHOUÉ ⚠️                        ║
    echo ║  Vérifiez la configuration et les logs                     ║
    echo ╚════════════════════════════════════════════════════════════╝
    exit /b 1
)

endlocal

