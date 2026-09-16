package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.RegistrationResponse;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.UserProfileResponse;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingApprovalException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountRejectedException;
import com.se2030.vaccinationportal.shared.auth.exception.DuplicateEmailException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRequestedRoleException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.CitizenProfile;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String CITIZEN_ROLE_CODE = "ROLE_CITIZEN";
    private static final Set<String> PUBLIC_STAFF_ROLE_CODES = Set.of(
            "ROLE_HEALTHCARE_OFFICER",
            "ROLE_CENTER_MANAGER",
            "ROLE_CAMPAIGN_OFFICER",
            "ROLE_INVENTORY_OFFICER",
            "ROLE_VACCINATOR"
    );

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            CitizenProfileRepository citizenProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        Objects.requireNonNull(request, "LoginRequest must not be null");

        String normalizedEmail = UserAccount.normalizeEmail(request.email());

        UserAccount account = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (account.getAccountStatus() == AccountStatus.PENDING_APPROVAL) {
            throw new AccountPendingApprovalException();
        }

        if (account.getAccountStatus() == AccountStatus.REJECTED) {
            throw new AccountRejectedException();
        }

        if (account.getAccountStatus() == AccountStatus.LOCKED
                || (account.getLockedUntil() != null && Instant.now().isBefore(account.getLockedUntil()))) {
            throw new LockedException("Account is temporarily locked");
        }

        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new DisabledException("Account is not active for login");
        }

        List<String> roleCodes = account.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .collect(Collectors.toList());

        String token = jwtTokenService.generateToken(account.getId(), account.getEmail(), roleCodes);
        UserProfileResponse userProfile = toUserProfile(account, roleCodes);

        return new LoginResponse(token, userProfile);
    }

    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        Objects.requireNonNull(principal, "UserPrincipal must not be null");
        UserAccount account = userAccountRepository.findById(principal.getUserId())
                .orElseThrow(() -> new BadCredentialsException("Authenticated account no longer exists"));
        List<String> roleCodes = account.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();
        return toUserProfile(account, roleCodes);
    }

    @Transactional
    public RegistrationResponse registerCitizen(CitizenRegistrationRequest request) {
        Objects.requireNonNull(request, "CitizenRegistrationRequest must not be null");
        String normalizedEmail = UserAccount.normalizeEmail(request.email());
        assertEmailAvailable(normalizedEmail);

        Role citizenRole = roleRepository.findByCode(CITIZEN_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException("Canonical citizen role is not configured"));

        UserAccount account = new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                AccountStatus.ACTIVE
        );
        account.setFullName(request.fullName());
        account.setPhoneNumber(request.phoneNumber());
        account.addRole(citizenRole);

        UserAccount savedAccount = saveRegistration(account);
        citizenProfileRepository.saveAndFlush(new CitizenProfile(savedAccount));
        return toRegistrationResponse(savedAccount);
    }

    @Transactional
    public RegistrationResponse registerStaffRequest(StaffRegistrationRequest request) {
        Objects.requireNonNull(request, "StaffRegistrationRequest must not be null");
        String normalizedEmail = UserAccount.normalizeEmail(request.email());
        assertEmailAvailable(normalizedEmail);

        String requestedRoleCode = normalizeRequestedRoleCode(request.requestedRoleCode());
        if (!PUBLIC_STAFF_ROLE_CODES.contains(requestedRoleCode)) {
            throw new InvalidRequestedRoleException();
        }
        Role requestedRole = roleRepository.findByCode(requestedRoleCode)
                .orElseThrow(InvalidRequestedRoleException::new);

        UserAccount account = new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                AccountStatus.PENDING_APPROVAL
        );
        account.setFullName(request.fullName());
        account.setPhoneNumber(request.phoneNumber());
        account.setRequestedRole(requestedRole);

        return toRegistrationResponse(saveRegistration(account));
    }

    public static Set<String> getPublicStaffRoleCodes() {
        return PUBLIC_STAFF_ROLE_CODES;
    }

    private void assertEmailAvailable(String normalizedEmail) {
        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }
    }

    private UserAccount saveRegistration(UserAccount account) {
        try {
            return userAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailConstraintViolation(exception)) {
                throw new DuplicateEmailException();
            }
            throw exception;
        }
    }

    private boolean isEmailConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("uk_user_accounts_email")
                        || (normalized.contains("user_accounts") && normalized.contains("email"))) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String normalizeRequestedRoleCode(String roleCode) {
        return roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
    }

    private UserProfileResponse toUserProfile(UserAccount account, List<String> roleCodes) {
        return new UserProfileResponse(
                account.getId(),
                account.getFullName(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getAccountStatus(),
                roleCodes
        );
    }

    private RegistrationResponse toRegistrationResponse(UserAccount account) {
        List<String> roleCodes = account.getRoles().stream()
                .map(Role::getCode)
                .sorted()
                .toList();
        return new RegistrationResponse(
                account.getId(),
                account.getFullName(),
                account.getEmail(),
                account.getPhoneNumber(),
                account.getAccountStatus(),
                roleCodes,
                account.getRequestedRole() != null ? account.getRequestedRole().getCode() : null,
                account.getCreatedAt()
        );
    }
}
