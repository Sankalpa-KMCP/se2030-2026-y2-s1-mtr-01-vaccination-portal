package com.se2030.vaccinationportal.clinical;

import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;

/** Dedicated real MySQL schema; full-profile migration bootstrap tests remain separate. */
@Tag("mysql-testcontainers")
public class ClinicalLifecycleMySqlConcurrencyIntegrationTests extends ClinicalLifecycleConcurrencyIntegrationTests {
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("clinical_lifecycle").withUsername("clinical_test").withPassword("clinical_test_password");
    static { MYSQL.start(); }

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
}
