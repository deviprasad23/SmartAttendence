@echo off
REM Start the Spring Boot backend (requires Maven installed)
cd /d "%~dp0"
if not exist pom.xml (
  echo pom.xml not found. Please run this script from the backend folder.
  exit /b 1
)

echo Running backend on http://localhost:8080 ...

mvn spring-boot:run
