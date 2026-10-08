package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import com.se2030.vaccinationportal.campaign.core.repository.CampaignLifecycleEventRepository;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignAuditObserverTests {
    @Test
    void savesTheSuppliedEventExactlyOnce() {
        var repository = mock(CampaignLifecycleEventRepository.class);
        var event = mock(CampaignLifecycleEvent.class);
        new CampaignAuditObserver(repository).update(event);
        verify(repository).save(event);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void persistenceFailuresAreNotSwallowed() {
        var repository = mock(CampaignLifecycleEventRepository.class);
        var event = mock(CampaignLifecycleEvent.class);
        var failure = new IllegalStateException("Audit save failed");
        when(repository.save(event)).thenThrow(failure);
        var observer = new CampaignAuditObserver(repository);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> observer.update(event)));
    }
}
