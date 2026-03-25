#!/usr/bin/env sh
# Start the Spring Boot backend (requires Maven installed)
cd "$(dirname "$0")"
if [ ! -f pom.xml ]; then
  echo "pom.xml not found. Please run this script from the backend folder."
  exit 1
fi

echo "Running backend on http://localhost:8080 ..."

mvn spring-boot:run
