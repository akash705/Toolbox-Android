#!/bin/sh
# Gradle wrapper script - downloads and runs Gradle
DIRNAME=$(cd "$(dirname "$0")" && pwd)
APP_NAME="Gradle"
CLASSPATH="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
    echo "ERROR: gradle-wrapper.jar not found at $CLASSPATH"
    exit 1
fi

# Determine the Java command
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

exec "$JAVACMD" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
