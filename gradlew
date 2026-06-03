#!/bin/sh
APP_HOME="$(cd "$(dirname "$0")/" && pwd -P)"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVACMD" "-Xmx64m" "-Xms64m" -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
