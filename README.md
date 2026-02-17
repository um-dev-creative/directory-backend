# Directory Backend
## Qodana and SonarCloud badges

[![Quality gate](https://sonarcloud.io/api/project_badges/quality_gate?project=umdc-directory-backend&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)

[![Qodana](https://github.com/um-dev-creative/directory-backend/actions/workflows/qodana_code_quality.yml/badge.svg)](https://github.com/um-dev-creative/directory-backend/actions/workflows/qodana_code_quality.yml)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=reliability_rating&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=security_rating&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=sqale_rating&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=code_smells&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=coverage&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=duplicated_lines_density&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=ncloc&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=vulnerabilities&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=umdc-directory-backend&metric=bugs&token=30babbee984be4a21f7e4627f90e80c5b47330fa)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)

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

## Requirements

Before following the developer getting-started steps, ensure your environment meets the minimum requirements and that any optional integrations you need are available.

- Java 21 (JDK 21) installed and JAVA_HOME configured.
- Maven 3.8+ installed and `mvn` available on your PATH.
- Optional but recommended: Docker (for running Postgres/Kafka/Vault locally).
- A PostgreSQL instance accessible to the application when running integration flows (use Docker for local development if you don't have a database).
- If you plan to exercise messaging or external integrations, have Kafka and Vault available or running locally/mocked.
- Copy and customize `default.env` for local environment variables (e.g., `cp default.env .env`).
- Recommended development machine: 8+ GB RAM, multi-core CPU, and several GB of free disk space for local DB/containers.

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
| Technology |                      Version | Source |
|---|-----------------------------:|---|
| Docker (base image) | amazoncorretto:21-alpine3.23 | `Dockerfile` (FROM) |
| Java |                           21 | `pom.xml`: `<properties>` (`java.version`) |
| JaCoCo |                       0.8.14 | `pom.xml`: `<properties>` (`maven.plugin.jacoco.version`) |
| Kafka |                     detected | `pom.xml`: `org.springframework.kafka:spring-kafka` dependency |
| Maven |                        3.8.0 | `pom.xml` present |
| PostgreSQL |                       42.7.7 | `pom.xml`: `<properties>` (`postgresql.version`) and `dependencyManagement` |
| Spring Boot |                        3.5.8 | `pom.xml`: `<parent>` |
| Spring Cloud |                     2025.0.1 | `pom.xml`: `<properties>` (`spring-cloud.version`) |
| Vault |                     detected | `default.env` (VAULT_ENABLED) |

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

[![SonarQube Cloud](https://sonarcloud.io/images/project_badges/sonarcloud-light.svg)](https://sonarcloud.io/summary/new_code?id=umdc-directory-backend)
