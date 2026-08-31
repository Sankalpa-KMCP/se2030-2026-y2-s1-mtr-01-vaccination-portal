package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.StaffApprovalRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRejectionRequest;
import com.se2030.vaccinationportal.shared.auth.exception.StaffRequestNotPendingException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.StaffRequestReviewService;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class StaffRequestReviewMySqlIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @Autowired private StaffRequestReviewService reviewService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private UserAccount administrator;
    private Role requestedRole;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer")
        ));
        requestedRole = roleRepository.findByCode("ROLE_HEALTHCARE_OFFICER").orElseThrow();
        administrator = new UserAccount(
                "mysql-admin@example.com", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        administrator.addRole(roleRepository.findByCode("ROLE_ADMIN").orElseThrow());
        administrator = userAccountRepository.saveAndFlush(administrator);
    }

    @Test
    @Timeout(30)
    @DisplayName("approval persists authoritative role and reviewer foreign key on real MySQL")
    void approval_persistsReviewIntegrityOnMySql() throws Exception {
        assertConnectedToRealMySql();
        UserAccount pending = savePending("mysql-approve@example.com");

        reviewService.approve(
                pending.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), administrator.getId());

        UserAccount approved = userAccountRepository.findStaffRequestById(pending.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, approved.getAccountStatus());
        assertEquals(administrator.getId(), approved.getReviewedBy().getId());
        assertNotNull(approved.getReviewedAt());
        assertEquals("ROLE_HEALTHCARE_OFFICER", approved.getRequestedRole().getCode());
        assertEquals(List.of("ROLE_CENTER_MANAGER"), approved.getRoles().stream().map(Role::getCode).toList());
        assertEquals(1, authoritativeRoleRowCount(pending.getId()));
    }

    @Test
    @Timeout(30)
    @DisplayName("simultaneous approve and reject serialize so exactly one review wins on real MySQL")
    void simultaneousApprovalAndRejection_onlyOneWinsOnMySql() throws Exception {
        assertConnectedToRealMySql();
        UserAccount pending = savePending("mysql-concurrent@example.com");

        List<Outcome> outcomes = runConcurrently(
                () -> reviewService.approve(
                        pending.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), administrator.getId()),
                () -> reviewService.reject(
                        pending.getId(), new StaffRejectionRequest("Concurrent rejection"), administrator.getId())
        );

        assertEquals(1, outcomes.stream().filter(Outcome::success).count(), outcomes.toString());
        Outcome rejectedOutcome = outcomes.stream().filter(outcome -> !outcome.success()).findFirst().orElseThrow();
        assertInstanceOf(StaffRequestNotPendingException.class, rejectedOutcome.error(), outcomes.toString());

        UserAccount reviewed = userAccountRepository.findStaffRequestById(pending.getId()).orElseThrow();
        assertNotEquals(AccountStatus.PENDING_APPROVAL, reviewed.getAccountStatus());
        assertEquals(administrator.getId(), reviewed.getReviewedBy().getId());
        assertNotNull(reviewed.getReviewedAt());
        assertTrue(authoritativeRoleRowCount(pending.getId()) <= 1, "No duplicate authoritative role rows may exist");
        if (reviewed.getAccountStatus() == AccountStatus.ACTIVE) {
            assertEquals(1, authoritativeRoleRowCount(pending.getId()));
            assertNull(reviewed.getRejectionReason());
        } else {
            assertEquals(AccountStatus.REJECTED, reviewed.getAccountStatus());
            assertEquals(0, authoritativeRoleRowCount(pending.getId()));
            assertEquals("Concurrent rejection", reviewed.getRejectionReason());
        }
    }

    private UserAccount savePending(String email) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode("Password123!"), AccountStatus.PENDING_APPROVAL);
        account.setFullName("MySQL Pending Staff");
        account.setPhoneNumber("+94 77 100 2000");
        account.setRequestedRole(requestedRole);
        return userAccountRepository.saveAndFlush(account);
    }

    private int authoritativeRoleRowCount(Long userId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_roles WHERE user_id = ?", Integer.class, userId);
        return count == null ? 0 : count;
    }

    private List<Outcome> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Outcome> firstFuture = executor.submit(contender(first, ready, start));
            Future<Outcome> secondFuture = executor.submit(contender(second, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS), "Review contenders did not become ready");
            start.countDown();
            return List.of(firstFuture.get(10, TimeUnit.SECONDS), secondFuture.get(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Outcome> contender(Callable<?> action, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Review contender timed out waiting for start");
            }
            try {
                return new Outcome(true, action.call(), null);
            } catch (Exception exception) {
                return new Outcome(false, null, exception);
            }
        };
    }

    private record Outcome(boolean success, Object result, Throwable error) {}
}
