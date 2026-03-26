#!/usr/bin/env sh
# ----------------------------------------------------------------------------
# Maven Wrapper (runs the wrapper main class directly)
# ----------------------------------------------------------------------------

# Determine the directory of this script
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"

# Ensure Maven wrapper knows which directory is the project root
export MAVEN_PROJECTBASEDIR="$SCRIPT_DIR"

JAVA_CMD="java"
if [ -n "$JAVA_HOME" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

exec "$JAVA_CMD" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" -cp "$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain "$@"
