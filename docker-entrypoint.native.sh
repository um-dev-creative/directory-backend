#!/bin/sh
# Entrypoint of the native image. `exec` makes the executable PID 1, so `docker stop` reaches it as SIGTERM and Spring shuts down
# gracefully. There is no JVM: only the options a native executable understands are passed (no -XX:+UseCompactObjectHeaders, -XX:MaxRAMPercentage or -jar).
#
# The truststore is the JDK cacerts plus the optional internal CA, built at image build time (Dockerfile.native). The TLS keystores
# travel inside the executable as classpath resources, like in the JVM image.
#
# NATIVE_OPTS is appended last, so a deployment can override any default below, e.g. NATIVE_OPTS="-Xmx512m" (this build has no -XX:MaxRAMPercentage).
set -e

exec ./directory-backend \
    -Djavax.net.ssl.trustStore=/usr/local/runme/truststore.jks \
    -Djavax.net.ssl.trustStorePassword=changeit \
    -Dspring.cloud.vault.enabled="${VAULT_ENABLED:-false}" \
    -Dspring.application.name=directory-backend \
    -Dapi.info.version=1.0.0 \
    ${NATIVE_OPTS}
