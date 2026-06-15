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
# Returns 0 if $1 is a JDK whose major version is exactly 17.
is_jdk17() {
  [ -x "$1/bin/java" ] || return 1
  "$1/bin/java" -version 2>&1 | grep -Eq 'version "17[.\"]'
}

JAVA17=""
# Try candidates in order, but VERIFY each is really 17. Note: on some machines
# `java_home -v 17` happily returns a newer JDK (e.g. 25) when no real 17 is
# registered, so we cannot trust it without checking the actual version.
for CAND in \
  "$(/usr/libexec/java_home -v 17 2>/dev/null || true)" \
  /opt/homebrew/opt/openjdk@17 \
  /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
  /usr/local/opt/openjdk@17 \
  /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; do
  if [ -n "$CAND" ] && is_jdk17 "$CAND"; then
    JAVA17="$CAND"
    break
  fi
done

if [ -z "$JAVA17" ]; then
  echo "ERROR: real JDK 17 not found. Install it first:  brew install openjdk@17" >&2
  exit 1
fi

export JAVA_HOME="$JAVA17"
export PATH="$JAVA_HOME/bin:$PATH"
echo "Using JAVA_HOME=$JAVA_HOME"
java -version

# --- run -------------------------------------------------------------------
export SPRING_PROFILES_ACTIVE=local
exec ./gradlew bootRun -Dorg.gradle.java.home="$JAVA_HOME" "$@"
