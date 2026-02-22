#!/bin/sh
# Gradle wrapper - run 'gradle wrapper' if this fails (requires Gradle installed)
set -e
DIRNAME="$(cd "$(dirname "$0")" && pwd)"
cd "$DIRNAME"
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -jar gradle/wrapper/gradle-wrapper.jar "$@"
fi
echo "Gradle wrapper JAR not found. Run: gradle wrapper"
exit 1
