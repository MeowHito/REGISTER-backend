#!/usr/bin/env bash
#
# Run the membership backend locally against the docker-compose infra.
#
# What it does:
#   1. Forces JDK 17 (Spring Boot 2.7 / Gradle 8.14 do not support JDK 25)
#   2. Activates the "local" Spring profile (uses application-local.yaml)
#   3. Starts the app with ./gradlew bootRun
#
# Prereqs: `docker compose up -d` must already be running (MySQL/Redis/RabbitMQ).
#
set -euo pipefail
cd "$(dirname "$0")"

# --- locate a JDK 17 -------------------------------------------------------
JAVA17=""
if [ -x /usr/libexec/java_home ] && /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  JAVA17="$(/usr/libexec/java_home -v 17)"
elif [ -d /opt/homebrew/opt/openjdk@17 ]; then
  JAVA17="/opt/homebrew/opt/openjdk@17"
elif [ -d /usr/local/opt/openjdk@17 ]; then
  JAVA17="/usr/local/opt/openjdk@17"
fi

if [ -z "$JAVA17" ]; then
  echo "ERROR: JDK 17 not found. Install it first:  brew install openjdk@17" >&2
  exit 1
fi

export JAVA_HOME="$JAVA17"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JAVA_HOME=$JAVA_HOME"
java -version

# --- run -------------------------------------------------------------------
export SPRING_PROFILES_ACTIVE=local
exec ./gradlew bootRun -Dorg.gradle.java.home="$JAVA_HOME" "$@"
