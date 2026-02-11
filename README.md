# Directory Backend

[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=prx-dev_directory-backend&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=prx-dev_directory-backend)


<!-- Tech badges -->

![Java](https://img.shields.io/badge/Java-21-blue?logo=java&style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen?logo=spring&style=flat-square)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-brightgreen?logo=spring&style=flat-square)
![JPA / Hibernate](https://img.shields.io/badge/JPA-Hibernate-orange?logo=hibernate&style=flat-square)


Overview
--------
Directory Backend is the backend microservice that powers the Directory platform. It provides REST APIs for managing businesses, campaigns, products and user accounts and integrates with internal PRX services (Backbone, Mercury) and third-party systems.

Key capabilities
- CRUD APIs for campaigns, businesses, products and users (exposed under `/api/v1/*`).
- Search and filter capabilities (specifications based on JPA criteria).
- Kafka messages for e.g. outgoing email notifications (TO classes under `com.prx.directory.kafka.to`).
- Centralized dependency and plugin version management in `pom.xml` for predictable upgrades.

Quick links
- Knowledge Base / Docs: https://prx.myjetbrains.com/youtrack/articles/DS-A-8/Directory-Backend
- Build instructions: `README-BUILD.md`
- Latest build validation notes: `BUILD_VALIDATION_REPORT.md`

Getting started (developer)
---------------------------
Prerequisites
- Java 21 (or the project's configured `java.version` in `pom.xml`)
- Maven (3.8+ recommended)
- Optional: Docker if you prefer containerized runs

Clone and build
```powershell
git clone <repo-url>
cd directory-backend
mvn -DskipTests package
```

Run tests
```powershell
# Run full test suite
mvn test

# Run a single test class
mvn -Dtest=com.prx.directory.kafka.to.EmailMessageTOTest test
```

Run locally
- The project builds to a runnable Spring Boot JAR in `target/`:
```powershell
mvn -DskipTests package
java -jar target/directory-backend.jar
```

Configuration
- Environment defaults are available in `default.env` (copy/override as needed).
- The service uses Spring Cloud / Spring Boot configuration conventions. For local development you can run with `--spring.profiles.active=local` and point to a local config server or an overridden `application.yml`.

Dependency & version management (short)
---------------------------------------
- Many dependency and plugin versions are centralized inside the top-level `pom.xml` under the `<properties>` block and controlled via `<dependencyManagement>` where appropriate.
- The project leaves the Spring Boot parent version as an explicit literal in the `<parent>` section (this avoids tool/linter issues when using a property there). However, most dependencies and plugin versions are centralized as properties (for example `${mapstruct.version}`, `${mockito.version}`, `${postgresql.version}`, etc.).

If you want to change a dependency version:
1. Prefer updating the property in `pom.xml` (if available) or the related `dependencyManagement` entry.
2. Re-run `mvn -DskipTests dependency:tree` to review transitive versions.

Build & validation notes
------------------------
See `README-BUILD.md` for a quick build checklist and common troubleshooting commands.
For the validation steps I ran while preparing these docs, see `BUILD_VALIDATION_REPORT.md`.

Testing & coverage
------------------
- Unit tests use JUnit 5 (Jupiter). Tests live under `src/test/java`.
- The project uses JaCoCo for coverage. To generate a coverage report:
```powershell
mvn clean test jacoco:report
# open the report at target/site/jacoco/index.html
```

API & Developer guidance
------------------------
- API contracts are typically defined in the `com.prx.directory.api.v1.*` packages and wired using Spring MVC / Jersey controllers.
- DTOs/TOs (transfer objects) are immutable Java records where appropriate — exercise caution when adding JSON annotations.

Contributing
------------
- Create a feature branch, run tests locally, and open a PR with a clear description of behavior and tests added.
- Keep backward compatibility in mind for public APIs and DTOs.

Security & dependencies
-----------------------
- The project contains some warnings from automated scans about transitive dependencies (hinting at older transitive libraries). If you see a security warning during your build, run:
```powershell
mvn -DskipTests dependency:tree > dependency-tree.txt
```
and inspect which direct dependency is bringing the vulnerable transitive. Then either:
- upgrade the direct dependency's version in `pom.xml` or
- add an `<exclusion>` and explicitly import a fixed version via `dependencyManagement`.

Contact
-------
- If you need help with build issues, dependency upgrades, or test coverage, open an issue and assign it to the team owners.

License
-------
This repository includes a `LICENSE` file at the project root — follow it for reuse/redistribution rules.

Acknowledgements
----------------
- Project tooling: Maven, JaCoCo, PMD
- Integrations: backbone, mercury (internal clients visible under `com.prx.directory.client.*`)
