package com.se2030.vaccinationportal.screening;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("mysql-testcontainers")
class Mf01ScreeningMigrationMySqlIntegrationTests {

    private static final String V5_MIGRATION = "db/migration/V5__mf01_screening_foundation.sql";
    private static final String V6_MIGRATION = "db/migration/V6__mf01_screening_corrections.sql";
    private static final Pattern MIGRATION_FILENAME = Pattern.compile("^V(\\d+)__.+\\.sql$");

    private static final Set<String> MF01_TABLES = Set.of(
            "consent_notice_version",
            "screening_question",
            "answer_option",
            "questionnaire",
            "questionnaire_version",
            "questionnaire_item",
            "eligibility_rule",
            "screening_submission",
            "screening_answer",
            "eligibility_assessment"
    );

    private static final List<String> MF01_TABLES_IN_DELETE_ORDER = List.of(
            "eligibility_assessment",
            "screening_answer",
            "screening_submission",
            "eligibility_rule",
            "questionnaire_item",
            "answer_option",
            "questionnaire_version",
            "screening_question",
            "consent_notice_version",
            "questionnaire"
    );

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("vaccination_portal_mf01_migration_test")
            .withUsername("test_user")
            .withPassword("test_password")
            .withReuse(false);

    @BeforeEach
    void resetMf01Schema() throws Exception {
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : MF01_TABLES_IN_DELETE_ORDER) {
                statement.execute("DROP TABLE IF EXISTS " + table);
            }
            statement.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }

    @Test
    @Timeout(30)
    void mf01FoundationCreatesExpectedSchemaOnEmptyMySql() throws Exception {
        try (Connection connection = connection()) {
            assertTrue(existingMf01Tables(connection).isEmpty(),
                    "MF-01 migration proof must begin with an empty schema");

            executeMigration(connection, V5_MIGRATION);

            assertEquals(MF01_TABLES, existingMf01Tables(connection));
            assertFalse(columnExists(connection, "screening_submission", "correction_instructions"));
            assertFalse(columnExists(connection, "screening_submission", "correction_requested_at"));

            assertUniqueIndexColumns(connection, "answer_option", List.of("question_id", "code"));
            assertUniqueIndexColumns(connection, "questionnaire_version",
                    List.of("questionnaire_id", "version_number"));
            assertUniqueIndexColumns(connection, "questionnaire_item", List.of("version_id", "display_order"));
            assertUniqueIndexColumns(connection, "questionnaire_item", List.of("version_id", "question_id"));
            assertUniqueIndexColumns(connection, "eligibility_rule", List.of("version_id", "rule_code"));
            assertUniqueIndexColumns(connection, "screening_answer", List.of("submission_id", "question_id"));
            assertUniqueIndexColumns(connection, "eligibility_assessment", List.of("submission_id"));

            assertRestrictiveForeignKey(
                    connection,
                    "answer_option",
                    "fk_answer_option_question",
                    "screening_question"
            );
            assertRestrictiveForeignKey(
                    connection,
                    "questionnaire_version",
                    "fk_questionnaire_version_questionnaire",
                    "questionnaire"
            );
            assertRestrictiveForeignKey(
                    connection,
                    "eligibility_assessment",
                    "fk_eligibility_assessment_sub",
                    "screening_submission"
            );
            assertForeignKey(
                    connection,
                    "screening_answer",
                    "fk_screening_ans_submission",
                    "screening_submission",
                    DatabaseMetaData.importedKeyCascade
            );
        }
    }

    @Test
    @Timeout(30)
    void mf01CorrectionMigrationAddsColumnsWithoutLosingExistingSubmission() throws Exception {
        try (Connection connection = connection()) {
            assertTrue(existingMf01Tables(connection).isEmpty(),
                    "MF-01 migration proof must begin with an empty schema");
            executeMigration(connection, V5_MIGRATION);

            assertFalse(columnExists(connection, "screening_submission", "correction_instructions"));
            assertFalse(columnExists(connection, "screening_submission", "correction_requested_at"));

            insertSubmissionFixture(connection);
            SubmissionSnapshot beforeUpgrade = readSubmission(connection, 401L);

            executeMigration(connection, V6_MIGRATION);

            assertTrue(columnExists(connection, "screening_submission", "correction_instructions"));
            assertTrue(columnExists(connection, "screening_submission", "correction_requested_at"));
            assertColumnNullable(connection, "screening_submission", "correction_instructions");
            assertColumnNullable(connection, "screening_submission", "correction_requested_at");
            assertEquals(beforeUpgrade, readSubmission(connection, 401L));

            try (Statement statement = connection.createStatement()) {
                assertEquals(1, statement.executeUpdate("""
                        UPDATE screening_submission
                        SET correction_instructions = 'Please clarify allergy details',
                            correction_requested_at = '2026-08-26 10:15:30.123456'
                        WHERE id = 401
                        """));

                try (ResultSet resultSet = statement.executeQuery("""
                        SELECT correction_instructions, correction_requested_at
                        FROM screening_submission
                        WHERE id = 401
                        """)) {
                    assertTrue(resultSet.next());
                    assertEquals("Please clarify allergy details", resultSet.getString("correction_instructions"));
                    assertNotNull(resultSet.getTimestamp("correction_requested_at"));
                    assertFalse(resultSet.next());
                }
            }
        }
    }

    @Test
    void classpathMigrationVersionsAreUniqueAndIncludeV1ThroughV6() throws Exception {
        Path migrationDirectory = Path.of("src", "main", "resources", "db", "migration");
        assertTrue(Files.isDirectory(migrationDirectory), "Classpath migration directory must exist");

        Map<Integer, List<String>> filenamesByVersion = new HashMap<>();
        try (Stream<Path> files = Files.list(migrationDirectory)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .forEach(filename -> {
                        Matcher matcher = MIGRATION_FILENAME.matcher(filename);
                        if (matcher.matches()) {
                            int version = Integer.parseInt(matcher.group(1));
                            filenamesByVersion.computeIfAbsent(version, ignored -> new ArrayList<>()).add(filename);
                        }
                    });
        }

        List<String> duplicates = filenamesByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> "V" + entry.getKey() + ": " + entry.getValue())
                .toList();
        assertTrue(duplicates.isEmpty(), "Duplicate classpath migration versions found: " + duplicates);

        for (int version = 1; version <= 6; version++) {
            assertTrue(filenamesByVersion.containsKey(version), "Missing classpath migration V" + version);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private void executeMigration(Connection connection, String classpathLocation) {
        ClassPathResource migration = new ClassPathResource(classpathLocation);
        assertTrue(migration.exists(), "Migration must exist: " + classpathLocation);
        ScriptUtils.executeSqlScript(connection, migration);
    }

    private Set<String> existingMf01Tables(Connection connection) throws Exception {
        Set<String> existingTables = new HashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME").toLowerCase();
                if (MF01_TABLES.contains(tableName)) {
                    existingTables.add(tableName);
                }
            }
        }
        return existingTables;
    }

    private boolean columnExists(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(),
                null,
                tableName,
                columnName
        )) {
            return columns.next();
        }
    }

    private void assertColumnNullable(Connection connection, String tableName, String columnName) throws Exception {
        try (ResultSet columns = connection.getMetaData().getColumns(
                connection.getCatalog(),
                null,
                tableName,
                columnName
        )) {
            assertTrue(columns.next(), "Missing column " + tableName + "." + columnName);
            assertEquals(
                    DatabaseMetaData.columnNullable,
                    columns.getInt("NULLABLE"),
                    "Column must remain nullable: " + tableName + "." + columnName
            );
        }
    }

    private void assertUniqueIndexColumns(
            Connection connection,
            String tableName,
            List<String> expectedColumns
    ) throws Exception {
        Map<String, SortedMap<Short, String>> indexes = new HashMap<>();
        try (ResultSet indexInfo = connection.getMetaData().getIndexInfo(
                connection.getCatalog(),
                null,
                tableName,
                true,
                false
        )) {
            while (indexInfo.next()) {
                String indexName = indexInfo.getString("INDEX_NAME");
                String columnName = indexInfo.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    indexes.computeIfAbsent(indexName, ignored -> new TreeMap<>())
                            .put(indexInfo.getShort("ORDINAL_POSITION"), columnName.toLowerCase());
                }
            }
        }

        boolean matchingIndexExists = indexes.values().stream()
                .map(index -> List.copyOf(index.values()))
                .anyMatch(expectedColumns::equals);
        assertTrue(
                matchingIndexExists,
                "Expected unique index on " + tableName + expectedColumns + ", found " + indexes
        );
    }

    private void assertRestrictiveForeignKey(
            Connection connection,
            String childTable,
            String foreignKeyName,
            String parentTable
    ) throws Exception {
        int deleteRule = importedForeignKeyDeleteRule(connection, childTable, foreignKeyName, parentTable);
        assertTrue(
                deleteRule == DatabaseMetaData.importedKeyRestrict
                        || deleteRule == DatabaseMetaData.importedKeyNoAction,
                "Expected RESTRICT/NO ACTION for " + foreignKeyName + " but metadata rule was " + deleteRule
        );
    }

    private void assertForeignKey(
            Connection connection,
            String childTable,
            String foreignKeyName,
            String parentTable,
            int expectedDeleteRule
    ) throws Exception {
        assertEquals(
                expectedDeleteRule,
                importedForeignKeyDeleteRule(connection, childTable, foreignKeyName, parentTable),
                "Unexpected delete rule for " + foreignKeyName
        );
    }

    private int importedForeignKeyDeleteRule(
            Connection connection,
            String childTable,
            String foreignKeyName,
            String parentTable
    ) throws Exception {
        try (ResultSet foreignKeys = connection.getMetaData().getImportedKeys(
                connection.getCatalog(),
                null,
                childTable
        )) {
            while (foreignKeys.next()) {
                if (foreignKeyName.equalsIgnoreCase(foreignKeys.getString("FK_NAME"))) {
                    assertEquals(parentTable, foreignKeys.getString("PKTABLE_NAME").toLowerCase());
                    return foreignKeys.getShort("DELETE_RULE");
                }
            }
        }
        throw new AssertionError("Missing foreign key " + foreignKeyName + " on table " + childTable);
    }

    private void insertSubmissionFixture(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO consent_notice_version
                        (id, notice_code, version_number, title, content, content_hash, published_at)
                    VALUES
                        (101, 'CONSENT-MIG', 1, 'Migration consent', 'Synthetic consent content',
                         'migration-content-hash', '2026-08-26 09:00:00.000000')
                    """);
            statement.executeUpdate("""
                    INSERT INTO questionnaire (id, code, name, description)
                    VALUES (201, 'QUESTIONNAIRE-MIG', 'Migration questionnaire', 'Synthetic migration fixture')
                    """);
            statement.executeUpdate("""
                    INSERT INTO questionnaire_version (id, questionnaire_id, version_number, status, published_at)
                    VALUES (301, 201, 1, 'PUBLISHED', '2026-08-26 09:30:00.000000')
                    """);
            assertEquals(1, statement.executeUpdate("""
                    INSERT INTO screening_submission
                        (id, citizen_id, campaign_id, version_id, consent_notice_version_id,
                         status, preliminary_result, consent_at, submitted_at)
                    VALUES
                        (401, 501, 601, 301, 101, 'PENDING_REVIEW', 'PRELIMINARY_REVIEW_REQUIRED',
                         '2026-08-26 10:00:00.000000', '2026-08-26 10:01:00.000000')
                    """));
        }
    }

    private SubmissionSnapshot readSubmission(Connection connection, long submissionId) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("""
                     SELECT id, citizen_id, campaign_id, version_id, consent_notice_version_id,
                            status, preliminary_result, consent_at, submitted_at
                     FROM screening_submission
                     WHERE id = %d
                     """.formatted(submissionId))) {
            assertTrue(resultSet.next(), "Expected screening submission " + submissionId);
            SubmissionSnapshot snapshot = new SubmissionSnapshot(
                    resultSet.getLong("id"),
                    resultSet.getLong("citizen_id"),
                    resultSet.getLong("campaign_id"),
                    resultSet.getLong("version_id"),
                    resultSet.getLong("consent_notice_version_id"),
                    resultSet.getString("status"),
                    resultSet.getString("preliminary_result"),
                    resultSet.getTimestamp("consent_at"),
                    resultSet.getTimestamp("submitted_at")
            );
            assertFalse(resultSet.next(), "Submission ID must remain unique");
            return snapshot;
        }
    }

    private record SubmissionSnapshot(
            long id,
            long citizenId,
            long campaignId,
            long versionId,
            long consentNoticeVersionId,
            String status,
            String preliminaryResult,
            Timestamp consentAt,
            Timestamp submittedAt
    ) {
    }
}
