package com.se2030.vaccinationportal.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * MySQL Testcontainers validation for the R05 screening submission active-uniqueness invariant.
 * Runs against the real Flyway-migrated schema (V0–V21) on production-equivalent MySQL and proves:
 * <ul>
 *   <li>the V21 migration applies on the migrated schema (generated column + unique constraint exist);</li>
 *   <li>a second ACTIVE submission for the same citizen/campaign violates
 *       {@code uk_screening_submission_active_citizen_campaign};</li>
 *   <li>a terminal (REVIEWED) historical submission still permits legitimate later rescreening.</li>
 * </ul>
 */
@Tag("mysql-testcontainers")
@ActiveProfiles("test")
class ScreeningSubmissionMySqlActiveUniquenessIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @DynamicPropertySource
    static void overrideSchemaManagement(DynamicPropertyRegistry registry) {
        // Schema must come exclusively from the real Flyway migrations V0–V21, not from Hibernate
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static long insertRowReturningId(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql, Statement.RETURN_GENERATED_KEYS);
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private long insertActiveSubmission(Connection connection, long citizenId, long campaignId,
                                        long versionId, long consentId, String status) throws SQLException {
        String sql = "INSERT INTO screening_submission (citizen_id, campaign_id, version_id, "
                + "consent_notice_version_id, status, preliminary_result, consent_at, submitted_at) "
                + "VALUES (?, ?, ?, ?, ?, NULL, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, citizenId);
            statement.setLong(2, campaignId);
            statement.setLong(3, versionId);
            statement.setLong(4, consentId);
            statement.setString(5, status);
            statement.setTimestamp(6, Timestamp.from(Instant.now()));
            statement.setTimestamp(7, Timestamp.from(Instant.now()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private long countActiveSubmissions(Connection connection, long citizenId, long campaignId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM screening_submission WHERE citizen_id = ? AND campaign_id = ? "
                + "AND status IN ('DRAFT','PENDING_REVIEW','CORRECTIONS_REQUIRED')";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, citizenId);
            statement.setLong(2, campaignId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    @Test
    void v21MigrationProducesGeneratedColumnAndUniqueInvariant() throws Exception {
        assertConnectedToRealMySql();
        try (Connection connection = dataSource.getConnection()) {
            // The generated column must exist and derive from the active statuses
            try (Statement statement = connection.createStatement();
                 ResultSet columns = statement.executeQuery(
                         "SELECT GENERATION_EXPRESSION FROM information_schema.COLUMNS "
                                 + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_submission' "
                                 + "AND COLUMN_NAME = 'active_campaign_id'")) {
                assertTrue(columns.next(), "active_campaign_id generated column must exist");
                String expression = columns.getString(1).replace("`", "").toUpperCase();
                assertTrue(expression.contains("DRAFT") && expression.contains("PENDING_REVIEW")
                                && expression.contains("CORRECTIONS_REQUIRED"),
                        "Generation expression must cover the active statuses: " + expression);
                assertFalse(expression.contains("REVIEWED"),
                        "Generation expression must not activate for REVIEWED: " + expression);
            }

            // The unique constraint must exist on citizen_id + active_campaign_id
            int uniqueColumns;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(DISTINCT COLUMN_NAME) FROM information_schema.STATISTICS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'screening_submission' "
                            + "AND INDEX_NAME = 'uk_screening_submission_active_citizen_campaign' AND NON_UNIQUE = 0")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    uniqueColumns = resultSet.getInt(1);
                }
            }
            assertEquals(2, uniqueColumns,
                    "Unique invariant must span citizen_id and active_campaign_id");

            // Flyway must record V21 as successfully applied
            try (Statement statement = connection.createStatement();
                 ResultSet history = statement.executeQuery(
                         "SELECT success FROM flyway_schema_history WHERE version = '21'")) {
                assertTrue(history.next(), "V21 must be recorded in flyway_schema_history");
                assertTrue(history.getBoolean(1), "V21 migration must have applied successfully");
            }
        }
    }

    @Test
    void duplicateActiveSubmission_violatesUniqueInvariantOnMySql() throws Exception {
        assertConnectedToRealMySql();
        try (Connection connection = dataSource.getConnection()) {
            long questionnaireId = insertRowReturningId(connection,
                    "INSERT INTO questionnaire (code, name, description) VALUES ('R05-MySQL-Q', 'R05 MySQL', 'fixture')");
            long versionId = insertRowReturningId(connection,
                    "INSERT INTO questionnaire_version (questionnaire_id, version_number, status, published_at) "
                            + "VALUES (" + questionnaireId + ", 1, 'PUBLISHED', NOW(6))");
            long consentId = insertRowReturningId(connection,
                    "INSERT INTO consent_notice_version (notice_code, version_number, title, content, content_hash, published_at) "
                            + "VALUES ('R05_MYSQL_NOTICE', 1, 'Consent', 'Content', 'hash-mysql', NOW(6))");

            long citizenId = 88001L;
            long campaignId = 88002L;

            long firstId = insertActiveSubmission(connection, citizenId, campaignId, versionId, consentId, "PENDING_REVIEW");
            assertTrue(firstId > 0, "First active submission must insert successfully");

            // Second ACTIVE submission for the same citizen/campaign must violate the database invariant
            assertThrows(SQLIntegrityConstraintViolationException.class,
                    () -> {
                        try (Connection second = dataSource.getConnection()) {
                            insertActiveSubmission(second, citizenId, campaignId, versionId, consentId, "PENDING_REVIEW");
                        }
                    },
                    "MySQL must reject a second active submission for the same citizen/campaign");

            assertEquals(1, countActiveSubmissions(connection, citizenId, campaignId));
        }
    }

    @Test
    void terminalReviewedSubmission_stillPermitsLegitimateRescreening() throws Exception {
        assertConnectedToRealMySql();
        try (Connection connection = dataSource.getConnection()) {
            long questionnaireId = insertRowReturningId(connection,
                    "INSERT INTO questionnaire (code, name, description) VALUES ('R05-MySQL-Q2', 'R05 MySQL 2', 'fixture')");
            long versionId = insertRowReturningId(connection,
                    "INSERT INTO questionnaire_version (questionnaire_id, version_number, status, published_at) "
                            + "VALUES (" + questionnaireId + ", 1, 'PUBLISHED', NOW(6))");
            long consentId = insertRowReturningId(connection,
                    "INSERT INTO consent_notice_version (notice_code, version_number, title, content, content_hash, published_at) "
                            + "VALUES ('R05_MYSQL_NOTICE2', 1, 'Consent', 'Content', 'hash-mysql-2', NOW(6))");

            long citizenId = 88003L;
            long campaignId = 88004L;

            long historicalId = insertActiveSubmission(connection, citizenId, campaignId, versionId, consentId, "PENDING_REVIEW");
            execute(connection, "UPDATE screening_submission SET status = 'REVIEWED' WHERE id = " + historicalId);
            assertEquals(0, countActiveSubmissions(connection, citizenId, campaignId),
                    "Terminal REVIEWED submission is no longer active");

            // Legitimate later rescreening must succeed against the database invariant
            long rescreenedId = insertActiveSubmission(connection, citizenId, campaignId, versionId, consentId, "PENDING_REVIEW");
            assertTrue(rescreenedId > 0, "Rescreening after a terminal submission must be allowed");
            assertEquals(1, countActiveSubmissions(connection, citizenId, campaignId));

            // And the invariant still holds against a THIRD active submission
            assertThrows(SQLException.class,
                    () -> {
                        try (Connection third = dataSource.getConnection()) {
                            insertActiveSubmission(third, citizenId, campaignId, versionId, consentId, "DRAFT");
                        }
                    });
        }
    }
}
