package com.se2030.vaccinationportal.shared.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
class DatabaseMigrationV4IntegrationTests {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("V4 migration applies cleanly, creates tables, inserts canonical roles, and contains 0 user seeds")
    void v4Migration_executesCleanlyAndValidatesSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__shared_authentication_foundation.sql"));

            try (Statement statement = connection.createStatement()) {
                // 1. Verify tables exist and can be queried
                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM user_accounts")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Expected 0 seeded user accounts in production V4 migration");
                }

                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM user_roles")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Expected 0 user_roles rows in production V4 migration");
                }

                try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM refresh_token_sessions")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Expected 0 refresh_token_sessions rows in production V4 migration");
                }

                // 2. Verify static canonical roles are inserted
                Set<String> roleCodes = new HashSet<>();
                try (ResultSet rs = statement.executeQuery("SELECT code FROM roles")) {
                    while (rs.next()) {
                        roleCodes.add(rs.getString("code"));
                    }
                }

                assertTrue(roleCodes.contains("ROLE_ADMIN"));
                assertTrue(roleCodes.contains("ROLE_CENTER_MANAGER"));
                assertTrue(roleCodes.contains("ROLE_HEALTHCARE_OFFICER"));
                assertTrue(roleCodes.contains("ROLE_CAMPAIGN_OFFICER"));
                assertTrue(roleCodes.contains("ROLE_INVENTORY_OFFICER"));
                assertTrue(roleCodes.contains("ROLE_CITIZEN"));
                assertTrue(roleCodes.contains("ROLE_MANAGEMENT_USER"));
                assertEquals(7, roleCodes.size());
            }
        }
    }
}
