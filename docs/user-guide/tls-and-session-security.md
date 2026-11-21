# Certificates and authentication (user guide)

Directory now handles TLS certificates and request authentication the same way as smartkin-backend, mercury and zerobound-rest.

## Before you build or run
1. Put these files in `certs/directory/` (the folder is git-ignored; never commit it):
   - `directory.jks`: the service keystore (server certificate and key). **It is not in the repository; request it from whoever issues the service certificates.**
   - `umdc-truststore.jks`: truststore with the internal CA.
   - `prx-internal-ca.crt`: the internal CA, only needed to build the Docker image.
2. Copy `secrets/local.env.example` to `secrets/local.env` and fill in `VAULT_TOKEN`, `SSL_KEYSTORE_PASSWORD`, `SSL_TRUSTSTORE_PASSWORD`. `config/local.env` already names the files.
3. Build the jar (`mvn clean package`) and the image:
   `docker build --secret id=prx_internal_ca,src=certs/directory/prx-internal-ca.crt -t lamata/directory-backend:0.0.1 .`
4. To read the stores from a mounted path instead of the jar, set `SSL_KEYSTORE_URL` / `SSL_TRUSTSTORE_URL` (for example `file:/certs/directory.jks`).

## What to expect
| Situation | Result |
| --- | --- |
| Call to `/api/v1/**` without a valid `session-token` | `401` from the session interceptor (public operations are the ones marked `@SkipSessionValidation`) |
| Call to any other path (except `/actuator/health/**`, `/error`, Swagger) | `403` |
| `certs/directory/` missing at build time | The jar builds but the application cannot start until the stores are provided (classpath or `*_URL`) |
| Image built without the CA secret | Builds; calls to the internal `*.tst` services fail certificate verification |
| `AUTH_CERT_URI` still set | Ignored |
