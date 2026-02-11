README - Build Instructions
===========================

Quick start (PowerShell)
------------------------
1. Ensure you have Java and Maven installed. Check with:

```powershell
mvn -v
java -version
```

2. Build the project (skip tests for a faster build):

```powershell
mvn -DskipTests package
```

3. Build the project (run tests):

```powershell
mvn package
```

4. Generate dependency tree (helpful for debugging version conflicts):

```powershell
mvn -DskipTests dependency:tree > dependency-tree.txt
```

Notes on centralized versions
----------------------------
- This project centralizes many dependency and plugin versions in the top-level `pom.xml` under the `<properties>` section. Common properties include:
  - `${spring-boot.version}`
  - `${spring-cloud.version}`
  - `${mapstruct.version}`
  - `${mockito.version}`
  - `${junit.jupiter.version}`
  - `${postgresql.version}`
  - and others (see `pom.xml`)

- The `spring-boot-starter-parent` parent version is referenced via `${spring-boot.version}` to keep Spring Boot upgrades centralized.

Troubleshooting
---------------
- If the build fails due to dependency or plugin version issues, run:

```powershell
mvn -DskipTests -e package
```

and inspect the output. Look for:
- Conflicting versions for the same library
- Plugin execution failures
- Missing artifacts from private repositories

- If you rely on private artifacts (PRX repo), ensure you can access `https://repo.repsy.io/mvn/lmata/prx` and that credentials (if required) are configured in your `settings.xml`.

Rollback
--------
- Use git to revert changes: create a branch before editing and commit often. To undo:

```powershell
git checkout main
git reset --hard <commit-before-changes>
```

Contact/Next steps
------------------
- To centralize further versions or generate a BOM, I can update `pom.xml` to add more properties or move shared versions into `dependencyManagement`. Tell me if you'd like me to proceed.
