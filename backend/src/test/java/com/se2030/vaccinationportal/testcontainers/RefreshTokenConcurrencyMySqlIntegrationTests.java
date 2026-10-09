package com.se2030.vaccinationportal.testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.RefreshTokenSession;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import com.se2030.vaccinationportal.shared.auth.service.RefreshTokenSessionService;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * MySQL/InnoDB Testcontainers coverage of concurrently scheduled refresh attempts
 * and durable replay/family revocation. The start latch does not guarantee that
 * both transactions read the active root before either conditional UPDATE.
 *
 * <p>Verifies F-024-3:
 * <ul>
 *   <li>Runs against production-equivalent MySQL/InnoDB and full Flyway migrations (including V24);</li>
 *   <li>Two independently scheduled transactions present the same root session R;</li>
 *   <li>Exactly one contender successfully rotates and issues a successor;</li>
 *   <li>The losing contender is categorized as reuse and revokes the whole token family;</li>
 *   <li>Root session R persists {@code reuse_detected_at} and {@code revoked_at};</li>
 *   <li>The winner's successor is revoked by the loser's family revocation;</li>
 *   <li>Subsequent attempts to use the successor are rejected;</li>
 *   <li>An unrelated concurrent family U remains unrevoked, unstamped, and fully usable.</li>
 * </ul>
 * </p>
 */
@SpringBootTest
@Testcontainers
@Tag("mysql-testcontainers")
@ActiveProfiles("test")
public class RefreshTokenConcurrencyMySqlIntegrationTests {

    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>("mysql:8.4.3")
            .withDatabaseName("vaccination_portal_refresh")
            .withUsername("test_refresh_user")
            .withPassword("test_refresh_password")
            .withReuse(false);

    static {
        MYSQL_CONTAINER.start();
    }

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.open-in-view", () -> "false");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private RefreshTokenSessionService refreshTokenSessionService;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthActionTokenService authActionTokenService;

    private Role citizenRole;

    @BeforeEach
    void setUp() throws Exception {
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        try (Connection connection = dataSource.getConnection()) {
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
        }
        var applied = flyway.info().applied();
        assertEquals(28, applied.length, "The fresh schema must contain Flyway V0 through V27");
        assertEquals("27", flyway.info().current().getVersion().toString());
        assertTrue(Arrays.stream(applied).allMatch(migration -> migration.getState().isApplied()));
        cleanup();
        citizenRole = roleRepository.findByCode("ROLE_CITIZEN")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_CITIZEN", "Citizen")));
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        refreshTokenSessionRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    private UserAccount createActiveUser(String email) {
        UserAccount user = new UserAccount(email, passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        user.setFullName("Test User");
        user.setPhoneNumber("+94771234567");
        user.addRole(citizenRole);
        return userAccountRepository.save(user);
    }

    @Test
    @DisplayName("Concurrent MySQL refresh attempts persist reuse and revoke the winner successor")
    void concurrentRotationOnMySql_enforcesAtomicClaimAndFamilyRevocation() throws Exception {
        UserAccount userA = createActiveUser("mysql-concurrency-a@portal.gov");

        // 1. Issue root session R for user A (target family)
        var rootIssue = refreshTokenSessionService.issueRootSession(userA);
        String rawTokenR = rootIssue.rawToken();
        RefreshTokenSession rootSessionBefore = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(rawTokenR))
                .orElseThrow();
        Long rootSessionId = rootSessionBefore.getId();
        String familyId = rootSessionBefore.getTokenFamilyId();

        // 2. Issue root session U for user B (unrelated family) to prove isolation
        UserAccount userB = createActiveUser("mysql-concurrency-b@portal.gov");
        var unrelatedIssue = refreshTokenSessionService.issueRootSession(userB);
        String rawTokenU = unrelatedIssue.rawToken();
        RefreshTokenSession unrelatedSessionBefore = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(rawTokenU))
                .orElseThrow();
        Long unrelatedSessionId = unrelatedSessionBefore.getId();
        String unrelatedFamilyId = unrelatedSessionBefore.getTokenFamilyId();

        // 3. Release two independent refresh callers together; pre-claim reads are not synchronized
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicReference<String> winnerSuccessorToken = new AtomicReference<>();
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    AuthService.RefreshSessionResult result = authService.refresh(rawTokenR);
                    winnerSuccessorToken.set(result.refreshToken());
                    return true;
                } catch (BadCredentialsException e) {
                    return false;
                }
            }));
        }

        int successes = 0;
        int failures = 0;
        try {
            assertTrue(readyLatch.await(15, TimeUnit.SECONDS), "Both refresh callers must become ready");
            startLatch.countDown();
            for (Future<Boolean> f : results) {
                if (f.get(15, TimeUnit.SECONDS)) {
                    successes++;
                } else {
                    failures++;
                }
            }
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS), "Refresh workers must terminate");
        }

        // Exactly one rotation succeeds; loser experiences reuse/unclaimed token
        assertEquals(1, successes, "Exactly one concurrent rotation may succeed on MySQL/InnoDB");
        assertEquals(1, failures, "The competing attempt must fail with BadCredentialsException");

        // Proof 1: Root session R is revoked and has reuse_detected_at persisted in MySQL
        RefreshTokenSession rootAfterRace = refreshTokenSessionRepository.findById(rootSessionId).orElseThrow();
        assertNotNull(rootAfterRace.getRevokedAt(), "Root session R must be revoked in MySQL");
        assertNotNull(rootAfterRace.getReuseDetectedAt(),
                "Root session R must have reuse_detected_at persisted in MySQL by the loser");

        // Proof 2: Winner's successor belongs to same family and is revoked by loser's family revocation
        assertNotNull(winnerSuccessorToken.get(), "Winner must have received a valid successor refresh token");
        RefreshTokenSession successorInDb = refreshTokenSessionRepository
                .findByTokenHash(authActionTokenService.hashToken(winnerSuccessorToken.get()))
                .orElseThrow();
        assertEquals(familyId, successorInDb.getTokenFamilyId(), "Winner's successor must belong to familyId");
        assertNotNull(successorInDb.getRevokedAt(),
                "Winner's successor must be marked revoked in MySQL due to family revocation by loser");

        // Proof 3: Winner's successor cannot subsequently be used to refresh
        assertThrows(BadCredentialsException.class, () -> authService.refresh(winnerSuccessorToken.get()),
                "Winner's revoked successor token must not allow subsequent refresh in MySQL");

        // Proof 4: Unrelated family U is completely unaffected and remains usable
        RefreshTokenSession unrelatedInDb = refreshTokenSessionRepository.findById(unrelatedSessionId).orElseThrow();
        assertNull(unrelatedInDb.getRevokedAt(), "Unrelated family U must not be revoked");
        assertNull(unrelatedInDb.getReuseDetectedAt(), "Unrelated family U must not have reuse detected");
        assertEquals(unrelatedFamilyId, unrelatedInDb.getTokenFamilyId());
        assertDoesNotThrow(() -> authService.refresh(rawTokenU),
                "Unrelated family U active token must remain fully usable for refresh in MySQL");
    }
}
