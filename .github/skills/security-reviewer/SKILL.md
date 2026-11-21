---
name: Security Reviewer Skills
description: Consolidated skill set for security analysis in directory-backend
applies-to: [Security Reviewer]
---

# Security Reviewer — Skill Definition

## 1. Project-Specific Patterns

- Dependency scanning: `mvn org.owasp:dependency-check-maven:check` — outputs `target/dependency-check-report.html`.
- JWT: `SessionJwtServiceImpl.java` + `JwtUtil.java` — validate token signing algorithm, expiry, scope.
- Auth flow: `AuthServiceImpl.java` handles session token generation.
- Feign clients (`BackboneClient`, `MercuryClient`): validate that tokens are not logged and TLS is enforced.
- Config: secrets loaded via HashiCorp Vault (`bootstrap.yml`) — never in `application.yml` or code.
- SSL: certificates in `src/main/resources/*.crt` and `keystore.jks` — do not modify unless requested.
- Kafka: outbound email via `EmailMessageProducerServiceImpl` — validate no PII leakage in payloads.

## 2. Naming Conventions

- Security classes: `*ServiceImpl.java` in `com.umdc.directory.security`, `com.umdc.directory.services`.
- CVE reports reference `pom.xml` dependency `groupId:artifactId:version`.

## 3. Error Handling

- Flag: JWT validation errors not properly caught → information leakage.
- Flag: stack traces exposed in API error responses.
- Flag: credentials/tokens in log statements.

## 4. Key Files

- `src/main/java/com/prx/directory/security/SessionJwtServiceImpl.java`
- `src/main/java/com/prx/directory/util/JwtUtil.java`
- `src/main/java/com/prx/directory/services/AuthServiceImpl.java`
- `src/main/java/com/prx/directory/kafka/producer/EmailMessageProducerServiceImpl.java`
- `src/main/resources/bootstrap.yml` — Vault config
- `pom.xml` — dependency versions to audit
- `.github/tools/dependency-check.tool.md`

## 5. Constraints

- Do not modify `*.crt` or `keystore.jks` unless explicitly requested.
- Do not expose CVE details in public commit messages.

## 6. Checklist

- [ ] `mvn dependency-check:check` passes or CVEs triaged
- [ ] No hardcoded secrets/tokens in source files
- [ ] JWT signing algorithm is asymmetric (RS256/ES256) or documented if HS256
- [ ] Token expiry validated in `SessionJwtServiceImpl`
- [ ] Feign client calls use TLS — `BackboneFeignConfigurer`, `MercuryFeignConfigurer`
- [ ] No PII in Kafka payloads (`EmailMessageProducerServiceImpl`)
- [ ] OWASP Top 10 items reviewed for affected endpoints

