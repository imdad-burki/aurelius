#!/bin/bash
# Aurelius build entry point (portable).
# Requires: JDK 17 (JAVA_HOME), Android SDK with platform 37 + build-tools 36
# (ANDROID_HOME), and a gradle.properties with YOUTUBE_API_KEY (see
# gradle.properties.example). Usage: ./build.sh [gradle args...]
# e.g. ./build.sh :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
set -e
cd "$(dirname "$0")"
if [ -z "$JAVA_HOME" ]; then
  if [ -d /usr/lib/jvm/java-17-openjdk-amd64 ]; then
    export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
  else
    echo "ERROR: set JAVA_HOME to a JDK 17 installation." >&2
    exit 1
  fi
fi
if [ -z "$ANDROID_HOME" ]; then
  echo "ERROR: set ANDROID_HOME to your Android SDK location." >&2
  exit 1
fi
exec ./gradlew --no-daemon "$@"
