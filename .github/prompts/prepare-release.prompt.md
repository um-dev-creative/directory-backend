---
name: Prepare Release
description: Validate, tag, and publish a new release of directory-backend.
mode: agent
agent: devops-engineer
tools: [run_in_terminal, read_file, replace_string_in_file, grep_search, file_search]
---

# Prepare Release

## Input Variables

- `${releaseVersion}` — new version tag (e.g., `0.2.0` — without `v` prefix; the tag will be `v0.2.0`)
- `${releaseNotes}` — summary of changes for this release (markdown list)

## Steps

1. **Validate build**:
   ```bash
   mvn -B -V -e clean verify
   ```
   Assert: BUILD SUCCESS, JaCoCo ≥ 80%, PMD 0 violations.

2. **Update `pom.xml` version**:
   ```bash
   mvn versions:set -DnewVersion=${releaseVersion} -DgenerateBackupPoms=false
   ```

3. **Update `CHANGELOG.md`**:
   Add a new section:
   ```markdown
   ## [${releaseVersion}] - YYYY-MM-DD
   ${releaseNotes}
   ```

4. **Commit version bump**:
   ```bash
   git add pom.xml CHANGELOG.md
   git commit -m "chore(release): bump version to ${releaseVersion}"
   ```

5. **Create annotated tag**:
   ```bash
   git tag -a v${releaseVersion} -m "Release v${releaseVersion}"
   git push origin main --tags
   ```

6. **Build Docker image**:
   ```bash
   docker build -t prx/directory-backend:${releaseVersion} .
   docker tag prx/directory-backend:${releaseVersion} prx/directory-backend:latest
   ```

7. **Publish GitHub release**:
   ```bash
   gh release create v${releaseVersion} \
     --title "v${releaseVersion}" \
     --notes "${releaseNotes}" \
     target/directory-backend.jar
   ```

## Constraints

- Never create a release tag without `mvn -B -V -e clean verify` passing.
- Never commit secrets or tokens.
- Do not modify `*.crt` or `keystore.jks` during release preparation.
- Always update `CHANGELOG.md` before tagging.

## Output Format

```markdown
## Release Preparation — v${releaseVersion}

### Pre-release Gates
| Gate | Result |
|------|--------|
| `mvn verify` | PASS/FAIL |
| Coverage | XX% (≥80%) |
| PMD | PASS/FAIL (0 violations) |

### Actions Taken
- [ ] pom.xml updated to ${releaseVersion}
- [ ] CHANGELOG.md updated
- [ ] Commit: `chore(release): bump version to ${releaseVersion}`
- [ ] Tag: `v${releaseVersion}` created and pushed
- [ ] Docker image: `prx/directory-backend:${releaseVersion}`
- [ ] GitHub release published

### Artifacts
- JAR: `target/directory-backend.jar`
- Docker: `prx/directory-backend:${releaseVersion}`
- Release URL: https://github.com/<org>/directory-backend/releases/tag/v${releaseVersion}
```

