@echo off
setlocal

REM ============================================================
REM  CuotiBen - rebuild debug APK
REM  Double click this file to compile.
REM  Output: app\build\outputs\apk\debug\app-debug.apk
REM ============================================================

set "JAVA_HOME=C:\avb\jdk"
set "ANDROID_HOME=C:\avb\sdk"
set "GRADLE_USER_HOME=C:\avb\gradle-home"
set "GRADLE_BIN=C:\avb\gradle\bin\gradle.bat"

cd /d "%~dp0"

if not exist "%JAVA_HOME%\bin\java.exe" goto nojdk
if not exist "%GRADLE_BIN%" goto nogradle

echo.
echo Building CuotiBen...
echo First run takes a few minutes, later runs about 40 seconds.
echo.

call "%GRADLE_BIN%" assembleDebug --console=plain
if errorlevel 1 goto failed

echo.
echo ============================================================
echo  BUILD OK
echo  APK: app\build\outputs\apk\debug\app-debug.apk
echo ============================================================
goto end

:nojdk
echo.
echo [ERROR] JDK not found at %JAVA_HOME%
echo         Portable toolchain missing. Build with Android Studio instead.
goto end

:nogradle
echo.
echo [ERROR] Gradle not found at %GRADLE_BIN%
goto end

:failed
echo.
echo ============================================================
echo  BUILD FAILED - copy the error above and ask for help.
echo ============================================================

:end
echo.
pause
