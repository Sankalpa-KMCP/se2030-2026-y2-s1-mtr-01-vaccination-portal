package com.se2030.vaccinationportal.testcontainers;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic Fresh Database Bootstrap and Schema Migration Integration Test.
 *
 * <p>Verifies X3 / VP-002: A completely empty, clean MySQL database instance
 * bootstraps successfully through Flyway (V0-V15), seeds canonical static data,
 * satisfies Hibernate schema validation ({@code ddl-auto=validate}), and proves
 * idempotent migration execution.</p>
 */
@SpringBootTest
@Testcontainers
@Tag("mysql-testcontainers")
class FreshDatabaseBootstrapMySqlIntegrationTests {

    private static final String MYSQL_IMAGE = "mysql:8.4.3";

    private static final MySQLContainer<?> FRESH_MYSQL_CONTAINER;

    static {
        FRESH_MYSQL_CONTAINER = new MySQLContainer<>(MYSQL_IMAGE)
                .withDatabaseName("vaccination_portal_fresh")
                .withUsername("test_fresh_user")
                .withPassword("test_fresh_password")
                .withReuse(false);
        FRESH_MYSQL_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", FRESH_MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", FRESH_MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", FRESH_MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    private static final List<String> EXPECTED_44_TABLES = Arrays.asList(
            "answer_option",
            "appointment",
            "campaign_center_assignments",
            "campaign_dose_programmes",
            "campaign_lifecycle_events",
            "campaign_location_targets",
            "campaign_questionnaire_assignments",
            "center_closures",
            "citizen_campaign_booking_guard",
            "citizen_profiles",
            "consent_notice_version",
            "digital_certificates",
            "eligibility_assessment",
            "eligibility_rule",
            "operating_hours",
            "operational_resources",
            "operational_staff_identities",
            "operational_staff_rosters",
            "questionnaire",
            "questionnaire_item",
            "questionnaire_version",
            "refresh_token_sessions",
            "roles",
            "screening_answer",
            "screening_question",
            "screening_submission",
            "screening_triggered_rule_evidence",
            "session_capacity_plans",
            "session_change_events",
            "session_recurrence_rule_weekdays",
            "session_recurrence_rules",
            "session_resource_assignments",
            "session_staff_assignments",
            "target_groups",
            "time_slots",
            "user_accounts",
            "user_roles",
            "vaccination_campaigns",
            "vaccination_centers",
            "vaccination_drafts",
            "vaccination_records",
            "vaccination_sessions",
            "vaccine_types",
            "waitlist_entry"
    );

    private static final Set<String> EXPECTED_8_ROLES = Set.of(
            "ROLE_ADMIN",
            "ROLE_CENTER_MANAGER",
            "ROLE_HEALTHCARE_OFFICER",
            "ROLE_CAMPAIGN_OFFICER",
            "ROLE_INVENTORY_OFFICER",
            "ROLE_CITIZEN",
            "ROLE_MANAGEMENT_USER",
            "ROLE_SUPER_ADMIN"
    );

    @Test
    @DisplayName("Should successfully connect to real MySQL database container")
    void shouldConnectToRealMySql() throws Exception {
        assertNotNull(dataSource, "DataSource must be injected");
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertEquals("MySQL", metaData.getDatabaseProductName(), "Must connect to real MySQL engine");
            String version = metaData.getDatabaseProductVersion();
            assertNotNull(version, "Database version must not be null");
            assertTrue(version.startsWith("8.") || version.startsWith("9."),
                    "Database version must be MySQL 8.x or 9.x: " + version);
        }
    }

    @Test
    @DisplayName("Should apply all migration versions V0 through V15 with SUCCESS status")
    void shouldApplyAllMigrationsSuccessfully() {
        assertNotNull(flyway, "Flyway bean must be configured and injected");
        MigrationInfo[] appliedMigrations = flyway.info().all();
        assertEquals(16, appliedMigrations.length, "Expected exactly 16 migrations (V0-V15)");

        for (int i = 0; i <= 15; i++) {
            String expectedVersion = String.valueOf(i);
            MigrationInfo info = appliedMigrations[i];
            assertEquals(expectedVersion, info.getVersion().getVersion(), "Migration version mismatch at index " + i);
            assertEquals(MigrationState.SUCCESS, info.getState(), "Migration V" + i + " must be in SUCCESS state");
        }
    }

    @Test
    @DisplayName("Should verify presence of all 44 physical tables plus flyway schema history")
    void shouldVerifyAll44RequiredTablesExist() throws Exception {
        Set<String> actualTables = new HashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        }

        assertTrue(actualTables.contains("flyway_schema_history"), "flyway_schema_history table must exist");

        for (String expectedTable : EXPECTED_44_TABLES) {
            assertTrue(
                    actualTables.contains(expectedTable.toLowerCase()),
                    "Required application table missing from migrated database: " + expectedTable
            );
        }

        // Verify total application tables count (excluding flyway history table)
        long appTableCount = actualTables.stream()
                .filter(t -> !t.equals("flyway_schema_history"))
                .count();
        assertEquals(44, appTableCount, "Database must contain exactly 44 application/runtime tables");
    }

    @Test
    @DisplayName("Should verify exactly 8 canonical static roles seeded by V4 and V15")
    void shouldVerifyCanonicalRolesSeeded() throws Exception {
        Set<String> seededRoles = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT code FROM roles")) {
            while (rs.next()) {
                seededRoles.add(rs.getString("code"));
            }
        }

        assertEquals(8, seededRoles.size(), "roles table must contain exactly 8 seeded roles");
        assertEquals(EXPECTED_8_ROLES, seededRoles, "Seeded roles must exactly match canonical 8 roles");
    }

    @Test
    @DisplayName("Should verify no default user accounts are seeded into user_accounts")
    void shouldVerifyNoDefaultUserAccountsSeeded() throws Exception {
        int userCount = -1;
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM user_accounts")) {
            if (rs.next()) {
                userCount = rs.getInt("total");
            }
        }
        assertEquals(0, userCount, "Fresh database must contain zero user accounts");
    }

    @Test
    @DisplayName("Should verify Flyway migration execution is idempotent (zero new migrations applied)")
    void shouldVerifyFlywayMigrationIsIdempotent() {
        assertNotNull(flyway, "Flyway bean must be present");
        MigrateResult result = flyway.migrate();
        assertEquals(0, result.migrationsExecuted, "Second migration execution must execute 0 new migrations");
        assertTrue(result.success, "Second migration execution must report success");
    }
}
