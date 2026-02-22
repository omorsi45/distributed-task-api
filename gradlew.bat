@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off

@rem Find project root
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
cd /d "%DIRNAME%"

:main
@rem Use Gradle from PATH if available
where gradle >nul 2>nul
if %ERRORLEVEL% equ 0 (
  gradle -q wrapper
  goto run
)
if exist ".\gradle\wrapper\gradle-wrapper.jar" goto run
echo Gradle wrapper not fully set up. Run: gradle wrapper
exit /b 1

:run
if exist ".\gradle\wrapper\gradle-wrapper.jar" (
  set GRADLE_OPTS=%GRADLE_OPTS% -Dorg.gradle.appname=%*
  "%~dp0gradle\wrapper\gradle-wrapper.jar" %*
) else (
  gradle %*
)
exit /b %ERRORLEVEL%
