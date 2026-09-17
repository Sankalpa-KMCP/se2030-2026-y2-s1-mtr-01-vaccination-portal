package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.dto.CitizenRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.MessageResponse;
import com.se2030.vaccinationportal.shared.auth.dto.RegistrationResponse;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResetPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.StaffRegistrationRequest;
import com.se2030.vaccinationportal.shared.auth.dto.UserProfileResponse;
import com.se2030.vaccinationportal.shared.auth.dto.VerifyEmailRequest;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingApprovalException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountPendingVerificationException;
import com.se2030.vaccinationportal.shared.auth.exception.AccountRejectedException;
import com.se2030.vaccinationportal.shared.auth.exception.DuplicateEmailException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidAuthActionTokenException;
import com.se2030.vaccinationportal.shared.auth.exception.InvalidRequestedRoleException;
import com.se2030.vaccinationportal.shared.auth.exception.TokenAlreadyUsedException;
import com.se2030.vaccinationportal.shared.auth.exception.TokenExpiredException;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.CitizenProfile;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.CitizenProfileRepository;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RefreshTokenSessionRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.AuthActionTokenService;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
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
            "ROLE_INVENTORY_OFFICER"
    );

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenSessionRepository refreshTokenSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final AuthActionTokenService authActionTokenService;
    private final AuthLinkDeliveryService authLinkDeliveryService;
    private final int verificationTokenExpirationHours;
    private final int resetTokenExpirationMinutes;

    public AuthService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            CitizenProfileRepository citizenProfileRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenSessionRepository refreshTokenSessionRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            AuthActionTokenService authActionTokenService,
            AuthLinkDeliveryService authLinkDeliveryService,
            @Value("${app.security.auth.verification-token-expiration-hours:24}") int verificationTokenExpirationHours,
            @Value("${app.security.auth.reset-token-expiration-minutes:30}") int resetTokenExpirationMinutes
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.citizenProfileRepository = citizenProfileRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenSessionRepository = refreshTokenSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.authActionTokenService = authActionTokenService;
        this.authLinkDeliveryService = authLinkDeliveryService;
        this.verificationTokenExpirationHours = verificationTokenExpirationHours;
        this.resetTokenExpirationMinutes = resetTokenExpirationMinutes;
    }

    public LoginResponse login(LoginRequest request) {
        Objects.requireNonNull(request, "LoginRequest must not be null");

        String normalizedEmail = UserAccount.normalizeEmail(request.email());

        UserAccount account = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (account.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new AccountPendingVerificationException();
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
                AccountStatus.PENDING_VERIFICATION
        );
        account.setFullName(request.fullName());
        account.setPhoneNumber(request.phoneNumber());
        account.addRole(citizenRole);

        UserAccount savedAccount = saveRegistration(account);
        citizenProfileRepository.saveAndFlush(new CitizenProfile(savedAccount));

        String rawToken = authActionTokenService.generateToken();
        String tokenHash = authActionTokenService.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(Duration.ofHours(verificationTokenExpirationHours));
        emailVerificationTokenRepository.saveAndFlush(new EmailVerificationToken(savedAccount, tokenHash, expiresAt));
        authLinkDeliveryService.sendEmailVerificationLink(savedAccount.getEmail(), rawToken);

        return toRegistrationResponse(savedAccount);
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        Objects.requireNonNull(request, "VerifyEmailRequest must not be null");
        String rawToken = request.token();
        String tokenHash = authActionTokenService.hashToken(rawToken);

        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidAuthActionTokenException::new);

        if (token.isConsumed()) {
            throw new TokenAlreadyUsedException();
        }

        Instant now = Instant.now();
        if (token.isExpired(now)) {
            throw new TokenExpiredException();
        }

        token.markUsed(now);
        emailVerificationTokenRepository.saveAndFlush(token);

        UserAccount user = token.getUser();
        user.setEmailVerifiedAt(now);
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            user.setAccountStatus(AccountStatus.ACTIVE);
        }
        userAccountRepository.saveAndFlush(user);

        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        Objects.requireNonNull(request, "ResendVerificationRequest must not be null");
        String normalizedEmail = UserAccount.normalizeEmail(request.email());

        Optional<UserAccount> userOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
                Instant now = Instant.now();
                List<EmailVerificationToken> existingTokens = emailVerificationTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
                for (EmailVerificationToken existingToken : existingTokens) {
                    existingToken.markUsed(now);
                }
                if (!existingTokens.isEmpty()) {
                    emailVerificationTokenRepository.saveAllAndFlush(existingTokens);
                }

                String rawToken = authActionTokenService.generateToken();
                String tokenHash = authActionTokenService.hashToken(rawToken);
                Instant expiresAt = now.plus(Duration.ofHours(verificationTokenExpirationHours));
                emailVerificationTokenRepository.saveAndFlush(new EmailVerificationToken(user, tokenHash, expiresAt));
                authLinkDeliveryService.sendEmailVerificationLink(user.getEmail(), rawToken);
            }
        }

        return new MessageResponse("If the account exists and is pending verification, a verification email has been sent.");
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        Objects.requireNonNull(request, "ForgotPasswordRequest must not be null");
        String normalizedEmail = UserAccount.normalizeEmail(request.email());

        Optional<UserAccount> userOpt = userAccountRepository.findByEmailIgnoreCase(normalizedEmail);
        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            if (user.getAccountStatus() != AccountStatus.REJECTED) {
                Instant now = Instant.now();
                List<PasswordResetToken> existingTokens = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(user.getId());
                for (PasswordResetToken existingToken : existingTokens) {
                    existingToken.markUsed(now);
                }
                if (!existingTokens.isEmpty()) {
                    passwordResetTokenRepository.saveAllAndFlush(existingTokens);
                }

                String rawToken = authActionTokenService.generateToken();
                String tokenHash = authActionTokenService.hashToken(rawToken);
                Instant expiresAt = now.plus(Duration.ofMinutes(resetTokenExpirationMinutes));
                passwordResetTokenRepository.saveAndFlush(new PasswordResetToken(user, tokenHash, expiresAt));
                authLinkDeliveryService.sendPasswordResetLink(user.getEmail(), rawToken);
            }
        }

        return new MessageResponse("If an eligible account is associated with this email address, password reset instructions have been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        Objects.requireNonNull(request, "ResetPasswordRequest must not be null");
        String rawToken = request.token();
        String tokenHash = authActionTokenService.hashToken(rawToken);

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidAuthActionTokenException::new);

        if (token.isConsumed()) {
            throw new TokenAlreadyUsedException();
        }

        Instant now = Instant.now();
        if (token.isExpired(now)) {
            throw new TokenExpiredException();
        }

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            user.setLockedUntil(null);
        }
        userAccountRepository.saveAndFlush(user);

        token.markUsed(now);
        passwordResetTokenRepository.saveAndFlush(token);

        refreshTokenSessionRepository.revokeAllActiveSessionsForUser(user.getId(), now);

        return new MessageResponse("Password has been reset successfully");
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
