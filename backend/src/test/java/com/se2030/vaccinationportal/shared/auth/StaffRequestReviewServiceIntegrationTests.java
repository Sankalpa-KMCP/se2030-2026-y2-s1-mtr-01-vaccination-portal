package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.StaffApprovalRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRejectionRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRequestResponse;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidApprovalRoleException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRejectionReasonException;
import com.se2030.vaccinationportal.shared.auth.exception.StaffRequestNotPendingException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import com.se2030.vaccinationportal.shared.auth.service.StaffRequestReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StaffRequestReviewServiceIntegrationTests {

    private static final String RAW_PASSWORD = "ValidPassword123!";

    @Autowired private StaffRequestReviewService reviewService;
    @Autowired private AuthService authService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenService jwtTokenService;

    private UserAccount administrator;
    private Role requestedRole;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.saveAll(List.of(
                new Role("ROLE_ADMIN", "System Administrator"),
                new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"),
                new Role("ROLE_HEALTHCARE_OFFICER", "Healthcare Officer"),
                new Role("ROLE_CAMPAIGN_OFFICER", "Campaign Officer"),
                new Role("ROLE_INVENTORY_OFFICER", "Inventory Officer"),
                new Role("ROLE_VACCINATOR", "Vaccinator"),
                new Role("ROLE_CITIZEN", "Citizen"),
                new Role("ROLE_MANAGEMENT_USER", "Management User")
        ));
        requestedRole = roleRepository.findByCode("ROLE_HEALTHCARE_OFFICER").orElseThrow();
        administrator = new UserAccount(
                "reviewer@example.com", passwordEncoder.encode(RAW_PASSWORD), AccountStatus.ACTIVE);
        administrator.addRole(roleRepository.findByCode("ROLE_ADMIN").orElseThrow());
        administrator = userAccountRepository.saveAndFlush(administrator);
    }

    @Test
    @DisplayName("approval assigns ROLE_VACCINATOR and preserves it through principal and JWT authority handling")
    void approveVaccinator_assignsGlobalAuthorityAndEnablesLogin() {
        UserAccount pending = savePending("vaccinator@example.com");

        reviewService.approve(
                pending.getId(), new StaffApprovalRequest(" role_vaccinator "), administrator.getId());

        UserAccount approved = userAccountRepository.findById(pending.getId()).orElseThrow();
        assertEquals(List.of("ROLE_VACCINATOR"), approved.getRoles().stream().map(Role::getCode).toList());

        UserPrincipal principal = UserPrincipal.fromUserAccount(approved);
        assertEquals(List.of("ROLE_VACCINATOR"), principal.getRoleCodes());
        assertEquals("ROLE_VACCINATOR", principal.getAuthorities().iterator().next().getAuthority());

        LoginResponse login = authService.login(new LoginRequest(approved.getEmail(), RAW_PASSWORD));
        assertEquals(List.of("ROLE_VACCINATOR"), login.user().roles());
        assertEquals(List.of("ROLE_VACCINATOR"), jwtTokenService.extractRoles(login.token()));
    }

    @Test
    @DisplayName("approval atomically activates staff, assigns the chosen role, preserves request, and records reviewer")
    void approvePendingRequest_persistsAuthoritativeDecisionAndEnablesLogin() {
        UserAccount pending = savePending("approve@example.com");

        StaffRequestResponse response = reviewService.approve(
                pending.getId(), new StaffApprovalRequest(" role_center_manager "), administrator.getId());

        UserAccount approved = userAccountRepository.findById(pending.getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, approved.getAccountStatus());
        assertEquals(List.of("ROLE_CENTER_MANAGER"), approved.getRoles().stream().map(Role::getCode).toList());
        assertEquals("ROLE_HEALTHCARE_OFFICER", approved.getRequestedRole().getCode());
        assertEquals(administrator.getId(), approved.getReviewedBy().getId());
        assertNotNull(approved.getReviewedAt());
        assertNull(approved.getRejectionReason());
        assertEquals(administrator.getId(), response.reviewedByUserId());

        LoginResponse login = authService.login(new LoginRequest(approved.getEmail(), RAW_PASSWORD));
        assertNotNull(login.token());
        assertEquals(List.of("ROLE_CENTER_MANAGER"), login.user().roles());
    }

    @ParameterizedTest(name = "approval role {0} is rejected")
    @ValueSource(strings = {
            "ROLE_ADMIN", "ROLE_CITIZEN", "ROLE_MANAGEMENT_USER",
            "ROLE_SYSTEM_ADMINISTRATOR", "ROLE_RECEPTION_STAFF", "UNKNOWN_ROLE"
    })
    void approveForbiddenRole_rejectedWithoutMutation(String roleCode) {
        UserAccount pending = savePending("invalid-role-" + roleCode.toLowerCase() + "@example.com");

        assertThrows(InvalidApprovalRoleException.class, () -> reviewService.approve(
                pending.getId(), new StaffApprovalRequest(roleCode), administrator.getId()));

        UserAccount unchanged = userAccountRepository.findById(pending.getId()).orElseThrow();
        assertEquals(AccountStatus.PENDING_APPROVAL, unchanged.getAccountStatus());
        assertTrue(unchanged.getRoles().isEmpty());
        assertNull(unchanged.getReviewedAt());
    }

    @Test
    @DisplayName("approved or rejected staff requests cannot be approved again")
    void approveAlreadyReviewedRequest_returnsConflictDomainError() {
        UserAccount approved = savePending("double-approve@example.com");
        reviewService.approve(approved.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), administrator.getId());
        assertThrows(StaffRequestNotPendingException.class, () -> reviewService.approve(
                approved.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), administrator.getId()));

        UserAccount rejected = savePending("approve-rejected@example.com");
        reviewService.reject(rejected.getId(), new StaffRejectionRequest("Not eligible"), administrator.getId());
        assertThrows(StaffRequestNotPendingException.class, () -> reviewService.approve(
                rejected.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), administrator.getId()));
    }

    @Test
    @DisplayName("rejection records its reason and reviewer without assigning authority")
    void rejectPendingRequest_persistsAuditWithoutRole() {
        UserAccount pending = savePending("reject@example.com");

        StaffRequestResponse response = reviewService.reject(
                pending.getId(), new StaffRejectionRequest("  Verification failed  "), administrator.getId());

        UserAccount rejected = userAccountRepository.findById(pending.getId()).orElseThrow();
        assertEquals(AccountStatus.REJECTED, rejected.getAccountStatus());
        assertTrue(rejected.getRoles().isEmpty());
        assertEquals("ROLE_HEALTHCARE_OFFICER", rejected.getRequestedRole().getCode());
        assertEquals(administrator.getId(), rejected.getReviewedBy().getId());
        assertNotNull(rejected.getReviewedAt());
        assertEquals("Verification failed", rejected.getRejectionReason());
        assertEquals("Verification failed", response.rejectionReason());
    }

    @Test
    @DisplayName("service-layer rejection requires a nonblank reason even outside HTTP validation")
    void rejectBlankReason_isRejectedWithoutMutation() {
        UserAccount pending = savePending("blank-rejection@example.com");

        assertThrows(InvalidRejectionReasonException.class, () -> reviewService.reject(
                pending.getId(), new StaffRejectionRequest("   "), administrator.getId()));

        UserAccount unchanged = userAccountRepository.findById(pending.getId()).orElseThrow();
        assertEquals(AccountStatus.PENDING_APPROVAL, unchanged.getAccountStatus());
        assertNull(unchanged.getReviewedAt());
    }

    @Test
    @DisplayName("approved or rejected staff requests cannot be rejected again")
    void rejectAlreadyReviewedRequest_returnsConflictDomainError() {
        UserAccount rejected = savePending("double-reject@example.com");
        reviewService.reject(rejected.getId(), new StaffRejectionRequest("Rejected"), administrator.getId());
        assertThrows(StaffRequestNotPendingException.class, () -> reviewService.reject(
                rejected.getId(), new StaffRejectionRequest("Again"), administrator.getId()));

        UserAccount approved = savePending("reject-approved@example.com");
        reviewService.approve(approved.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), administrator.getId());
        assertThrows(StaffRequestNotPendingException.class, () -> reviewService.reject(
                approved.getId(), new StaffRejectionRequest("Too late"), administrator.getId()));
    }

    @Test
    @DisplayName("service refuses a reviewer that does not hold active authoritative ROLE_ADMIN")
    void reviewWithNonAdministratorReviewer_isDenied() {
        UserAccount citizen = new UserAccount(
                "citizen-reviewer@example.com", passwordEncoder.encode(RAW_PASSWORD), AccountStatus.ACTIVE);
        citizen.addRole(roleRepository.findByCode("ROLE_CITIZEN").orElseThrow());
        citizen = userAccountRepository.saveAndFlush(citizen);
        UserAccount pending = savePending("service-denied@example.com");

        Long citizenId = citizen.getId();
        assertThrows(AccessDeniedException.class, () -> reviewService.approve(
                pending.getId(), new StaffApprovalRequest("ROLE_CENTER_MANAGER"), citizenId));
    }

    private UserAccount savePending(String email) {
        UserAccount account = new UserAccount(email, passwordEncoder.encode(RAW_PASSWORD), AccountStatus.PENDING_APPROVAL);
        account.setFullName("Pending Staff");
        account.setPhoneNumber("+94 77 100 2000");
        account.setRequestedRole(requestedRole);
        return userAccountRepository.saveAndFlush(account);
    }
}
