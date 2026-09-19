package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.UserProfileResponse;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingVerificationException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role centerManagerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();

        centerManagerRole = roleRepository.save(new Role("ROLE_CENTER_MANAGER", "Vaccination Center Manager"));
        adminRole = roleRepository.save(new Role("ROLE_ADMIN", "System Administrator"));
    }

    @Test
    @DisplayName("login with valid active account returns token and user profile")
    void login_validActiveAccount_success() {
        UserAccount account = new UserAccount("manager@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.addRole(centerManagerRole);
        userAccountRepository.save(account);

        LoginResponse response = authService.login(new LoginRequest("manager@portal.gov", "Password123!"));

        assertNotNull(response);
        assertNotNull(response.token());
        assertNotNull(response.user());
        assertEquals("manager@portal.gov", response.user().email());
        assertTrue(response.user().roles().contains("ROLE_CENTER_MANAGER"));
    }

    @Test
    @DisplayName("login normalizes email and performs case-insensitive lookup")
    void login_emailCaseInsensitive_success() {
        UserAccount account = new UserAccount("nurse@portal.gov", passwordEncoder.encode("SecretPass123!"), AccountStatus.ACTIVE);
        userAccountRepository.save(account);

        LoginResponse response = authService.login(new LoginRequest("  NURSE@PORTAL.GOV  ", "SecretPass123!"));

        assertNotNull(response);
        assertEquals("nurse@portal.gov", response.user().email());
    }

    @Test
    @DisplayName("login with incorrect password throws BadCredentialsException")
    void login_wrongPassword_throwsBadCredentials() {
        UserAccount account = new UserAccount("officer@portal.gov", passwordEncoder.encode("CorrectPassword1!"), AccountStatus.ACTIVE);
        userAccountRepository.save(account);

        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("officer@portal.gov", "WrongPassword!"))
        );
    }

    @Test
    @DisplayName("login with unknown email throws BadCredentialsException without leaking user existence")
    void login_unknownEmail_throwsBadCredentials() {
        assertThrows(BadCredentialsException.class, () ->
                authService.login(new LoginRequest("unknown@portal.gov", "AnyPassword123!"))
        );
    }

    @Test
    @DisplayName("login with PENDING_VERIFICATION account throws AccountPendingVerificationException")
    void login_pendingVerificationAccount_throwsAccountPendingVerificationException() {
        UserAccount account = new UserAccount("unverified@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.PENDING_VERIFICATION);
        userAccountRepository.save(account);

        assertThrows(AccountPendingVerificationException.class, () ->
                authService.login(new LoginRequest("unverified@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("login with SUSPENDED account throws DisabledException")
    void login_suspendedAccount_throwsDisabledException() {
        UserAccount account = new UserAccount("suspended@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.SUSPENDED);
        userAccountRepository.save(account);

        assertThrows(DisabledException.class, () ->
                authService.login(new LoginRequest("suspended@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("login with explicitly LOCKED account status throws LockedException")
    void login_lockedAccount_throwsLockedException() {
        UserAccount account = new UserAccount("locked@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.LOCKED);
        userAccountRepository.save(account);

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("locked@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("login with future locked_until timestamp throws LockedException")
    void login_temporaryLockNotExpired_throwsLockedException() {
        UserAccount account = new UserAccount("temp_locked@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.setLockedUntil(Instant.now().plus(30, ChronoUnit.MINUTES));
        userAccountRepository.save(account);

        assertThrows(LockedException.class, () ->
                authService.login(new LoginRequest("temp_locked@portal.gov", "Password123!"))
        );
    }

    @Test
    @DisplayName("getCurrentUserProfile loads the current account including nullable legacy profile fields")
    void getCurrentUserProfile_validPrincipal_returnsProfile() {
        UserAccount account = new UserAccount("admin@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.addRole(adminRole);
        account = userAccountRepository.saveAndFlush(account);
        UserPrincipal principal = UserPrincipal.fromClaims(account.getId(), account.getEmail(), List.of("ROLE_ADMIN"));
        UserProfileResponse profile = authService.getCurrentUserProfile(principal);

        assertNotNull(profile);
        assertEquals(account.getId(), profile.id());
        assertNull(profile.fullName());
        assertNull(profile.phoneNumber());
        assertEquals("admin@portal.gov", profile.email());
        assertEquals(AccountStatus.ACTIVE, profile.accountStatus());
        assertEquals(List.of("ROLE_ADMIN"), profile.roles());
    }
}
