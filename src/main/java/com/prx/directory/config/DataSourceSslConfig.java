package com.prx.directory.config;

import com.prx.commons.exception.StandardException;
import com.prx.directory.constant.DirectoryAppConstants;
import com.prx.directory.constant.DirectoryMessage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.prx.directory.constant.DirectoryAppConstants.ENTITY_PACKAGE;
import static com.prx.directory.constant.DirectoryAppConstants.REPOSITORY_PACKAGE;

/**
 * DataSourceSslConfig
 * Programmatic configuration of a HikariCP DataSource with optional
 * PostgreSQL SSL/TLS driver properties and Hikari connection pool tuning.
 *
 * <p>This configuration will create and expose a {@link javax.sql.DataSource}
 * bean when no other DataSource bean is present in the Spring context. It reads
 * standard Spring Boot datasource properties (URL, username, password) and
 * several custom properties under the {@code app.db.ssl.*} prefix to enable
 * and configure SSL/TLS verification for PostgreSQL connections.</p>
 *
 * <p>Key behavior:
 * <ul>
 *   <li>If {@code app.db.ssl.enabled=true} the Postgres JDBC driver properties
 *       {@code ssl=true} and {@code sslmode=<configured-mode>} will be set on
 *       the DataSource.</li>
 *   <li>If {@code app.db.ssl.root-cert} is provided and uses the
 *       {@code classpath:} prefix, the resource will be extracted to a
 *       temporary file and that file path will be provided to the driver as
 *       {@code sslrootcert} (the driver expects a filesystem path).</li>
 *   <li>Hikari pool limits are applied from {@code spring.datasource.hikari.*}
 *       properties including maximum pool size, minimum idle, connection
 *       timeout and initialization-fail-timeout.</li>
 * </ul>
 * </p>
 *
 * <p>Usage example (application-prod.yml):
 * <pre>
 * app:
 *   db:
 *     ssl:
 *       enabled: true
 *       mode: verify-full
 *       root-cert: classpath:prod-ca-2021.crt
 * spring:
 *   datasource:
 *     url: jdbc:postgresql://db-host:5432/directory
 *     username: ${DB_USER}
 *     password: ${DB_PASS}
 *     hikari:
 *       maximum-pool-size: 20
 *       minimum-idle: 5
 *       connection-timeout: 30000
 * </pre>
 * </p>
 *
 * <p>Security note: prefer {@code verify-full} in production to ensure hostname
 * verification and certificate chain validation. Keep the CA certificate file
 * protected (file system permissions) and avoid exposing it in logs.</p>
 *
 * @see com.zaxxer.hikari.HikariDataSource
 */
@Configuration
@EntityScan(basePackages = {ENTITY_PACKAGE})
@EnableJpaRepositories(basePackages = {REPOSITORY_PACKAGE})
public class DataSourceSslConfig {

    /**
     * JDBC URL used to connect to PostgreSQL. Example:
     * {@code jdbc:postgresql://db-host:5432/directory}
     */
    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    /**
     * Database username. Provided via configuration/environment.
     */
    @Value("${spring.datasource.username:}")
    private String username;

    /**
     * Database password. Provided via configuration/environment.
     */
    @Value("${spring.datasource.password:}")
    private String password;

    /**
     * Toggle to enable driver-level SSL/TLS settings (default: false).
     * When {@code true} additional driver properties will be applied.
     */
    @Value("${app.db.ssl.enabled:false}")
    private boolean sslEnabled;

    /**
     * SSL mode passed to the PostgreSQL JDBC driver (default: verify-full).
     * Common values: {@code require}, {@code verify-ca}, {@code verify-full}.
     */
    @Value("${app.db.ssl.mode:verify-full}")
    private String sslMode;

    /**
     * Path or classpath reference to the root CA certificate used to validate
     * the PostgreSQL server certificate. Example values:
     * <ul>
     *   <li>Absolute path: {@code /etc/ssl/certs/prod-ca-2021.crt}</li>
     *   <li>Classpath resource: {@code classpath:prod-ca-2021.crt}</li>
     * </ul>
     * If a classpath resource is given it will be extracted to a temporary
     * file and that filesystem path will be passed to the JDBC driver.
     */
    @Value("${app.db.ssl.root-cert:}")
    private String sslRootCert;

    /**
     * Maximum number of connections in the Hikari pool (default: 10).
     */
    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPool;

    /**
     * Minimum number of idle connections Hikari tries to maintain (default: 2).
     */
    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    /**
     * Maximum time to wait for a connection from the pool in milliseconds
     * (default: 30000).
     */
    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connTimeout;

    /**
     * Hikari initialization fail timeout (ms). Set to {@code -1} to disable
     * fail-fast initialization (useful for tests where the DB may be
     * intentionally unreachable during startup).
     */
    @Value("${spring.datasource.hikari.initialization-fail-timeout:1}")
    private long initFailTimeout;

    /**
     * Create and configure a {@link HikariDataSource} bean.
     *
     * <p>The returned DataSource is configured with the provided JDBC URL,
     * credentials and Hikari properties. If SSL is enabled the method will
     * set the Postgres driver properties {@code ssl} and {@code sslmode} and
     * will also set {@code sslrootcert} when a root certificate path is
     * provided. When {@code sslrootcert} is given as a classpath resource
     * (prefix {@code classpath:}) the resource will be copied to a temporary
     * file and that absolute filesystem path will be provided to the driver.</p>
     *
     * @return a fully configured {@link DataSource} backed by HikariCP
     * @throws StandardException when certificate extraction or temporary file creation fails
     */
    @Bean
    @Primary
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setMaximumPoolSize(maxPool);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connTimeout);
        config.setInitializationFailTimeout(initFailTimeout);

        // Add JDBC driver properties to enable SSL/TLS for PostgreSQL when requested
        if (sslEnabled) {
            // PostgreSQL JDBC driver recognizes the following properties: ssl, sslmode, sslrootcert
            config.addDataSourceProperty("ssl", "true");
            config.addDataSourceProperty("sslmode", sslMode);
            if (sslRootCert != null && !sslRootCert.isEmpty()) {
                String certPath = sslRootCert;
                if (sslRootCert.startsWith("classpath:")) {
                    String resourcePath = sslRootCert.substring("classpath:".length());
                    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
                        if (in == null) {
                            throw new IllegalArgumentException("Root certificate was not found SSL in the classpath: " + resourcePath);
                        }
                        Path tempFile = Files.createTempFile("db-root-cert", ".crt");
                        tempFile.toFile().deleteOnExit();
                        try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                            in.transferTo(out);
                        }
                        certPath = tempFile.toAbsolutePath().toString();
                    } catch (Exception e) {
                        throw new StandardException(DirectoryAppConstants.APPLICATION_NAME, DirectoryMessage.DATABASE_CERTIFICATE_ERROR, e);
                    }
                }
                config.addDataSourceProperty("sslrootcert", certPath);
            }
        }

        return new HikariDataSource(config);
    }
}
