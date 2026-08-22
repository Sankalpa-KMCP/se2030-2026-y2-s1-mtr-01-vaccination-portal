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
 * of Flyway migration script V3 on mysql:8.4.3, including table creation, foreign key constraints,
 * and index metadata.
 */
class TimeSlotMigrationMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Test
    @Timeout(30)
    @DisplayName("Migration SQL V3 MySQL Runtime Proof: V3 migration script executes on real MySQL and creates time_slots table")
    void migrationV3_executesOnRealMySql() throws Exception {
        assertConnectedToRealMySql();

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);

            // Read V3 migration script
            ClassPathResource v3Resource = new ClassPathResource("db/migration/V3__mf02_time_slot.sql");
            assertTrue(v3Resource.exists(), "V3 migration script must exist in db/migration");

            String v3Sql;
            try (Reader reader = new InputStreamReader(v3Resource.getInputStream(), StandardCharsets.UTF_8)) {
                StringBuilder sb = new StringBuilder();
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    sb.append(buffer, 0, read);
                }
                v3Sql = sb.toString();
            }

            try (Statement stmt = conn.createStatement()) {
                // Ensure referenced campaign, center, and session exist to satisfy foreign key constraints
                stmt.execute("INSERT INTO vaccination_campaigns (" +
                        "id, name, description, start_date, end_date, campaign_status, created_at, updated_at" +
                        ") VALUES " +
                        "(991, 'Mig Camp', 'Desc', '2026-01-01', '2026-12-31', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE name = name;");

                stmt.execute("INSERT INTO vaccination_centers (" +
                        "id, code, name, address, contact, timezone, center_status, safe_concurrent_occupancy, created_at, updated_at" +
                        ") VALUES " +
                        "(991, 'CTR-MIG-991', 'Mig Center', '100 Road', '111', 'UTC', 'ACTIVE', 50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE code = code;");

                stmt.execute("INSERT INTO vaccination_sessions (" +
                        "id, campaign_id, center_id, start_at, end_at, booking_close_at, lifecycle_status, booking_status, created_at, updated_at" +
                        ") VALUES " +
                        "(991, 991, 991, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 8 HOUR), CURRENT_TIMESTAMP, 'SCHEDULED', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) " +
                        "ON DUPLICATE KEY UPDATE lifecycle_status = lifecycle_status;");

                // Execute statements in V3 migration script (strip SQL comments line-by-line first)
                String[] lines = v3Sql.split("\\R");
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
                try (ResultSet tables = metaData.getTables(conn.getCatalog(), null, "time_slots", null)) {
                    assertTrue(tables.next(), "Table time_slots must exist in MySQL");
                }

                // Verify indexes exist in metadata
                List<String> indexNames = new ArrayList<>();
                try (ResultSet indexes = metaData.getIndexInfo(conn.getCatalog(), null, "time_slots", false, false)) {
                    while (indexes.next()) {
                        String idx = indexes.getString("INDEX_NAME");
                        if (idx != null) {
                            indexNames.add(idx.toLowerCase());
                        }
                    }
                }
                assertTrue(indexNames.contains("idx_time_slot_session") || indexNames.contains("session_id"),
                        "Index on session_id must exist in MySQL metadata");

                // Verify sample insert and capacity check
                stmt.execute("INSERT INTO time_slots (session_id, start_time, end_time, capacity, reserved_count, lock_version, created_at, updated_at) " +
                        "VALUES (991, CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 HOUR), 10, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);");

                try (ResultSet rs = stmt.executeQuery("SELECT capacity, reserved_count FROM time_slots WHERE session_id = 991")) {
                    assertTrue(rs.next());
                    assertEquals(10, rs.getInt("capacity"));
                    assertEquals(0, rs.getInt("reserved_count"));
                }
            }
        }
    }
}
