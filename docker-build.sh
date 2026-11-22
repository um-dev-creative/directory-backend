#!/bin/sh
# Builds the container images of directory-backend.
#
#   ./docker-build.sh jvm      # Dockerfile        -> lamata/directory-backend:<version>         (packages the jar first)
#   ./docker-build.sh native   # Dockerfile.native -> lamata/directory-backend:<version>-native  (compiles from source, slow)
#   ./docker-build.sh all      # both
#
# Environment: IMAGE (default lamata/directory-backend), VERSION (default 0.0.1), MAVEN_SETTINGS (default ~/.m2/settings.xml),
# INTERNAL_CA (default certs/directory/prx-internal-ca.crt; optional, skipped when the file is missing).
set -eu

IMAGE="${IMAGE:-lamata/directory-backend}"
VERSION="${VERSION:-0.0.1}"
MAVEN_SETTINGS="${MAVEN_SETTINGS:-$HOME/.m2/settings.xml}"
INTERNAL_CA="${INTERNAL_CA:-certs/directory/prx-internal-ca.crt}"

ca_args=""
if [ -f "$INTERNAL_CA" ]; then
  ca_args="--secret id=prx_internal_ca,src=$INTERNAL_CA"
fi

build_jvm() {
  ./mvnw -B -DskipTests package
  # shellcheck disable=SC2086
  docker build -f Dockerfile $ca_args -t "$IMAGE:$VERSION" .
}

build_native() {
  [ -f "$MAVEN_SETTINGS" ] || { echo "Maven settings not found: $MAVEN_SETTINGS (private repositories need it)" >&2; exit 1; }
  # shellcheck disable=SC2086
  docker build -f Dockerfile.native --secret "id=maven_settings,src=$MAVEN_SETTINGS" $ca_args -t "$IMAGE:$VERSION-native" .
}

case "${1:-}" in
  jvm) build_jvm ;;
  native) build_native ;;
  all) build_jvm; build_native ;;
  *) echo "usage: $0 jvm|native|all" >&2; exit 2 ;;
esac
