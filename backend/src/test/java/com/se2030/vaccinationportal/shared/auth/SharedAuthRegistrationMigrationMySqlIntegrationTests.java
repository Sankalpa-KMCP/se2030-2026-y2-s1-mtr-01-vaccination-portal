package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.testcontainers.AbstractIsolatedMySqlMigrationIntegrationTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class SharedAuthRegistrationMigrationMySqlIntegrationTests extends AbstractIsolatedMySqlMigrationIntegrationTest {

    @DynamicPropertySource
    static void isolatedDatabase(DynamicPropertyRegistry registry) {
        configureIsolatedDatabase(registry, "historical_auth_v12");
    }

    @Test
    @Timeout(30)
    @DisplayName("V12 migrates existing authentication rows on real MySQL without credential seeds or phone uniqueness")
    void migrationV12_preservesRowsAndAddsRegistrationFoundation() throws Exception {
        assertConnectedToRealMySql();

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);

            statement.execute("SET FOREIGN_KEY_CHECKS = 0;");
            statement.execute("DROP TABLE IF EXISTS email_verification_tokens;");
            statement.execute("DROP TABLE IF EXISTS password_reset_tokens;");
            statement.execute("DROP TABLE IF EXISTS refresh_token_sessions;");
            statement.execute("DROP TABLE IF EXISTS user_roles;");
            statement.execute("DROP TABLE IF EXISTS citizen_profiles;");
            statement.execute("DROP TABLE IF EXISTS user_accounts;");
            statement.execute("DROP TABLE IF EXISTS roles;");
            statement.execute("SET FOREIGN_KEY_CHECKS = 1;");

            executeMigration(statement, "db/migration/V4__shared_authentication_foundation.sql");
            statement.executeUpdate("""
                    INSERT INTO user_accounts (email, password_hash, account_status)
                    VALUES ('legacy@example.com', '$2a$10$legacyHashForMigrationOnly', 'ACTIVE')
                    """);

            executeMigration(statement, "db/migration/V12__shared_auth_registration_foundation.sql");

            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            assertNullableColumn(metadata, catalog, "full_name");
            assertNullableColumn(metadata, catalog, "phone_number");
            assertNullableColumn(metadata, catalog, "requested_role_id");
            assertNullableColumn(metadata, catalog, "reviewed_by_user_id");
            assertNullableColumn(metadata, catalog, "reviewed_at");
            assertNullableColumn(metadata, catalog, "rejection_reason");

            assertForeignKey(metadata, catalog, "requested_role_id", "roles", "id");
            assertForeignKey(metadata, catalog, "reviewed_by_user_id", "user_accounts", "id");

            assertEquals(0, scalarInt(statement, """
                    SELECT COUNT(*)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'user_accounts'
                      AND column_name = 'phone_number'
                      AND non_unique = 0
                    """), "phone_number must not have a unique index");

            assertEquals(1, scalarInt(statement, "SELECT COUNT(*) FROM user_accounts"),
                    "V12 must preserve the one pre-existing account and seed no accounts");
            assertEquals(0, scalarInt(statement, "SELECT COUNT(*) FROM user_roles"),
                    "V12 must not assign any role or administrator authority");
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT full_name, phone_number, requested_role_id, reviewed_by_user_id,
                           reviewed_at, rejection_reason
                    FROM user_accounts WHERE email = 'legacy@example.com'
                    """)) {
                assertTrue(resultSet.next());
                for (int column = 1; column <= 6; column++) {
                    assertNull(resultSet.getObject(column), "new field " + column + " must be null for a legacy row");
                }
            }

            statement.executeUpdate("""
                    INSERT INTO user_accounts (email, phone_number, password_hash, account_status)
                    VALUES ('phone-one@example.com', '+94 77 123 4567', 'hash-one', 'ACTIVE'),
                           ('phone-two@example.com', '+94 77 123 4567', 'hash-two', 'ACTIVE')
                    """);
            assertEquals(2, scalarInt(statement,
                    "SELECT COUNT(*) FROM user_accounts WHERE phone_number = '+94 77 123 4567'"));

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "UPDATE user_accounts SET requested_role_id = 999999 WHERE email = 'legacy@example.com'"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "UPDATE user_accounts SET reviewed_by_user_id = 999999 WHERE email = 'legacy@example.com'"));
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

    private void assertNullableColumn(DatabaseMetaData metadata, String catalog, String columnName) throws SQLException {
        try (ResultSet resultSet = metadata.getColumns(catalog, null, "user_accounts", columnName)) {
            assertTrue(resultSet.next(), "Column user_accounts." + columnName + " must exist");
            assertEquals(DatabaseMetaData.columnNullable, resultSet.getInt("NULLABLE"),
                    "Column user_accounts." + columnName + " must permit null");
        }
    }

    private void assertForeignKey(
            DatabaseMetaData metadata,
            String catalog,
            String foreignKeyColumn,
            String referencedTable,
            String referencedColumn
    ) throws SQLException {
        boolean found = false;
        try (ResultSet resultSet = metadata.getImportedKeys(catalog, null, "user_accounts")) {
            while (resultSet.next()) {
                if (foreignKeyColumn.equalsIgnoreCase(resultSet.getString("FKCOLUMN_NAME"))
                        && referencedTable.equalsIgnoreCase(resultSet.getString("PKTABLE_NAME"))
                        && referencedColumn.equalsIgnoreCase(resultSet.getString("PKCOLUMN_NAME"))) {
                    found = true;
                    assertEquals(DatabaseMetaData.importedKeySetNull, resultSet.getShort("DELETE_RULE"),
                            foreignKeyColumn + " must use ON DELETE SET NULL");
                }
            }
        }
        assertTrue(found, "Foreign key for user_accounts." + foreignKeyColumn + " must exist");
    }

    private int scalarInt(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
