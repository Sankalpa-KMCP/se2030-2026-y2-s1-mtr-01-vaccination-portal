package com.se2030.vaccinationportal.shared.auth.query;

import com.se2030.vaccinationportal.shared.auth.api.AccountDisplayReader;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Converts account persistence into the single display value needed by callers. */
@Component
public class JpaAccountDisplayReader implements AccountDisplayReader {
    private final UserAccountRepository accounts;

    public JpaAccountDisplayReader(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<String> findFullName(Long accountId) {
        return accounts.findById(accountId).map(UserAccount::getFullName);
    }
}
