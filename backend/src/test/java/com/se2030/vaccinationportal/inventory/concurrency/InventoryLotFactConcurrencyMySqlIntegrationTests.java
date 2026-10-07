package com.se2030.vaccinationportal.inventory.concurrency;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers concurrency tests verifying single-row
 * command locking and deterministic serialization for inventory batch lot facts (F-045-03).
 */
@Tag("mysql-testcontainers")
public class InventoryLotFactConcurrencyMySqlIntegrationTests extends InventoryLotFactConcurrencyIntegrationTests {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("inventory_lifecycle")
            .withUsername("inv_test")
            .withPassword("inv_test_password");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysql(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Override
    protected Connection observerConnection() throws Exception {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), "root", MYSQL.getPassword());
    }

    @Test
    void assertConnectedToRealMySql() throws Exception {
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
