package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Production-equivalent MySQL Testcontainers integration test proving real MySQL runtime execution
 * of Flyway migration script V4 on mysql:8.4.3, including table creation, foreign keys, unique indexes,
 * canonical role catalog seeds, and zero user credential seeds.
 */
class SharedAuthenticationMigrationMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Test
    @Timeout(30)
    @DisplayName("Migration SQL V4 MySQL Runtime Proof: V4 migration executes on real MySQL and verifies schema, constraints, and canonical roles")
    void migrationV4_executesOnRealMySql() throws Exception {
        assertConnectedToRealMySql();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);

            // Read V4 migration script from classpath
            ClassPathResource v4Resource = new ClassPathResource("db/migration/V4__shared_authentication_foundation.sql");
            assertTrue(v4Resource.exists(), "V4 migration script must exist in db/migration");

            String v4Sql;
            try (Reader reader = new InputStreamReader(v4Resource.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                v4Sql = sb.toString();
            }

            try (Statement stmt = conn.createStatement()) {
                // Drop V4-owned tables in dependency-safe order so test proves V4 creates schema independently of Hibernate auto-DDL
                stmt.execute("DROP TABLE IF EXISTS refresh_token_sessions;");
                stmt.execute("DROP TABLE IF EXISTS user_roles;");
                stmt.execute("DROP TABLE IF EXISTS user_accounts;");
                stmt.execute("DROP TABLE IF EXISTS roles;");

                // Execute statements in V4 migration script (strip SQL comments line-by-line first)
                String[] lines = v4Sql.split("\\R");
                StringBuilder cleanedSql = new StringBuilder();
                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (!trimmedLine.startsWith("--")) {
                        cleanedSql.append(line).append("\n");
                    }
                }

                for (String sqlStatement : cleanedSql.toString().split(";")) {
                    String trimmed = sqlStatement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }

                // 1. Verify tables exist in MySQL metadata
                DatabaseMetaData metaData = conn.getMetaData();
                String catalog = conn.getCatalog();

                try (ResultSet rs = metaData.getTables(catalog, null, "user_accounts", null)) {
                    assertTrue(rs.next(), "Table user_accounts must exist in MySQL");
                }
                try (ResultSet rs = metaData.getTables(catalog, null, "roles", null)) {
                    assertTrue(rs.next(), "Table roles must exist in MySQL");
                }
                try (ResultSet rs = metaData.getTables(catalog, null, "user_roles", null)) {
                    assertTrue(rs.next(), "Table user_roles must exist in MySQL");
                }
                try (ResultSet rs = metaData.getTables(catalog, null, "refresh_token_sessions", null)) {
                    assertTrue(rs.next(), "Table refresh_token_sessions must exist in MySQL");
                }

                // 2. Verify unique indexes exist in MySQL metadata
                List<String> userAccountIndexes = getIndexNames(metaData, catalog, "user_accounts");
                assertTrue(userAccountIndexes.stream().anyMatch(name -> name.contains("email")),
                        "Unique index on email must exist in user_accounts metadata");

                List<String> roleIndexes = getIndexNames(metaData, catalog, "roles");
                assertTrue(roleIndexes.stream().anyMatch(name -> name.contains("code")),
                        "Unique index on code must exist in roles metadata");

                List<String> refreshTokenIndexes = getIndexNames(metaData, catalog, "refresh_token_sessions");
                assertTrue(refreshTokenIndexes.stream().anyMatch(name -> name.contains("token_hash") || name.contains("uk_refresh_token_hash")),
                        "Unique index on token_hash must exist in refresh_token_sessions metadata");

                // 3. Verify exactly 7 static canonical role catalog entries
                Set<String> roleCodes = new HashSet<>();
                try (ResultSet rs = stmt.executeQuery("SELECT code, name FROM roles")) {
                    while (rs.next()) {
                        roleCodes.add(rs.getString("code"));
                    }
                }

                assertEquals(7, roleCodes.size(), "Expected exactly 7 canonical role catalog rows");
                assertTrue(roleCodes.contains("ROLE_ADMIN"));
                assertTrue(roleCodes.contains("ROLE_CENTER_MANAGER"));
                assertTrue(roleCodes.contains("ROLE_HEALTHCARE_OFFICER"));
                assertTrue(roleCodes.contains("ROLE_CAMPAIGN_OFFICER"));
                assertTrue(roleCodes.contains("ROLE_INVENTORY_OFFICER"));
                assertTrue(roleCodes.contains("ROLE_CITIZEN"));
                assertTrue(roleCodes.contains("ROLE_MANAGEMENT_USER"));

                // 4. Verify ZERO user credential seeds and ZERO refresh session seeds
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_accounts")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Expected 0 seeded user accounts in production V4 migration");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_roles")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Expected 0 user_roles rows in production V4 migration");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM refresh_token_sessions")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Expected 0 refresh_token_sessions rows in production V4 migration");
                }

                // 5. Verify Foreign Key integrity with dynamic insertion and cascade check
                stmt.execute("INSERT INTO user_accounts (id, email, password_hash, account_status, created_at, updated_at) " +
                        "VALUES (9901, 'test.mig@portal.gov', '$2a$10$dummyHashValueForTestingForeignKeyInMySQL12345', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);");

                stmt.execute("INSERT INTO user_roles (user_id, role_id) " +
                        "SELECT 9901, id FROM roles WHERE code = 'ROLE_CENTER_MANAGER';");

                stmt.execute("INSERT INTO refresh_token_sessions (user_id, token_family_id, token_hash, expires_at, created_at) " +
                        "VALUES (9901, 'fam-9901', 'hash-dummy-value-9901', DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 7 DAY), CURRENT_TIMESTAMP);");

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_roles WHERE user_id = 9901")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "User role relationship must be valid in MySQL");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = 9901")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "Refresh token session must be valid in MySQL");
                }

                // Delete test user to verify ON DELETE CASCADE
                stmt.execute("DELETE FROM user_accounts WHERE id = 9901;");

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user_roles WHERE user_id = 9901")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "user_roles row must cascade delete in MySQL");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM refresh_token_sessions WHERE user_id = 9901")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "refresh_token_sessions row must cascade delete in MySQL");
                }
            }
        }
    }

    private List<String> getIndexNames(DatabaseMetaData metaData, String catalog, String tableName) throws Exception {
        List<String> indexNames = new ArrayList<>();
        try (ResultSet rs = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                if (indexName != null) {
                    indexNames.add(indexName.toLowerCase());
                }
            }
        }
        return indexNames;
    }
}
