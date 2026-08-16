package com.se2030.vaccinationportal.testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared abstract base class for MySQL Testcontainers concurrency assurance tests.
 * Manages a singleton MySQL container lifecycle across test suites to eliminate container
 * start/stop overhead per test class, and verifies connection to production-equivalent MySQL.
 */
@SpringBootTest
@Testcontainers
@Tag("mysql-testcontainers")
public abstract class AbstractMySqlTestContainersIntegrationTest {

    private static final String MYSQL_IMAGE = "mysql:8.4.3";

    protected static final MySQLContainer<?> MYSQL_CONTAINER;

    static {
        MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("vaccination_portal_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(false);
        MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.open-in-view", () -> "false");
    }

    @Autowired
    protected DataSource dataSource;

    /**
     * Asserts that the active test execution is connected to real MySQL and not falling back to in-memory H2.
     */
    protected void assertConnectedToRealMySql() throws Exception {
        assertNotNull(dataSource, "DataSource must not be null");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            assertEquals("MySQL", productName, "Expected MySQL database product name");
            String productVersion = metaData.getDatabaseProductVersion();
            assertNotNull(productVersion, "MySQL product version must be present");
            assertTrue(
                    productVersion.startsWith("8.") || productVersion.startsWith("9."),
                    "Expected MySQL 8.x/9.x but connected to version: " + productVersion
            );
        }
    }
}
