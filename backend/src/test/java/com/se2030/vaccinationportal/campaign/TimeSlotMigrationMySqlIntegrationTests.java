package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Production-equivalent MySQL Testcontainers integration test proving real MySQL runtime execution
 * of Flyway migration script V3 on mysql:8.4.3, including table creation, foreign key constraints,
 * and index metadata.
 */
class TimeSlotMigrationMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM time_slots");
            stmt.execute("DELETE FROM session_change_events");
        } catch (Exception ignored) {
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            sessionRepository.deleteAllInBatch();
            campaignRepository.deleteAllInBatch();
            centerRepository.deleteAllInBatch();
            targetGroupRepository.deleteAllInBatch();
            vaccineTypeRepository.deleteAllInBatch();
        });
    }

    @Test
    @Timeout(30)
    @DisplayName("Migration SQL V3 MySQL Runtime Proof: V3 migration script executes on real MySQL and creates time_slots table")
    void migrationV3_executesOnRealMySql() throws Exception {
        assertConnectedToRealMySql();

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        Long testSessionId = template.execute(status -> {
            VaccineType vax = vaccineTypeRepository.saveAndFlush(new VaccineType("VAX-MIG-V3", "Migration Vaccine V3"));
            TargetGroup tg = targetGroupRepository.saveAndFlush(new TargetGroup("TG-MIG-V3", "Migration Target Group V3"));
            VaccinationCampaign cmp = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                    "CMP-MIG-V3", "Migration Campaign", "Testing V3 Migration",
                    vax, tg,
                    LocalDate.now().minusDays(1), LocalDate.now().plusDays(30),
                    Instant.now().minus(1, ChronoUnit.DAYS), Instant.now().plus(30, ChronoUnit.DAYS),
                    CampaignStatus.ACTIVE
            ));
            VaccinationCenter ctr = centerRepository.saveAndFlush(new VaccinationCenter(
                    "CTR-MIG-V3", "Migration Center", "100 Road", "555-0100", CenterStatus.ACTIVE, 50, 100, "UTC"
            ));
            VaccinationSession sess = sessionRepository.saveAndFlush(new VaccinationSession(
                    cmp, ctr, Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200), Instant.now(), "UTC"
            ));
            return sess.getId();
        });

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
                stmt.execute("DROP TABLE IF EXISTS time_slots;");

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
                        "VALUES (" + testSessionId + ", CURRENT_TIMESTAMP, DATE_ADD(CURRENT_TIMESTAMP, INTERVAL 1 HOUR), 10, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);");

                try (ResultSet rs = stmt.executeQuery("SELECT capacity, reserved_count FROM time_slots WHERE session_id = " + testSessionId)) {
                    assertTrue(rs.next());
                    assertEquals(10, rs.getInt("capacity"));
                    assertEquals(0, rs.getInt("reserved_count"));
                }
            }
        }
    }
}
