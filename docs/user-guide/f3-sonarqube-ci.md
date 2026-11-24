# SonarQube check (user guide)

The pull request check "SonarQube / Build and analyze" builds the project, runs the tests and sends the analysis to SonarCloud.

## What you may see

| Situation | Result |
|---|---|
| Secret `SONAR_TOKEN` valid | Build, tests and analysis run. |
| Secret missing (for example a PR from a fork) | Warning "SonarCloud analysis skipped"; build and tests still run and the check passes. |
| Token expired, revoked or without access | The check fails with "SONAR_TOKEN rejected by SonarCloud". |

## Renewing the token (repository admin)

1. In SonarCloud open My Account > Security and generate a new token for the organization `umdevc` (it needs permission to analyze project `umdc-directory-backend`).
2. In GitHub open Settings > Secrets and variables > Actions and update the secret `SONAR_TOKEN`.
3. Re-run the failed check.
