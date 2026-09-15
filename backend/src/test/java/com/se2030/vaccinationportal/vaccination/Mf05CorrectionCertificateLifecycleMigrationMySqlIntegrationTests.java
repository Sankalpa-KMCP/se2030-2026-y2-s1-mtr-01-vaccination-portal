package com.se2030.vaccinationportal.vaccination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Verifies V15 against real MySQL, including migration of existing V11 certificate data. */
@SpringBootTest
@Testcontainers
@Tag("mysql-testcontainers")
class Mf05CorrectionCertificateLifecycleMigrationMySqlIntegrationTests {

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("mf05_lifecycle_migration")
            .withUsername("mf05_test_user")
            .withPassword("mf05_test_password")
            .withReuse(false);

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.flyway.target", () -> "14");
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway initialFlyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void v15MigratesExistingDataAndEnforcesCorrectionAndCertificateLifecycleSchema() {
        assertEquals("14", initialFlyway.info().current().getVersion().getVersion());
        insertV11Fixture();

        Flyway v15Flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("15"))
                .load();
        MigrateResult result = v15Flyway.migrate();

        assertEquals(1, result.migrationsExecuted);
        assertEquals("15", v15Flyway.info().current().getVersion().getVersion());
        assertEquals(16, v15Flyway.info().applied().length);
        assertEquals(1, jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = 'vaccination_corrections'
                """,
                Integer.class
        ));

        assertBackfilledLegacyCertificate();
        assertCorrectionConstraints();
        assertCertificateHistoryAndLifecycleConstraints();
    }

    private void insertV11Fixture() {
        jdbcTemplate.update("""
                INSERT INTO vaccine_types (id, code, name)
                VALUES (9101, 'MF05-MIGRATION-VACCINE', 'MF-05 Migration Vaccine')
                """);
        jdbcTemplate.update("""
                INSERT INTO target_groups (id, code, description)
                VALUES (9101, 'MF05-MIGRATION-GROUP', 'MF-05 migration target group')
                """);
        jdbcTemplate.update("""
                INSERT INTO vaccination_campaigns (
                    id, code, name, vaccine_type_id, target_group_id,
                    service_start_date, service_end_date, booking_open_at, booking_close_at, status
                ) VALUES (
                    9101, 'MF05-MIGRATION-CAMPAIGN', 'MF-05 Migration Campaign', 9101, 9101,
                    '2026-09-01', '2026-12-31', '2026-08-01 00:00:00', '2026-12-30 00:00:00', 'PUBLISHED'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO vaccination_centers (
                    id, code, name, address, contact, timezone, center_status, safe_concurrent_occupancy
                ) VALUES (
                    9101, 'MF05-MIG-CENTER', 'MF-05 Migration Center', 'Test address',
                    '0110000000', 'UTC', 'ACTIVE', 10
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO appointment (id, citizen_id, campaign_id, slot_id, status, booked_at)
                VALUES (9101, 9201, 9101, 9301, 'CHECKED_IN', '2026-08-20 08:00:00')
                """);
        jdbcTemplate.update("""
                INSERT INTO vaccination_records (
                    id, appointment_id, citizen_id, campaign_id, center_id, vaccinator_id,
                    vaccine_batch_id, dose_number, administration_time, next_dose_date,
                    authorized_notes, finalized_at
                ) VALUES (
                    9101, 9101, 9201, 9101, 9101, 9401,
                    9501, 2, '2026-09-10 09:15:00.123456', '2026-10-10',
                    'Historical record', '2026-09-10 09:20:00.123456'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    id, vaccination_record_id, certificate_reference, issued_at,
                    status, verification_token, lock_version
                ) VALUES (
                    9101, 9101, 'CERT-MF05-LEGACY', '2026-09-10 09:30:00.123456',
                    'VALID', 'mf05-legacy-verification-token', 0
                )
                """);
    }

    private void assertBackfilledLegacyCertificate() {
        assertEquals(9501L, jdbcTemplate.queryForObject(
                "SELECT issued_vaccine_batch_id FROM digital_certificates WHERE id = 9101",
                Long.class
        ));
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT issued_dose_number FROM digital_certificates WHERE id = 9101",
                Integer.class
        ));
        assertEquals("2026-09-10 09:15:00.123456", jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(issued_administration_time, '%Y-%m-%d %H:%i:%s.%f') "
                        + "FROM digital_certificates WHERE id = 9101",
                String.class
        ));
        assertEquals("CERT-MF05-LEGACY", jdbcTemplate.queryForObject(
                "SELECT certificate_reference FROM digital_certificates WHERE id = 9101",
                String.class
        ));
    }

    private void assertCorrectionConstraints() {
        jdbcTemplate.update("""
                INSERT INTO vaccination_corrections (
                    vaccination_record_id, field_name, old_value, new_value,
                    reason, corrected_by, corrected_at, status
                ) VALUES (
                    9101, 'AUTHORIZED_NOTES', NULL, NULL,
                    'Clear note', 9401, '2026-09-10 10:00:00.123456', 'APPLIED'
                )
                """);
        assertNull(jdbcTemplate.queryForObject(
                "SELECT old_value FROM vaccination_corrections WHERE vaccination_record_id = 9101",
                String.class
        ));
        assertNull(jdbcTemplate.queryForObject(
                "SELECT new_value FROM vaccination_corrections WHERE vaccination_record_id = 9101",
                String.class
        ));

        assertRejectedByDatabase(() -> jdbcTemplate.update("""
                INSERT INTO vaccination_corrections (
                    vaccination_record_id, field_name, reason, corrected_by, corrected_at, status
                ) VALUES (9101, 'citizenId', 'Invalid field', 9401, NOW(6), 'APPLIED')
                """));
        assertRejectedByDatabase(() -> jdbcTemplate.update("""
                INSERT INTO vaccination_corrections (
                    vaccination_record_id, field_name, reason, corrected_by, corrected_at, status
                ) VALUES (9101, 'DOSE_NUMBER', 'Invalid status', 9401, NOW(6), 'UNKNOWN')
                """));
    }

    private void assertCertificateHistoryAndLifecycleConstraints() {
        jdbcTemplate.update("""
                UPDATE digital_certificates
                SET status = 'SUPERSEDED',
                    superseded_at = '2026-09-10 10:30:00.123456',
                    superseded_by = 9401,
                    supersession_reason = 'Corrected certificate'
                WHERE id = 9101
                """);
        jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    id, vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version, reissued_from_certificate_id,
                    issued_vaccine_batch_id, issued_dose_number, issued_administration_time
                ) VALUES (
                    9102, 9101, 'CERT-MF05-VALID', '2026-09-10 10:31:00.123456', 'VALID',
                    'mf05-valid-verification-token', 0, 9101,
                    9502, 2, '2026-09-10 09:15:00.123456'
                )
                """);
        jdbcTemplate.update(
                "UPDATE digital_certificates SET superseded_by_certificate_id = 9102 WHERE id = 9101"
        );
        jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    id, vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version, issued_vaccine_batch_id,
                    issued_dose_number, issued_administration_time
                ) VALUES (
                    9103, 9101, 'CERT-MF05-REVOKED', '2026-09-10 10:32:00.123456', 'REVOKED',
                    'mf05-revoked-verification-token', 0, 9501,
                    2, '2026-09-10 09:15:00.123456'
                )
                """);
        jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    id, vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version, issued_vaccine_batch_id,
                    issued_dose_number, issued_administration_time
                ) VALUES (
                    9104, 9101, 'CERT-MF05-SUPERSEDED', '2026-09-10 10:33:00.123456', 'SUPERSEDED',
                    'mf05-superseded-verification-token', 0, 9501,
                    2, '2026-09-10 09:15:00.123456'
                )
                """);

