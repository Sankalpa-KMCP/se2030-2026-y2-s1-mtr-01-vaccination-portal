package com.se2030.vaccinationportal.campaign.session.planning;

import com.se2030.vaccinationportal.campaign.CampaignTestFixtures;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.reference.repository.TargetGroupRepository;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.center.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import com.se2030.vaccinationportal.campaign.reference.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class TimeSlotIntegrityMigrationMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    private static final String MIGRATION = "db/migration/V13__mf02_time_slot_integrity_constraints.sql";

    @Autowired private VaccinationSessionRepository sessionRepository;
    @Autowired private VaccinationCampaignRepository campaignRepository;
    @Autowired private VaccinationCenterRepository centerRepository;
    @Autowired private VaccineTypeRepository vaccineTypeRepository;
    @Autowired private TargetGroupRepository targetGroupRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM time_slots");
            statement.execute("DELETE FROM session_change_events");
            // Campaign lifecycle events reference the campaign; they must be cleared
            // before the campaign row or the FK blocks the whole teardown.
            statement.execute("DELETE FROM campaign_lifecycle_events");
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

    @TestFactory
    @Timeout(30)
    Stream<DynamicTest> migrationV13EnforcesTimeSlotChecksOnRealMySql() throws Exception {
        assertConnectedToRealMySql();
        long sessionId = createSession();
        executeMigration();

        return Stream.of(
                DynamicTest.dynamicTest("V13 creates all deterministic TimeSlot CHECK constraints", this::assertConstraintMetadata),
                DynamicTest.dynamicTest("CHECK rejects capacity <= 0", () ->
                        assertInsertRejected(sessionId, "2026-09-10 08:00:00", "2026-09-10 09:00:00", 0, 0)),
                DynamicTest.dynamicTest("CHECK rejects reserved_count < 0", () ->
                        assertInsertRejected(sessionId, "2026-09-10 09:00:00", "2026-09-10 10:00:00", 5, -1)),
                DynamicTest.dynamicTest("CHECK rejects reserved_count > capacity", () ->
                        assertInsertRejected(sessionId, "2026-09-10 10:00:00", "2026-09-10 11:00:00", 4, 5)),
                DynamicTest.dynamicTest("CHECK rejects start_time >= end_time", () ->
                        assertInsertRejected(sessionId, "2026-09-10 11:00:00", "2026-09-10 11:00:00", 5, 0)),
                DynamicTest.dynamicTest("Valid TimeSlot row persists", () ->
                        assertDoesNotThrow(() -> insertSlot(
                                sessionId, "2026-09-10 12:00:00", "2026-09-10 13:00:00", 5, 2
                        )))
        );
    }

    private long createSession() {
        VaccineType vaccineType = vaccineTypeRepository.saveAndFlush(
                new VaccineType("VAX-TS-V13", "TimeSlot V13 vaccine", "dose", true)
        );
        TargetGroup targetGroup = targetGroupRepository.saveAndFlush(
                new TargetGroup("TG-TS-V13", "TimeSlot V13 target", true)
        );
        VaccinationCenter center = centerRepository.saveAndFlush(new VaccinationCenter(
                "CTR-TS-V13", "TimeSlot V13 center", "100 Migration Way", "555-0130",
                CenterStatus.ACTIVE, 50, 20, "UTC"
        ));
        VaccinationCampaign campaign = campaignRepository.saveAndFlush(CampaignTestFixtures.campaign(
                "CMP-TS-V13",
                "TimeSlot V13 campaign",
                "Desc",
                vaccineType,
                targetGroup,
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 30),
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-09-29T00:00:00Z"),
                CampaignStatus.ACTIVE
        ));
        return sessionRepository.saveAndFlush(new VaccinationSession(
                campaign,
                center,
                Instant.parse("2026-09-10T08:00:00Z"),
                Instant.parse("2026-09-10T14:00:00Z"),
                Instant.parse("2026-09-10T07:00:00Z"),
                "UTC"
        )).getId();
    }

    private void executeMigration() throws Exception {
        ClassPathResource resource = new ClassPathResource(MIGRATION);
        assertTrue(resource.exists(), "V13 migration must exist");

        String migrationSql;
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder sql = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                sql.append(buffer, 0, read);
            }
            migrationSql = sql.toString();
        }

        String executableSql = migrationSql.lines()
                .filter(line -> !line.trim().startsWith("--"))
                .reduce("", (left, right) -> left + right + "\n")
                .trim();
        if (executableSql.endsWith(";")) {
            executableSql = executableSql.substring(0, executableSql.length() - 1);
        }

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(executableSql);
        }
    }

    private void assertConstraintMetadata() throws SQLException {
        Set<String> expected = Set.of(
                "chk_time_slots_capacity_positive",
                "chk_time_slots_reserved_nonnegative",
                "chk_time_slots_reserved_within_capacity",
                "chk_time_slots_time_order"
        );
        Set<String> actual = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT CONSTRAINT_NAME
                     FROM information_schema.TABLE_CONSTRAINTS
                     WHERE CONSTRAINT_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'time_slots'
                       AND CONSTRAINT_TYPE = 'CHECK'
                     """);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                actual.add(resultSet.getString(1).toLowerCase());
            }
        }
        assertTrue(actual.containsAll(expected));
        assertEquals(4, expected.stream().filter(actual::contains).count());
    }

    private void assertInsertRejected(
            long sessionId,
            String startTime,
            String endTime,
            int capacity,
            int reservedCount
    ) {
        assertThrows(SQLException.class, () -> insertSlot(
                sessionId, startTime, endTime, capacity, reservedCount
        ));
    }

    private void insertSlot(
            long sessionId,
            String startTime,
            String endTime,
            int capacity,
            int reservedCount
    ) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO time_slots
                         (session_id, start_time, end_time, capacity, reserved_count,
                          lock_version, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                     """)) {
            statement.setLong(1, sessionId);
            statement.setString(2, startTime);
            statement.setString(3, endTime);
            statement.setInt(4, capacity);
            statement.setInt(5, reservedCount);
            statement.executeUpdate();
        }
    }
}
