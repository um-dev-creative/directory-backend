package com.umdc.directory.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/// Guard: directory-backend must never access a DB schema other than directory_site.
/// Data of schema general belongs to Backbone and is obtained only through BackboneClient.
@DisplayName("No access to the general schema")
class NoGeneralSchemaAccessTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path ENTITIES = MAIN_JAVA.resolve("com/umdc/directory/jpa/entity");
    private static final Path REPOSITORIES = MAIN_JAVA.resolve("com/umdc/directory/jpa/repository");
    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");

    private static final Pattern SCHEMA = Pattern.compile("schema\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern TABLE_ANNOTATION = Pattern.compile("@Table\\s*\\(([^)]*)\\)");
    private static final Pattern GENERAL_SQL = Pattern.compile("general(\\.|\")", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERAL_WORD = Pattern.compile("\\bgeneral\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERAL_ENTITY_IMPORT = Pattern.compile(
            "import\\s+com\\.umdc\\.directory\\.jpa\\.entity\\.(UserEntity|ContactTypeEntity)\\s*;");

    private static List<Path> files(Path root, String suffix) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            return s.filter(p -> p.toString().endsWith(suffix)).sorted().toList();
        }
    }

    @Test
    @DisplayName("every @Entity/@Table is in schema directory_site")
    void entitiesAreInDirectorySite() throws IOException {
        List<String> offenders = new ArrayList<>();
        int tables = 0;
        for (Path file : files(ENTITIES, ".java")) {
            String src = Files.readString(file);
            if (!src.contains("@Entity")) {
                continue;
            }
            var table = TABLE_ANNOTATION.matcher(src);
            if (!table.find()) {
                offenders.add(file + " has @Entity without @Table(schema = \"directory_site\")");
                continue;
            }
            tables++;
            var schema = SCHEMA.matcher(table.group(1));
            if (!schema.find() || !"directory_site".equals(schema.group(1))) {
                offenders.add(file + " maps schema " + (schema.reset().find() ? schema.group(1) : "<default>"));
            }
        }
        assertTrue(tables > 0, "no entity was scanned");
        assertTrue(offenders.isEmpty(), "Entities outside directory_site: " + offenders);
    }

    @Test
    @DisplayName("no migration references the general schema")
    void migrationsDoNotReferenceGeneral() throws IOException {
        List<String> offenders = new ArrayList<>();
        var scripts = files(MIGRATIONS, ".sql");
        for (Path file : scripts) {
            if (GENERAL_SQL.matcher(Files.readString(file)).find()) {
                offenders.add(file.toString());
            }
        }
        assertTrue(!scripts.isEmpty(), "no migration was scanned");
        assertTrue(offenders.isEmpty(), "Migrations referencing the general schema: " + offenders);
    }

    @Test
    @DisplayName("no repository query mentions general")
    void repositoriesDoNotQueryGeneral() throws IOException {
        List<String> offenders = new ArrayList<>();
        var repositories = files(REPOSITORIES, ".java");
        for (Path file : repositories) {
            String src = Files.readString(file);
            var query = Pattern.compile("@(Native)?Query\\s*\\(").matcher(src);
            while (query.find()) {
                // The attribute list of the annotation, plus the constants it may reference, is checked below.
                int end = src.indexOf(")\n", query.end());
                String args = src.substring(query.end(), end < 0 ? src.length() : end);
                if (GENERAL_WORD.matcher(args).find()) {
                    offenders.add(file + " -> " + args.strip());
                }
            }
            // Query constants (Q_*/String literals starting a JPQL/SQL statement) are checked as string literals too.
            var literal = Pattern.compile("\"((?:SELECT|UPDATE|DELETE|INSERT|WITH)\\b[^\"]*)\"", Pattern.CASE_INSENSITIVE).matcher(src);
            while (literal.find()) {
                if (GENERAL_WORD.matcher(literal.group(1)).find()) {
                    offenders.add(file + " -> " + literal.group(1));
                }
            }
        }
        assertTrue(!repositories.isEmpty(), "no repository was scanned");
        assertTrue(offenders.isEmpty(), "Queries mentioning general: " + offenders);
    }

    @Test
    @DisplayName("no class of com.umdc.directory imports an entity of the general schema")
    void noClassImportsGeneralEntities() throws IOException {
        List<String> offenders = new ArrayList<>();
        var sources = new ArrayList<Path>(files(MAIN_JAVA, ".java"));
        sources.addAll(files(Path.of("src/test/java"), ".java"));
        for (Path file : sources) {
            if (file.getFileName().toString().equals("NoGeneralSchemaAccessTest.java")) {
                continue;
            }
            String src = Files.readString(file);
            if (GENERAL_ENTITY_IMPORT.matcher(src).find()
                    || Pattern.compile("schema\\s*=\\s*\"general\"").matcher(src).find()) {
                offenders.add(file.toString());
            }
        }
        assertTrue(!Files.exists(ENTITIES.resolve("UserEntity.java")) && !Files.exists(ENTITIES.resolve("ContactTypeEntity.java")),
                "UserEntity/ContactTypeEntity must not exist");
        assertTrue(offenders.isEmpty(), "Classes referencing general-schema entities: " + offenders);
    }

    @Test
    @DisplayName("V12 drops cross-schema foreign keys by catalog lookup, not by hard-coded names")
    void v12DropsByCatalogLookup() throws IOException {
        String code = Files.readString(MIGRATIONS.resolve("V12__drop_cross_schema_foreign_keys.sql")).lines()
                .filter(l -> !l.stripLeading().startsWith("--")).reduce("", (a, b) -> a + b + "\n");
        assertTrue(code.contains("pg_constraint") && code.contains("confrelid") && code.contains("contype = 'f'"),
                "V12 must look the foreign keys up in pg_constraint");
        assertTrue(code.contains("relnamespace <> 'directory_site'::regnamespace"),
                "V12 must select the keys referencing another schema");
        assertTrue(code.contains("DROP CONSTRAINT IF EXISTS %I"), "V12 must drop the constraint found, idempotently");
        for (String name : List.of("fk_business_user_1", "fk_contact_business_contact_type_1", "user_favorite_user_fk_fkey")) {
            assertTrue(!code.contains(name), "V12 must not hard-code " + name);
        }
    }
}
