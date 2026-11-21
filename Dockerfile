# syntax=docker/dockerfile:1

# JVM image of directory-backend. Build the jar first (`mvn -B clean package`), then:
#
#   docker build --secret id=prx_internal_ca,src=certs/directory/prx-internal-ca.crt -t lamata/directory-backend:0.0.1 .
#
# Two stages: the first splits the Spring Boot jar into layers (dependencies / loader / snapshots / application), so a code-only
# change rebuilds one small layer instead of the whole jar; the second is the runtime image.

ARG JAVA_IMAGE=amazoncorretto:25-alpine

FROM ${JAVA_IMAGE} AS layers
WORKDIR /workspace
COPY target/directory-backend.jar directory-backend.jar
RUN java -Djarmode=tools -jar directory-backend.jar extract --layers --destination /workspace/extracted

FROM ${JAVA_IMAGE}
LABEL version="0.0.1"
LABEL description="Directory Backend API"
LABEL maintainer="Luis Mata luis.antonio.mata@gmail.com"

WORKDIR /usr/local/runme

# The keystores of the TLS bundle travel inside the jar (pom.xml, certs/directory). The internal CA, which is git-ignored too, comes in
# as an optional BuildKit secret instead of a COPY from a path that may be missing. When given, it is added to the JVM trust store so
# outbound calls to the *.tst services (Vault, Config Server, backbone, mercury) verify their certificates; without it those calls use
# the default trust store only.
RUN --mount=type=secret,id=prx_internal_ca,target=/run/secrets/prx-internal-ca.crt,required=false \
    if [ -f /run/secrets/prx-internal-ca.crt ]; then \
      keytool -importcert -alias prx-internal-ca -cacerts -file /run/secrets/prx-internal-ca.crt -storepass changeit -noprompt; \
    fi

COPY --from=layers /workspace/extracted/dependencies/ ./
COPY --from=layers /workspace/extracted/spring-boot-loader/ ./
COPY --from=layers /workspace/extracted/snapshot-dependencies/ ./
COPY --from=layers /workspace/extracted/application/ ./
COPY docker-entrypoint.sh docker-entrypoint.sh

RUN addgroup -S appmng && adduser -S jvapps -G appmng \
    && chown -R jvapps:appmng . \
    && chmod -R 740 . \
    && chmod 750 docker-entrypoint.sh

USER jvapps:appmng

EXPOSE 8197
# Liveness over the container's own loopback; the certificate is not verified because it only reaches this process.
HEALTHCHECK --interval=15s --timeout=3s --start-period=90s --retries=5 \
  CMD wget --no-check-certificate -qO- https://127.0.0.1:8197/actuator/health/liveness || exit 1
CMD ["./docker-entrypoint.sh"]
