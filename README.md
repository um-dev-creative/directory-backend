# Directory Backend
## Qodana and SonarCloud badges

[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=umdc-directory-backend&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)

[![Qodana](https://github.com/um-dev-creative/directory-backend/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/um-dev-creative/directory-backend/actions/workflows/qodana_code_quality.yml)

## Tech stack badges
<!-- badges: start -->
![Java](https://img.shields.io/badge/Java-21-blue?logo=java&style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-brightgreen?logo=spring&style=flat-square)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-brightgreen?logo=spring&style=flat-square)
[![Maven](https://img.shields.io/badge/Maven-%3E%3D%203.8.0-orange?logo=apachemaven&style=flat-square)](https://maven.apache.org/)
[![JUnit](https://img.shields.io/badge/JUnit%20Jupiter-5.14.1-red?logo=junit5&style=flat-square)](https://junit.org/junit5/)
[![JaCoCo](https://img.shields.io/badge/JaCoCo-0.8.14-yellow?logo=jacoco&style=flat-square)](https://www.jacoco.org/jacoco/)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-42.7.7-blue?logo=postgresql&style=flat-square)
![Docker (base image)](https://img.shields.io/badge/Docker-amazoncorretto%3A21--alpine3.23-blue?logo=docker&style=flat-square)
![Vault](https://img.shields.io/badge/Vault-detected-black?logo=hashicorp&style=flat-square)
![Kafka](https://img.shields.io/badge/Kafka-detected-orange?logo=apachekafka&style=flat-square)
<!-- badges: end -->

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
- Java 21 (configured as `java.version` in `pom.xml`)
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

Tech stack and versions
-----------------------
| Technology |           Version | Source |
|---|------------------:|---|
| Docker (base image) | amazoncorretto:25-alpine3.22 | `Dockerfile` (FROM) |
| Java |                21 | `pom.xml`: `<properties>` (`java.version`) |
| JaCoCo |           0.8.14 | `pom.xml`: `<properties>` (`maven.plugin.jacoco.version`) |
| Kafka |         detected | `pom.xml`: `org.springframework.kafka:spring-kafka` dependency |
| Maven |             detected | `pom.xml` present |
| PostgreSQL |            42.7.7 | `pom.xml`: `<properties>` (`postgresql.version`) and `dependencyManagement` |
| Spring Boot |             3.5.8 | `pom.xml`: `<parent>` |
| Spring Cloud |          2025.0.1 | `pom.xml`: `<properties>` (`spring-cloud.version`) |
| Vault |            detected | `default.env` (VAULT_ENABLED) |

Dependency & version management (short)
---------------------------------------
- Many dependency and plugin versions are centralized inside the top-level `pom.xml` under the `<properties>` block and controlled via `<dependencyManagement>` where appropriate.
- If you want to change a dependency version, prefer updating the property in `pom.xml` (if available) or the related `dependencyManagement` entry.

Build & validation notes
------------------------
See `README-BUILD.md` for a quick build checklist and common troubleshooting commands.

Testing & coverage
------------------
- Unit tests use JUnit 5 (Jupiter). Tests live under `src/test/java`.
- The project uses JaCoCo for coverage. To generate a coverage report:
```powershell
mvn clean test jacoco:report
# open the report at target/site/jacoco/index.html
```

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

End of README.
