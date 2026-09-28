package com.se2030.vaccinationportal.testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.shared.auth.dto.ForgotPasswordRequest;
import com.se2030.vaccinationportal.shared.auth.dto.ResendVerificationRequest;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.EmailVerificationToken;
import com.se2030.vaccinationportal.shared.auth.model.PasswordResetToken;
import com.se2030.vaccinationportal.shared.auth.model.Role;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.EmailVerificationTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.PasswordResetTokenRepository;
import com.se2030.vaccinationportal.shared.auth.repository.RoleRepository;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.shared.auth.service.AuthLinkIssuanceThrottle;
import com.se2030.vaccinationportal.shared.auth.service.AuthService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * MySQL-specific regression test for F2-ALIAS under actual MySQL collation (utf8mb4_unicode_ci).
 * Proves that multiple database-equivalent identifier spellings resolving to the same account row
 * share exactly one cooldown identity and cannot bypass cooldown to churn tokens.
 */
@Tag("mysql-testcontainers")
@ActiveProfiles("test")
public class AuthLinkIssuanceMySqlAliasIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {

    @DynamicPropertySource
    static void configureFlyway(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("app.security.auth.link-issuance-cooldown-seconds", () -> "60");
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private AuthLinkIssuanceThrottle authLinkIssuanceThrottle;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        emailVerificationTokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    @DisplayName("F2-ALIAS MySQL: database-equivalent spellings resolve to same account and share one cooldown")
    void mysqlCollationAliases_shareSingleAccountCooldown() throws Exception {
        assertConnectedToRealMySql();

        // 1. Insert one real account under canonical spelling
        UserAccount account = new UserAccount("resume.alias@portal.gov", passwordEncoder.encode("Password123!"), AccountStatus.ACTIVE);
        account.setFullName("Collation Test User");
        account = userAccountRepository.saveAndFlush(account);
        Long expectedAccountId = account.getId();

        // 2. Query using two database-equivalent spellings under MySQL utf8mb4_unicode_ci collation
        String spellingA = "resume.alias@portal.gov";
        String spellingB = "RESUME.ALIAS@PORTAL.GOV";

        Optional<UserAccount> lookupA = userAccountRepository.findByEmailIgnoreCase(spellingA);
        Optional<UserAccount> lookupB = userAccountRepository.findByEmailIgnoreCase(spellingB);

        // 3. Both lookups resolve the SAME account ID
        assertTrue(lookupA.isPresent());
        assertTrue(lookupB.isPresent());
        assertEquals(expectedAccountId, lookupA.get().getId());
        assertEquals(expectedAccountId, lookupB.get().getId());

        // 4. First auth-link request is admitted
        authService.forgotPassword(new ForgotPasswordRequest(spellingA));
        List<PasswordResetToken> tokensAfterFirst = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(expectedAccountId);
        assertEquals(1, tokensAfterFirst.size());
        String initialTokenHash = tokensAfterFirst.get(0).getTokenHash();

        // 5. Second alias request inside cooldown is denied admission
        authService.forgotPassword(new ForgotPasswordRequest(spellingB));

        // 6. Exactly one token/link issuance occurs
        List<PasswordResetToken> tokensAfterSecond = passwordResetTokenRepository.findAllByUser_IdAndUsedAtIsNull(expectedAccountId);
        assertEquals(1, tokensAfterSecond.size());
        assertEquals(initialTokenHash, tokensAfterSecond.get(0).getTokenHash(),
                "Outstanding token must not be rotated by alias request inside cooldown");
    }
}
