package com.se2030.vaccinationportal.shared.auth.query;

import com.se2030.vaccinationportal.shared.auth.api.AccountDisplayReader;
import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountDisplayReaderIntegrationTests {
    @Autowired private AccountDisplayReader reader;
    @Autowired private UserAccountRepository accounts;

    @Test
    void productionReaderReturnsPersistedNameWithoutAccountDetails() {
        UserAccount citizen = new UserAccount("display-reader@example.com", "hash", AccountStatus.ACTIVE);
        citizen.setFullName("Display Reader Citizen");
        citizen = accounts.saveAndFlush(citizen);
        UserAccount other = new UserAccount("other-display-reader@example.com", "hash", AccountStatus.ACTIVE);
        other.setFullName("Other Citizen");
        accounts.saveAndFlush(other);

        assertThat(reader.findFullName(citizen.getId())).contains("Display Reader Citizen");
        assertThat(reader.findFullName(Long.MAX_VALUE)).isEmpty();
    }
}
