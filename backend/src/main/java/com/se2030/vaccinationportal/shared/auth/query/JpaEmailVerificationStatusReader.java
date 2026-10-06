package com.se2030.vaccinationportal.shared.auth.query;

import com.se2030.vaccinationportal.shared.auth.api.EmailVerificationStatusReader;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.springframework.stereotype.Component;

/**
 * Answers email verification state from the account's verification timestamp.
 * Accounts that do not exist are reported unverified rather than leaking existence.
 */
@Component
public class JpaEmailVerificationStatusReader implements EmailVerificationStatusReader {

    private final UserAccountRepository accounts;

    public JpaEmailVerificationStatusReader(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public boolean isEmailVerified(Long userId) {
        if (userId == null) {
            return false;
        }
        return accounts.findById(userId)
                .map(account -> account.getEmailVerifiedAt() != null)
                .orElse(false);
    }
}
