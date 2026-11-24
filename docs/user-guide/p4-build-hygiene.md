# Build hygiene (user guide)

For developers and CI operators.

## What changed

- `mvn clean verify` output is clean: the old `[ERROR] An error has occurred` from the OpenAPI plugin is gone. The plugin only works against a running app, so it is skipped by default.
- Coverage is still measured on `mvn clean verify` (report in `target/site/jacoco/`), and the gate (METHOD coverage 85%) is unchanged.
- Your IDE or CI may pass extra JVM arguments with `-DargLine="..."`; they are now added after the coverage agent instead of replacing it.
- SonarCloud reads the coverage report from `target/site/jacoco/jacoco.xml`.

## Commands

| Goal | Command |
| --- | --- |
| Normal build with tests and coverage gate | `mvn -B clean verify` |
| Generate `target/openapi.json` from a running app | start the app, then `mvn verify -Dspringdoc.skip=false` (optional `-Dspringdoc.apiDocsUrl=http://host:port/v3/api-docs`) |
| Run the PostgreSQL migration smoke test (needs Docker) | `mvn -B verify -Ppg-it` |

The `pg-it` profile is never active by default and does not change the 85% gate (JaCoCo is skipped in that profile).
