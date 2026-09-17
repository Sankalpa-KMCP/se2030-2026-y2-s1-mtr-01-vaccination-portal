package com.se2030.vaccinationportal.shared.auth.bootstrap;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
public class FirstAdministratorBootstrapService {

    private static final String ADMIN_ROLE_CODE = "ROLE_ADMIN";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^[0-9]{10}$");

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public FirstAdministratorBootstrapService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void bootstrap(String email, String password, String fullName, String phoneNumber) {
        Role administratorRole = roleRepository.findByCodeForUpdate(ADMIN_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException("Canonical ROLE_ADMIN is not configured"));

        if (userAccountRepository.existsByAuthoritativeRoleCode(ADMIN_ROLE_CODE)) {
            return;
        }

        String normalizedEmail = validateAndNormalizeEmail(email);
        validatePassword(password);
        String normalizedFullName = validateText(fullName, "full name", 255);
        String normalizedPhoneNumber = validatePhoneNumber(phoneNumber);

        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalStateException(
                    "Configured bootstrap administrator email already belongs to a non-administrator account");
        }

        UserAccount administrator = new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(password),
                AccountStatus.ACTIVE
        );
        administrator.setFullName(normalizedFullName);
        administrator.setPhoneNumber(normalizedPhoneNumber);
        administrator.addRole(administratorRole);
        userAccountRepository.saveAndFlush(administrator);
    }

    private String validateAndNormalizeEmail(String email) {
        String normalized = UserAccount.normalizeEmail(email);
        if (normalized.length() > 255 || !EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalStateException("Bootstrap administrator email is invalid");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() > 128) {
            throw new IllegalStateException("Bootstrap administrator password must be nonblank and at most 128 characters");
        }
    }

    private String validatePhoneNumber(String phoneNumber) {
        String normalized = validateText(phoneNumber, "phone number", 32);
        if (!PHONE_NUMBER_PATTERN.matcher(normalized).matches()) {
            throw new IllegalStateException("Bootstrap administrator phone number must contain exactly 10 digits");
        }
        return normalized;
    }

    private String validateText(String value, String label, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Bootstrap administrator " + label + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalStateException(
                    "Bootstrap administrator " + label + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }
}
