package com.se2030.vaccinationportal.testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Historical schema mutations belong to a class-specific database, never the application database. */
@SpringBootTest
@Testcontainers
@Tag("mysql-testcontainers")
public abstract class AbstractIsolatedMySqlMigrationIntegrationTest {
    @Autowired
    protected DataSource dataSource;

    protected static void configureIsolatedDatabase(DynamicPropertyRegistry registry, String database) {
        if (!database.matches("historical_[a-z0-9_]+")) {
            throw new IllegalArgumentException("Expected a historical migration database name");
        }
        MySQLContainer<?> mysql = AbstractMySqlTestContainersIntegrationTest.MYSQL_CONTAINER;
        try (Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
             var statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + database + "`");
            statement.execute("GRANT ALL PRIVILEGES ON `" + database + "`.* TO 'test_user'@'%'");
        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException("Cannot provision isolated migration database: " + database, ex);
        }
        String url = mysql.getJdbcUrl().replace("/" + mysql.getDatabaseName(), "/" + database);
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");
    }

    protected void assertConnectedToRealMySql() throws Exception {
        assertNotNull(dataSource, "DataSource must not be null");
        try (Connection connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            assertEquals("MySQL", metadata.getDatabaseProductName(), "Expected MySQL database product name");
            String version = metadata.getDatabaseProductVersion();
            assertNotNull(version, "MySQL product version must be present");
            assertTrue(version.startsWith("8.") || version.startsWith("9."),
                    "Expected MySQL 8.x/9.x but connected to version: " + version);
            assertTrue(connection.getCatalog().startsWith("historical_"),
                    "Historical migration must use its isolated database");
        }
    }
}
