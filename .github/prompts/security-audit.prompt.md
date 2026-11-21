---
name: Security Audit
description: Run a security audit on directory-backend dependencies, JWT flow, and OWASP Top 10 concerns.
mode: agent
agent: security-reviewer
tools: [run_in_terminal, read_file, grep_search, file_search, validate_cves]
---

# Security Audit

## Input Variables

- `${auditScope}` — scope of audit: `full` | `deps-only` | `jwt-only` | `api-only`

## Steps

### Always: Dependency CVE Scan

```bash
mvn org.owasp:dependency-check-maven:check
# Report: target/dependency-check-report.html
```

Use `validate_cves` tool for any flagged dependencies to get CVE details and minimum safe version.

### If `${auditScope}` = `full` or `jwt-only`

1. Read `src/main/java/com/prx/directory/security/SessionJwtServiceImpl.java`.
2. Read `src/main/java/com/prx/directory/util/JwtUtil.java`.
3. Check:
   - Signing algorithm (prefer RS256/ES256; flag HS256 with shared secret).
   - Token expiry is set and enforced.
   - Claims validated (`iss`, `sub`, `exp`, `aud`).
   - Tokens not logged.

### If `${auditScope}` = `full` or `api-only`

4. Review controller endpoints for:
   - Missing authentication checks (`@PreAuthorize` or Spring Security config).
   - Input validation — flag unvalidated path/query params.
   - Stack traces in error responses (information leakage).

### If `${auditScope}` = `full`

5. Check Feign clients (`BackboneClient`, `MercuryClient`) — TLS enforced, tokens not logged.
6. Check Kafka producer (`EmailMessageProducerServiceImpl`) — no PII in message payloads.
7. Check `bootstrap.yml` — secrets sourced from Vault, not hardcoded.

## Constraints

- Do not modify `*.crt` or `keystore.jks` — report only.
- Do not expose CVE details in public commit messages.
- Never commit secrets.

## Output Format

```markdown
## Security Audit Report — ${auditScope}

### Dependency CVEs
| Dependency | CVE | Severity | Current | Fix Version |
|------------|-----|----------|---------|-------------|

### JWT Findings
| Check | Status | Notes |
|-------|--------|-------|

### API Security Findings
| Endpoint | Issue | Severity | Recommendation |
|----------|-------|----------|----------------|

### Other Findings
| Area | Issue | Severity |
|------|-------|----------|

### Overall Risk: LOW / MEDIUM / HIGH

### Recommended Actions
1. item
```

