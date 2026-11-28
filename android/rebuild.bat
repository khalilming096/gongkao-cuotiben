@echo off
setlocal

REM ============================================================
REM  CuotiBen - rebuild debug APK
REM  Double click this file to compile.
REM  Environment variables are already set on this machine:
REM    JAVA_HOME        JDK 17
REM    ANDROID_HOME     Android SDK root
REM    GRADLE_USER_HOME Gradle dependency cache
REM  Output: app\build\outputs\apk\debug\app-debug.apk
REM ============================================================

cd /d "%~dp0"

if "%JAVA_HOME%"=="" echo [ERROR] JAVA_HOME is not set. & goto end
if "%ANDROID_HOME%"=="" echo [ERROR] ANDROID_HOME is not set. & goto end
if not exist "%JAVA_HOME%\bin\java.exe" goto nojdk
if not exist "%ANDROID_HOME%\platforms\android-34" goto nosdk
if not exist "%~dp0gradlew.bat" goto nowrapper

echo.
echo Building CuotiBen...
echo   JAVA_HOME        = %JAVA_HOME%
echo   ANDROID_HOME     = %ANDROID_HOME%
echo   GRADLE_USER_HOME = %GRADLE_USER_HOME%
echo.
echo First build downloads Gradle 8.7 and dependencies (a few minutes).
echo.

call "%~dp0gradlew.bat" assembleDebug --console=plain
if errorlevel 1 goto failed

echo.
echo ============================================================
echo  BUILD OK
echo  APK: app\build\outputs\apk\debug\app-debug.apk
echo ============================================================
goto end

:nojdk
echo [ERROR] JDK not found at %JAVA_HOME%
goto end

:nosdk
echo [ERROR] Android platform 34 not found at %ANDROID_HOME%
goto end

:nowrapper
echo [ERROR] gradlew.bat not found next to this script.
goto end

:failed
echo.
echo ============================================================
echo  BUILD FAILED - copy the error above and ask for help.
echo ============================================================

:end
echo.
pause
