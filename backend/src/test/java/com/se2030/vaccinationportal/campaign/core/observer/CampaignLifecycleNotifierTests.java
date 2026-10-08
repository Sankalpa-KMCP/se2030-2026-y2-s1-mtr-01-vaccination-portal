package com.se2030.vaccinationportal.campaign.core.observer;

import com.se2030.vaccinationportal.campaign.core.model.CampaignLifecycleEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignLifecycleNotifierTests {
    @Test
    void notifiesTwoObserversOnceInRegistrationOrder() {
        var first = mock(CampaignLifecycleObserver.class);
        var second = mock(CampaignLifecycleObserver.class);
        var event = mock(CampaignLifecycleEvent.class);
        var subject = new CampaignLifecycleNotifier(List.of(first, second));
        subject.addObserver(first);
        subject.notifyObservers(event);
        var order = inOrder(first, second);
        order.verify(first).update(event);
        order.verify(second).update(event);
        verifyNoMoreInteractions(first, second);
    }

    @Test
    void removedObserverReceivesNoUpdates() {
        var observer = mock(CampaignLifecycleObserver.class);
        var subject = new CampaignLifecycleNotifier(List.of(observer));
        subject.removeObserver(observer);
        subject.removeObserver(observer);
        subject.notifyObservers(mock(CampaignLifecycleEvent.class));
        verifyNoInteractions(observer);
    }

    @Test
    void rejectsNullRegistrationsAndNotifications() {
        var subject = new CampaignLifecycleNotifier(List.of());
        assertThrows(NullPointerException.class, () -> subject.addObserver(null));
        assertThrows(NullPointerException.class, () -> subject.removeObserver(null));
        assertThrows(NullPointerException.class, () -> subject.notifyObservers(null));
        assertThrows(NullPointerException.class, () -> new CampaignLifecycleNotifier(null));
    }

    @Test
    void registrationChangesOnlyAffectTheNextNotification() {
        var first = mock(CampaignLifecycleObserver.class);
        var second = mock(CampaignLifecycleObserver.class);
        var third = mock(CampaignLifecycleObserver.class);
        var event = mock(CampaignLifecycleEvent.class);
        var subject = new CampaignLifecycleNotifier(List.of(first, second));
        doAnswer(call -> { subject.removeObserver(second); subject.addObserver(third); return null; })
                .when(first).update(event);
        subject.notifyObservers(event);
        verify(second).update(event);
        verifyNoInteractions(third);
        subject.notifyObservers(event);
        verify(second, times(1)).update(event);
        verify(third).update(event);
    }

    @Test
    void observerFailurePropagatesAndStopsNotification() {
        var first = mock(CampaignLifecycleObserver.class);
        var second = mock(CampaignLifecycleObserver.class);
        var event = mock(CampaignLifecycleEvent.class);
        var failure = new IllegalStateException("Observer failed");
        doThrow(failure).when(first).update(event);
        var subject = new CampaignLifecycleNotifier(List.of(first, second));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> subject.notifyObservers(event)));
        verifyNoInteractions(second);
    }
}
