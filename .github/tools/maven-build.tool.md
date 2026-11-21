---
name: Maven Build
description: Tool for executing Maven build lifecycle commands
type: terminal
command-prefix: mvn
---

# Maven Build Tool

## Purpose

Execute Maven build lifecycle commands for the **directory-backend** project.

## Available Commands

### Build

```bash
# Fast build (skip tests)
mvn -DskipTests package

# Full CI-aligned build
mvn -B -V -e clean verify

# Clean only
mvn clean
```

### Test

```bash
# Run all tests
mvn test

# Run single test class
mvn -Dtest=com.umdc.controller.v1.api.directory.CampaignControllerTest test

# Run single test method
mvn -Dtest=CampaignControllerTest#listCampaignsReturnsOk test

# Skip integration tests
mvn -DskipITs=true test
```

### Coverage

```bash
# Generate JaCoCo report
mvn clean test jacoco:report

# Coverage with profile
mvn -P coverage clean verify

# View report
open target/site/jacoco/index.html
```

### Static Analysis

```bash
# PMD check
mvn pmd:check pmd:cpd-check

# View PMD report
open target/pmd.xml
```

### Benchmarks

```bash
# Run JMH benchmarks
mvn test-compile exec:java -P benchmark -DskipTests
```

### Dependency Management

```bash
# Check for dependency updates
mvn versions:display-dependency-updates

# Check for plugin updates
mvn versions:display-plugin-updates

# Dependency tree
mvn dependency:tree
```

### OpenRewrite

```bash
# Run Spring Boot upgrade recipe (dry-run)
mvn rewrite:dryRun

# Apply recipe
mvn rewrite:run
```

## Output Locations

- Build artifact: `target/directory-backend.jar`
- JaCoCo reports: `target/site/jacoco/`
- PMD reports: `target/pmd.xml`, `target/cpd.xml`
- Surefire reports: `target/surefire-reports/`
- Maven archiver: `target/maven-archiver/`
