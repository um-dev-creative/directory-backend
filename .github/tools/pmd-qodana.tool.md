---
name: PMD and Qodana
description: Static analysis tools for Java code quality in directory-backend
type: terminal
command-prefix: mvn pmd
used-by: [Code Reviewer, Developer]
---

# PMD and Qodana Tool

## Purpose

Runs static analysis on `directory-backend` Java source code. PMD enforces rules defined in `ruleset.xml`; Qodana performs broader inspection with results in `qodana.sarif.json`.

## Available Commands

### PMD

```bash
# Check for violations (fails build on violation)
mvn pmd:check

# Generate PMD report without failing build
mvn pmd:pmd

# Check CPD (copy-paste detection)
mvn pmd:cpd-check
```

### PMD Report Output
- XML report: `target/pmd.xml`
- CPD report: `target/cpd.xml`
- HTML report: `target/site/pmd.html` (after `mvn site`)

### Reading PMD Results
```bash
# Count violations
grep -c '<violation' target/pmd.xml

# Show all violations with rule names
grep -E 'violation|rule=' target/pmd.xml | head -40
```

### Qodana

```bash
# Run Qodana scan locally (requires Docker)
qodana scan --project-dir . --results-dir target/qodana

# Or via GitHub Actions workflow
# → .github/workflows/qodana_code_quality.yml
```

### Qodana Report Output
- SARIF: `qodana.sarif.json`
- HTML report: `target/qodana/` (when run locally)

## Notes

- PMD ruleset is defined in `ruleset.xml` at project root — do not modify without reviewing all existing violations.
- Qodana config is in `qodana.yaml` at project root.
- CI PMD check is part of `mvn -B -V -e clean verify` — a violation blocks the build.
- Run `mvn pmd:pmd` (not `pmd:check`) for report-only mode when exploring violations.

