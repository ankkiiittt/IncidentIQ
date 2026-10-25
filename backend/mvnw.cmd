@echo off
setlocal

set ERROR_CODE=0

if not "%JAVA_HOME%" == "" goto OkJHome

for %%i in (java.exe) do set "JAVACMD=%%~$PATH:i"
goto checkJCmd

:OkJHome
set "JAVACMD=%JAVA_HOME%\bin\java.exe"

:checkJCmd
if exist "%JAVACMD%" goto chkMHome

echo The JAVA_HOME environment variable is not defined correctly. >&2
goto error

:chkMHome
set "DIRNAME=%~dp0"
if "%DIRNAME%" == "" set "DIRNAME=."
set "WRAPPER_JAR=%DIRNAME%.mvn\wrapper\maven-wrapper.jar"
set "WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain"

rem Strip trailing backslash from DIRNAME for the system property
set "PROJECT_DIR=%DIRNAME%"
if "%PROJECT_DIR:~-1%"=="\" set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

"%JAVACMD%" -cp "%WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%PROJECT_DIR%" %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
exit /B %ERROR_CODE%
