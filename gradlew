#!/bin/sh

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0" 2>/dev/null)
DEFAULT_JVM_OPTS=""
MAX_FD=maximum

warn () { echo "$*"; } >&2
die () { echo "$*"; exit 1; } >&2

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

if [ -n "$JAVA_HOME" ] ; then
    JAVACMD=$JAVA_HOME/bin/java
else
    JAVACMD=java
fi

exec "$JAVACMD" \
  $DEFAULT_JVM_OPTS \
  $JAVA_OPTS \
  $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
