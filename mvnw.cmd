@echo off
REM Maven Wrapper (runs the wrapper main class directly when jar has no Main-Class)
SET SCRIPT_DIR=%~dp0
REM Trim trailing backslash from SCRIPT_DIR for safe quoting
IF "%SCRIPT_DIR:~-1%"=="\" SET "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
REM Ensure Maven wrapper knows the project base directory
SET "MAVEN_PROJECTBASEDIR=%SCRIPT_DIR%"
IF DEFINED JAVA_HOME (
  SET "JAVA_CMD=%JAVA_HOME%\bin\java"
) ELSE (
  SET "JAVA_CMD=java"
)
"%JAVA_CMD%" -Dmaven.multiModuleProjectDirectory="%SCRIPT_DIR%" -cp "%SCRIPT_DIR%\.mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain %*
