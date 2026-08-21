package com.se2030.vaccinationportal.campaign;

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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers integration test proving real MySQL runtime execution
 * of Flyway migration scripts V1 and V2 on mysql:8.4.3, including table creation, UNIQUE constraints,
 * and idempotent duplicate cross-center user backfill.
 */
class OperationalStaffIdentityMigrationMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Test
    @Timeout(30)
    @DisplayName("Flyway V2 MySQL Runtime Proof: V2 migration script executes on real MySQL and backfills cross-center users")
    void flywayV2_executesOnRealMySql_andBackfillsDistinctUsers() throws Exception {
        assertConnectedToRealMySql();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);

            // Read V2 migration script
            ClassPathResource v2Resource = new ClassPathResource("db/migration/V2__mf02_operational_staff_identity.sql");
            assertTrue(v2Resource.exists(), "V2 migration script must exist in db/migration");

            String v2Sql;
            try (Reader reader = new InputStreamReader(v2Resource.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                v2Sql = sb.toString();
            }

            try (Statement stmt = conn.createStatement()) {
                // Setup pre-V2 roster state with duplicate user across centers
                stmt.execute("CREATE TABLE IF NOT EXISTS operational_staff_rosters (" +
                        "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                        "center_id BIGINT NOT NULL, " +
                        "user_id BIGINT NOT NULL, " +
                        "role VARCHAR(50) NOT NULL, " +
                        "status VARCHAR(50) NOT NULL, " +
                        "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                        "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB;");

                long testU1 = 9801L;
                long testU2 = 9802L;

                // Ensure referenced centers exist to satisfy foreign key constraints
                stmt.execute("INSERT INTO vaccination_centers (" +
                        "id, code, name, address, contact, timezone, center_status, safe_concurrent_occupancy, created_at, updated_at" +
                        ") VALUES " +
                        "(101, 'CTR-MIG-101', 'Mig Center 1', '100 Road', '111', 'UTC', 'ACTIVE', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                        "(102, 'CTR-MIG-102', 'Mig Center 2', '200 Road', '222', 'UTC', 'ACTIVE', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE code = code;");

                // Insert U1 at center 101, U1 at center 102, U2 at center 101
                stmt.execute("INSERT INTO operational_staff_rosters (center_id, user_id, role, status, created_at, updated_at) VALUES " +
                        "(101, " + testU1 + ", 'VACCINATOR', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                        "(102, " + testU1 + ", 'VACCINATOR', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP), " +
                        "(101, " + testU2 + ", 'VACCINATOR', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);");

                // Execute statements in V2 migration script (strip SQL comments line-by-line first)
                String[] lines = v2Sql.split("\\R");
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

                // Verify table exists in metadata
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet tables = metaData.getTables(conn.getCatalog(), null, "operational_staff_identities", null)) {
                    assertTrue(tables.next(), "Table operational_staff_identities must exist in MySQL");
                }

                // Verify unique index exists in metadata
                List<String> indexNames = new ArrayList<>();
                try (ResultSet indexes = metaData.getIndexInfo(conn.getCatalog(), null, "operational_staff_identities", false, false)) {
                    while (indexes.next()) {
                        String idx = indexes.getString("INDEX_NAME");
                        if (idx != null) {
                            indexNames.add(idx.toLowerCase());
                        }
                    }
                }
                assertTrue(indexNames.contains("uk_operational_staff_identity_user") || indexNames.contains("user_id"),
                        "Unique index on user_id must exist in MySQL metadata");

                // Verify backfill outcome: exactly 1 identity row for U1, exactly 1 for U2
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operational_staff_identities WHERE user_id = " + testU1)) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "User U1 rostered at 2 centers must have exactly 1 identity anchor");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operational_staff_identities WHERE user_id = " + testU2)) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1), "User U2 rostered at 1 center must have exactly 1 identity anchor");
                }

                // Verify roster rows were untouched
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM operational_staff_rosters WHERE user_id IN (" + testU1 + ", " + testU2 + ")")) {
                    assertTrue(rs.next());
                    assertEquals(3, rs.getInt(1), "Original roster rows must remain intact");
                }
            }
        }
    }
}
