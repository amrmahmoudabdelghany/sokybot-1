@echo off
rem ============================================================================
rem Sokybot Desktop Launcher for Windows
rem ============================================================================
rem This script launches the Sokybot application with bundled or system JRE.
rem
rem Environment Variables:
rem   JAVA_HOME     - Path to JRE/JDK (optional if bundled JRE exists)
rem   SOKYBOT_OPTS  - Additional JVM options
rem   SOKYBOT_DEBUG - Set to "true" to enable debug output
rem ============================================================================

setlocal enabledelayedexpansion

set "APP_NAME=${app.name}"
set "APP_VERSION=${project.version}"

rem Determine the application home directory
set "SCRIPT_DIR=%~dp0"
set "APP_HOME=%SCRIPT_DIR%"

rem Remove trailing backslash
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"

echo Starting %APP_NAME% v%APP_VERSION%...

rem Check for bundled JRE first
if exist "%APP_HOME%\jre\bin\java.exe" (
    set "JAVA_CMD=%APP_HOME%\jre\bin\java.exe"
    if defined SOKYBOT_DEBUG echo Using bundled JRE: %JAVA_CMD%
) else if defined JAVA_HOME (
    set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    if defined SOKYBOT_DEBUG echo Using JAVA_HOME: %JAVA_CMD%
) else (
    rem Try to find java in PATH
    where java >nul 2>&1
    if %ERRORLEVEL% equ 0 (
        set "JAVA_CMD=java"
        if defined SOKYBOT_DEBUG echo Using java from PATH
    ) else (
        echo ERROR: No JRE found!
        echo Please either:
        echo   1. Bundle a JRE in the 'jre' folder
        echo   2. Set JAVA_HOME environment variable
        echo   3. Add java to your PATH
        pause
        exit /b 1
    )
)

rem Verify Java exists
if not "%JAVA_CMD%"=="java" (
    if not exist "%JAVA_CMD%" (
        echo ERROR: Java not found at %JAVA_CMD%
        pause
        exit /b 1
    )
)

rem Set Karaf home
set "KARAF_HOME=%APP_HOME%\runtime"

rem Set default JVM options
set "DEFAULT_OPTS=-Xms256m -Xmx1024m"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.home=%KARAF_HOME%"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.base=%KARAF_HOME%"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.data=%KARAF_HOME%\data"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.etc=%KARAF_HOME%\etc"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.instances=%KARAF_HOME%\instances"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.startLocalConsole=false"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Dkaraf.startRemoteShell=false"
set "DEFAULT_OPTS=%DEFAULT_OPTS% -Djava.util.logging.config.file=%KARAF_HOME%\etc\java.util.logging.properties"

rem Add user options
if defined SOKYBOT_OPTS (
    set "JAVA_OPTS=%DEFAULT_OPTS% %SOKYBOT_OPTS%"
) else (
    set "JAVA_OPTS=%DEFAULT_OPTS%"
)

rem Build classpath
set "CLASSPATH=%KARAF_HOME%\system\org\apache\karaf\org.apache.karaf.main\${karaf.version}\org.apache.karaf.main-${karaf.version}.jar"
set "CLASSPATH=%CLASSPATH%;%KARAF_HOME%\system\org\apache\karaf\org.apache.karaf.specs.activator\${karaf.version}\org.apache.karaf.specs.activator-${karaf.version}.jar"

rem Display debug info
if defined SOKYBOT_DEBUG (
    echo.
    echo Debug Information:
    echo   APP_HOME:   %APP_HOME%
    echo   KARAF_HOME: %KARAF_HOME%
    echo   JAVA_CMD:   %JAVA_CMD%
    echo   JAVA_OPTS:  %JAVA_OPTS%
    echo.
)

rem Launch the application
cd /d "%KARAF_HOME%"
"%JAVA_CMD%" %JAVA_OPTS% -cp "%CLASSPATH%" ${app.main.class}

rem Capture exit code
set "EXIT_CODE=%ERRORLEVEL%"

if %EXIT_CODE% neq 0 (
    echo.
    echo Application exited with code: %EXIT_CODE%
    pause
)

exit /b %EXIT_CODE%