        assertEquals(4, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM digital_certificates WHERE vaccination_record_id = 9101",
                Integer.class
        ));
        assertEquals(9101L, jdbcTemplate.queryForObject(
                "SELECT reissued_from_certificate_id FROM digital_certificates WHERE id = 9102",
                Long.class
        ));
        assertEquals(9102L, jdbcTemplate.queryForObject(
                "SELECT superseded_by_certificate_id FROM digital_certificates WHERE id = 9101",
                Long.class
        ));

        assertRejectedByDatabase(() -> jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version
                ) VALUES (
                    9101, 'CERT-MF05-SECOND-VALID', NOW(6), 'VALID',
                    'mf05-second-valid-token', 0
                )
                """));
        assertRejectedByDatabase(() -> jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version
                ) VALUES (
                    9101, 'CERT-MF05-LEGACY', NOW(6), 'REVOKED',
                    'mf05-unique-token-for-reference-test', 0
                )
                """));
        assertRejectedByDatabase(() -> jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version
                ) VALUES (
                    9101, 'CERT-MF05-UNIQUE-REFERENCE', NOW(6), 'REVOKED',
                    'mf05-valid-verification-token', 0
                )
                """));
        assertRejectedByDatabase(() -> jdbcTemplate.update("""
                INSERT INTO digital_certificates (
                    vaccination_record_id, certificate_reference, issued_at, status,
                    verification_token, lock_version
                ) VALUES (
                    9101, 'CERT-MF05-INVALID-STATUS', NOW(6), 'UNKNOWN',
                    'mf05-invalid-status-token', 0
                )
                """));
    }

    private void assertRejectedByDatabase(Runnable operation) {
        assertThrows(DataIntegrityViolationException.class, operation::run);
    }
}
