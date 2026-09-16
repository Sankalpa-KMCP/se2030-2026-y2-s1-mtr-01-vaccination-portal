package com.se2030.vaccinationportal.shared.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("mysql-testcontainers")
class SuperAdminRoleMigrationMySqlIntegrationTests {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("super_admin_migration")
            .withUsername("super_admin_user")
            .withPassword("super_admin_password")
            .withReuse(false);

    @Test
    @DisplayName("V15 seeds canonical ROLE_SUPER_ADMIN, preserves existing roles, is idempotent, and contains 0 user seeds")
    void v15_realMySql_seedsSuperAdminRoleCleanly() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {

            // Apply V4 foundation first
            executeMigration(statement, "db/migration/V4__shared_authentication_foundation.sql");
            assertEquals(7, countRoles(statement), "V4 must seed the 7 initial canonical roles");
            assertEquals(0, scalarInt(statement, "SELECT COUNT(*) FROM user_accounts"),
                    "V4 must seed 0 user accounts");

            // Apply V15 migration
            executeMigration(statement, "db/migration/V15__add_super_admin_role.sql");

            // Verify ROLE_SUPER_ADMIN is now present and total count is 8
            Set<String> rolesAfterV15 = getRoleCodes(statement);
            assertEquals(8, rolesAfterV15.size(), "V15 must result in exactly 8 canonical roles");
            assertTrue(rolesAfterV15.contains("ROLE_SUPER_ADMIN"), "ROLE_SUPER_ADMIN must be seeded by V15");
            assertTrue(rolesAfterV15.contains("ROLE_ADMIN"));
            assertTrue(rolesAfterV15.contains("ROLE_CITIZEN"));
            assertEquals(0, scalarInt(statement, "SELECT COUNT(*) FROM user_accounts"),
                    "V15 must seed 0 user accounts");

            // Re-apply V15 to prove idempotence
            executeMigration(statement, "db/migration/V15__add_super_admin_role.sql");
            assertEquals(8, countRoles(statement), "Re-executing V15 must not duplicate roles");
        }
    }

    private void executeMigration(Statement statement, String classpathLocation) throws IOException, SQLException {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        assertTrue(resource.exists(), classpathLocation + " must exist");
        String sql = resource.getContentAsString(StandardCharsets.UTF_8);
        StringBuilder withoutComments = new StringBuilder();
        for (String line : sql.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                withoutComments.append(line).append('\n');
            }
        }
        for (String sqlStatement : withoutComments.toString().split(";")) {
            if (!sqlStatement.isBlank()) {
                statement.execute(sqlStatement.trim());
            }
        }
    }

    private int countRoles(Statement statement) throws SQLException {
        return scalarInt(statement, "SELECT COUNT(*) FROM roles");
    }

    private Set<String> getRoleCodes(Statement statement) throws SQLException {
        Set<String> codes = new HashSet<>();
        try (ResultSet rs = statement.executeQuery("SELECT code FROM roles")) {
            while (rs.next()) {
                codes.add(rs.getString("code"));
            }
        }
        return codes;
    }

    private int scalarInt(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
