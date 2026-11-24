package com.umdc.directory.postgres;

import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test (profile {@code pg-it}, needs Docker): applies the migration scripts to a real PostgreSQL, twice, to prove they
 * run and are idempotent. Not part of the default build. Uses a minimal baseline of the tables that predate the scripts.
 */
class MigrationsPostgresIT {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration");
    private static final Pattern NAME = Pattern.compile("V(\\d+)__.*\\.sql");

    private static final String BASELINE = """
            CREATE SCHEMA IF NOT EXISTS directory_site;
            CREATE FUNCTION directory_site.uuid_generate_v4() RETURNS uuid AS 'SELECT gen_random_uuid()' LANGUAGE sql;
            CREATE TABLE directory_site.business (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_fk UUID);
            CREATE TABLE directory_site.product (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), active BOOLEAN NOT NULL DEFAULT TRUE);
            CREATE TABLE directory_site.campaign (id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                business_fk UUID, active BOOLEAN NOT NULL DEFAULT TRUE, start_date TIMESTAMP, end_date TIMESTAMP);
            CREATE TABLE directory_site.user_favorite (id UUID PRIMARY KEY DEFAULT gen_random_uuid(), user_fk UUID NOT NULL,
                business_fk UUID, product_fk UUID, campaign_fk UUID, active BOOLEAN NOT NULL DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL DEFAULT now(), updated_at TIMESTAMP, deleted_at TIMESTAMP, deleted_by UUID);
            """;

    @Test
    void migrationsApplyAndAreIdempotentOnPostgres() throws Exception {
        List<Path> scripts = scripts();
        assertTrue(scripts.stream().anyMatch(p -> p.getFileName().toString().startsWith("V6__")), "V6 must exist");
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");) {
            postgres.start();
            try (Connection c = java.sql.DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement st = c.createStatement()) {
                st.execute(BASELINE);
                for (int round = 1; round <= 2; round++) {
                    for (Path script : scripts) {
                        apply(st, script);
                    }
                }
                assertTrue(exists(st, "location_opening_hours"), "V6 table");
                assertTrue(exists(st, "saved_search"), "V7 table");
                assertTrue(exists(st, "favorite_list"), "V13 table");
                assertTrue(exists(st, "business_event_daily"), "V11 table");
            }
        }
    }

    private static void apply(Statement st, Path script) throws IOException, SQLException {
        try {
            st.execute(Files.readString(script, StandardCharsets.UTF_8));
        } catch (SQLException e) {
            throw new SQLException("Failed applying " + script.getFileName() + ": " + e.getMessage(), e);
        }
    }

    private static boolean exists(Statement st, String table) throws SQLException {
        try (ResultSet rs = st.executeQuery(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'directory_site' AND table_name = '" + table + "'")) {
            rs.next();
            return rs.getInt(1) == 1;
        }
    }

    private static List<Path> scripts() throws IOException {
        try (Stream<Path> files = Files.list(MIGRATIONS)) {
            return files.filter(p -> NAME.matcher(p.getFileName().toString()).matches())
                    .sorted((a, b) -> Integer.compare(number(a), number(b))).toList();
        }
    }

    private static int number(Path p) {
        Matcher m = NAME.matcher(p.getFileName().toString());
        assertTrue(m.matches());
        return Integer.parseInt(m.group(1));
    }
}
