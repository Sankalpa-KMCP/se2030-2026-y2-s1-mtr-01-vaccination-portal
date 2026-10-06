package com.se2030.vaccinationportal.shared.auth.query;

import com.se2030.vaccinationportal.shared.auth.model.AccountStatus;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaAccountDisplayReaderTests {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final JpaAccountDisplayReader reader = new JpaAccountDisplayReader(accounts);

    @Test
    void returnsOnlyTheRequestedAccountsFullName() {
        UserAccount account = new UserAccount("private@example.com", "private hash", AccountStatus.ACTIVE);
        account.setFullName("Citizen Display Name");
        when(accounts.findById(10L)).thenReturn(Optional.of(account));

        assertThat(reader.findFullName(10L)).contains("Citizen Display Name");
        verify(accounts).findById(10L);
    }

    @Test
    void absentAccountProducesTheExistingEmptyDisplayValue() {
        when(accounts.findById(10L)).thenReturn(Optional.empty());
        assertThat(reader.findFullName(10L)).isEmpty();
    }

    @Test
    void nullNameProducesTheExistingEmptyDisplayValue() {
        UserAccount account = new UserAccount("unnamed@example.com", "hash", AccountStatus.ACTIVE);
        when(accounts.findById(10L)).thenReturn(Optional.of(account));
        assertThat(reader.findFullName(10L)).isEmpty();
    }

    @Test
    void invalidIdentityPreservesRepositoryFailure() {
        IllegalArgumentException failure = new IllegalArgumentException("The given id must not be null");
        when(accounts.findById(null)).thenThrow(failure);
        assertThatThrownBy(() -> reader.findFullName(null)).isSameAs(failure);
    }
}
