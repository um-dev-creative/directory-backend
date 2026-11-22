# Container images (user guide)

Directory ships two images, built from two files:

| Image | File | Tag | Build time | Start / memory |
|---|---|---|---|---|
| JVM | `Dockerfile` | `lamata/directory-backend:0.0.1` | short (reuses the jar) | JVM start, more memory |
| Native (GraalVM) | `Dockerfile.native` | `lamata/directory-backend:0.0.1-native` | long (compiles from source, ~8 GB of Docker memory) | fast start, less memory |

> The native image builds, but it has not been verified against the real Config Server/Vault/database yet. Use the JVM image for deployments for now.

## Before you build
1. `certs/directory/` must hold the keystores (see [certificates](tls-and-session-security.md)). It is git-ignored; never commit it.
2. Docker with BuildKit (default in current Docker).
3. For the native image: `~/.m2/settings.xml` with the credentials of the private Maven repositories (the `com.umdc.*` artifacts).

## Build
```bash
./docker-build.sh jvm      # packages the jar, then builds Dockerfile
./docker-build.sh native   # builds Dockerfile.native
./docker-build.sh all      # both
```
Optional environment: `IMAGE`, `VERSION`, `MAVEN_SETTINGS`, `INTERNAL_CA` (the internal CA file; skipped when missing).

The settings file and the internal CA are passed as BuildKit **secrets**: they are never stored in an image layer.

## Run
```bash
docker run --rm -p 8197:8197 --env-file config/local.env --env-file secrets/local.env lamata/directory-backend:0.0.1-native
```
Extra options: `JAVA_OPTS` (JVM image) or `NATIVE_OPTS` (native image). `VAULT_ENABLED` defaults to `false`.
Health: `https://127.0.0.1:8197/actuator/health/liveness`.
