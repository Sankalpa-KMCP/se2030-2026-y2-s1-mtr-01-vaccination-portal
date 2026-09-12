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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("mysql-testcontainers")
class CitizenProfileMigrationMySqlIntegrationTests {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("citizen_profile_migration")
            .withUsername("citizen_profile_user")
            .withPassword("citizen_profile_password")
            .withReuse(false);

    @Test
    @DisplayName("V14 creates an optional one-to-one citizen profile without backfill or credential seeds")
    void v14_realMySql_createsSafeCitizenProfileSchema() throws Exception {
        try (Connection connection = MYSQL.createConnection("");
             Statement statement = connection.createStatement()) {
            executeMigration(statement, "db/migration/V4__shared_authentication_foundation.sql");
            assertEquals(0, scalarInt(statement, "SELECT COUNT(*) FROM user_accounts"),
                    "Authentication foundation must seed no user credentials");

            statement.executeUpdate("""
                    INSERT INTO user_accounts (email, password_hash, account_status)
                    VALUES ('legacy-citizen@example.com', 'migration-test-hash', 'ACTIVE')
                    """);
            executeMigration(statement, "db/migration/V12__shared_auth_registration_foundation.sql");
            int accountCountBeforeV14 = scalarInt(statement, "SELECT COUNT(*) FROM user_accounts");

            executeMigration(statement, "db/migration/V14__citizen_profile_foundation.sql");

            assertEquals(accountCountBeforeV14, scalarInt(statement, "SELECT COUNT(*) FROM user_accounts"),
                    "V14 must preserve all existing user accounts");
            assertEquals(0, scalarInt(statement, "SELECT COUNT(*) FROM citizen_profiles"),
                    "V14 must not bulk-create or infer citizen profiles");
            assertEquals(0, scalarInt(statement,
                    "SELECT COUNT(*) FROM user_accounts WHERE email <> 'legacy-citizen@example.com'"),
                    "V14 must seed no administrator or user credentials");

            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();
            assertColumn(metadata, catalog, "user_id", DatabaseMetaData.columnNoNulls);
            assertColumn(metadata, catalog, "date_of_birth", DatabaseMetaData.columnNullable);
            assertColumn(metadata, catalog, "gender", DatabaseMetaData.columnNullable);
            assertColumn(metadata, catalog, "address", DatabaseMetaData.columnNullable);
            assertColumn(metadata, catalog, "created_at", DatabaseMetaData.columnNoNulls);
            assertColumn(metadata, catalog, "updated_at", DatabaseMetaData.columnNoNulls);
            assertPrimaryKey(metadata, catalog, "user_id");
            assertUserForeignKey(metadata, catalog);

            try (ResultSet legacy = statement.executeQuery("""
                    SELECT full_name, phone_number
                    FROM user_accounts
                    WHERE email = 'legacy-citizen@example.com'
                    """)) {
                assertTrue(legacy.next());
                assertNull(legacy.getObject("full_name"));
                assertNull(legacy.getObject("phone_number"));
            }

            statement.executeUpdate("""
                    INSERT INTO citizen_profiles (user_id, date_of_birth, gender, address)
                    SELECT id, '1990-01-15', 'OTHER', 'Migration Test Address'
                    FROM user_accounts WHERE email = 'legacy-citizen@example.com'
                    """);
            assertEquals(1, scalarInt(statement, "SELECT COUNT(*) FROM citizen_profiles"));
            assertThrows(SQLException.class, () -> statement.executeUpdate("""
                    INSERT INTO citizen_profiles (user_id, address)
                    SELECT id, 'Duplicate Profile'
                    FROM user_accounts WHERE email = 'legacy-citizen@example.com'
                    """), "Primary key must enforce exactly one citizen profile per account");
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                    "INSERT INTO citizen_profiles (user_id, address) VALUES (999999, 'Unknown User')"),
                    "Citizen profiles must reference an existing user account");

            statement.executeUpdate(
                    "DELETE FROM user_accounts WHERE email = 'legacy-citizen@example.com'");
            assertEquals(0, scalarInt(statement, "SELECT COUNT(*) FROM citizen_profiles"),
                    "Deleting a user account must cascade to its citizen profile");
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

    private void assertColumn(
            DatabaseMetaData metadata,
            String catalog,
            String columnName,
            int expectedNullability
    ) throws SQLException {
        try (ResultSet columns = metadata.getColumns(catalog, null, "citizen_profiles", columnName)) {
            assertTrue(columns.next(), "Column citizen_profiles." + columnName + " must exist");
            assertEquals(expectedNullability, columns.getInt("NULLABLE"),
                    "Unexpected nullability for citizen_profiles." + columnName);
        }
    }

    private void assertPrimaryKey(DatabaseMetaData metadata, String catalog, String expectedColumn)
            throws SQLException {
        try (ResultSet primaryKeys = metadata.getPrimaryKeys(catalog, null, "citizen_profiles")) {
            assertTrue(primaryKeys.next(), "citizen_profiles must have a primary key");
            assertEquals(expectedColumn, primaryKeys.getString("COLUMN_NAME").toLowerCase());
        }
    }

    private void assertUserForeignKey(DatabaseMetaData metadata, String catalog) throws SQLException {
        boolean found = false;
        try (ResultSet foreignKeys = metadata.getImportedKeys(catalog, null, "citizen_profiles")) {
            while (foreignKeys.next()) {
                if ("user_id".equalsIgnoreCase(foreignKeys.getString("FKCOLUMN_NAME"))
                        && "user_accounts".equalsIgnoreCase(foreignKeys.getString("PKTABLE_NAME"))
                        && "id".equalsIgnoreCase(foreignKeys.getString("PKCOLUMN_NAME"))) {
                    found = true;
                    assertEquals(DatabaseMetaData.importedKeyCascade, foreignKeys.getShort("DELETE_RULE"));
                }
            }
        }
        assertTrue(found, "citizen_profiles.user_id must reference user_accounts.id");
    }

    private int scalarInt(Statement statement, String sql) throws SQLException {
        try (ResultSet resultSet = statement.executeQuery(sql)) {
            assertTrue(resultSet.next());
            return resultSet.getInt(1);
        }
    }
}
