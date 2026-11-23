# Dependency update (Spring Boot 4.1.2) — user guide

**For whom:** anyone who builds, runs or deploys `directory-backend`.

## What changed for you

The backend now uses the same platform versions as `backbone-rest`, `mercury` and `zerobound-rest`. The REST API, its URLs and its responses are **the same**: this is a maintenance update with no new feature and no contract change.

## What you need

| Item | Value |
|------|-------|
| JDK | 25 (unchanged) |
| Maven | 3.9+ (`mvn -B clean verify`) |
| UMDC libraries | `commons` 0.0.4, `commons-services` 0.0.3, `security-oauth` 0.0.3, resolved from your local `~/.m2` or the Repsy repository |

## What to do

1. `git pull` and rebuild: `mvn -B clean verify`.
2. Deploy as usual. No configuration, database or environment-variable change is needed.

## Why

Several libraries had security advisories (Tomcat, Jackson 2 and 3, logback, hibernate-validator, Apache `httpclient`). The new versions close them, as they already do in `backbone-rest`.
