package com.se2030.vaccinationportal.campaign.session.planning.policy;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WaitlistPromotionBookabilityStrategyTests {
    private final WaitlistPromotionBookabilityStrategy strategy = new WaitlistPromotionBookabilityStrategy();

    @ParameterizedTest
    @EnumSource(CampaignStatus.class)
    void permitsOnlyPublishedAndActiveCampaigns(CampaignStatus status) {
        TimeSlot slot = slot(status);
        SlotBookability expected = (status == CampaignStatus.PUBLISHED || status == CampaignStatus.ACTIVE)
                ? SlotBookability.BOOKABLE : SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN;
        assertEquals(expected, strategy.evaluate(slot, Instant.parse("2030-01-01T12:00:00Z")));
        verify(slot, never()).reserve();
    }

    @Test
    void acceptsSuspendedSessionAfterCutoffWithPastFullSlotWithoutReapplyingDirectRules() {
        Instant now = Instant.parse("2030-01-01T12:00:00Z");
        TimeSlot slot = slot(CampaignStatus.ACTIVE);
        VaccinationSession session = slot.getSession();
        when(session.isBookingSuspended()).thenReturn(true);
        when(session.getBookingCloseAt()).thenReturn(now.minusSeconds(60));
        when(slot.getStartTime()).thenReturn(now.minusSeconds(30));
        when(slot.hasAvailableCapacity()).thenReturn(false);

        assertEquals(SlotBookability.BOOKABLE, strategy.evaluate(slot, now));
        assertEquals(SlotBookability.BOOKABLE, strategy.evaluate(slot, null));
        verify(session, never()).isBookingSuspended();
        verify(session, never()).getBookingCloseAt();
        verify(slot, never()).getStartTime();
        verify(slot, never()).hasAvailableCapacity();
        verify(slot, never()).reserve();
    }

    private TimeSlot slot(CampaignStatus status) {
        VaccinationCampaign campaign = mock(VaccinationCampaign.class);
        VaccinationSession session = mock(VaccinationSession.class);
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getSession()).thenReturn(session);
        when(session.getCampaign()).thenReturn(campaign);
        when(campaign.getStatus()).thenReturn(status);
        return slot;
    }
}
