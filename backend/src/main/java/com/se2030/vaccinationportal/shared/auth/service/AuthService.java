package com.se2030.vaccinationportal.shared.auth.service;

import com.se2030.vaccinationportal.shared.auth.dto.LoginRequest;
import com.se2030.vaccinationportal.shared.auth.dto.LoginResponse;
import com.se2030.vaccinationportal.shared.auth.dto.UserProfileResponse;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
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
        UserProfileResponse userProfile = new UserProfileResponse(account.getId(), account.getEmail(), roleCodes);

        return new LoginResponse(token, userProfile);
    }

    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        Objects.requireNonNull(principal, "UserPrincipal must not be null");
        return new UserProfileResponse(
                principal.getUserId(),
                principal.getEmail(),
                principal.getRoleCodes()
        );
    }
}
