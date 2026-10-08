package com.se2030.vaccinationportal.campaign.session.planning.policy;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import com.se2030.vaccinationportal.campaign.reference.model.VaccineType;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DirectBookingBookabilityStrategyTests {
    private final Instant now = Instant.parse("2030-01-01T12:00:00Z");
    private final DirectBookingBookabilityStrategy strategy = new DirectBookingBookabilityStrategy();

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31})
    void preservesRejectionsAndPrecedenceForEveryCombination(int failures) {
        boolean campaignClosed = (failures & 1) != 0;
        boolean suspended = (failures & 2) != 0;
        boolean cutoffPassed = (failures & 4) != 0;
        boolean slotPast = (failures & 8) != 0;
        boolean full = (failures & 16) != 0;
        TimeSlot slot = slot(!campaignClosed, suspended,
                now.plusSeconds(cutoffPassed ? -1 : 1), now.plusSeconds(slotPast ? -1 : 1), !full);
        SlotBookability expected = campaignClosed ? SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN
                : suspended ? SlotBookability.SESSION_BOOKING_SUSPENDED
                : cutoffPassed ? SlotBookability.SESSION_BOOKING_CLOSED
                : slotPast ? SlotBookability.SLOT_IN_PAST
                : full ? SlotBookability.NO_CAPACITY : SlotBookability.BOOKABLE;

        assertEquals(expected, strategy.evaluate(slot, now));
        verify(slot, never()).reserve();
    }

    @ParameterizedTest
    @CsvSource({"-1,1,SESSION_BOOKING_CLOSED", "0,1,SESSION_BOOKING_CLOSED",
            "1,-1,SLOT_IN_PAST", "1,0,SLOT_IN_PAST", "1,1,BOOKABLE"})
    void preservesNanosecondCutoffAndSlotBoundaries(long cutoffOffset, long startOffset,
                                                   SlotBookability expected) {
        assertEquals(expected, strategy.evaluate(slot(true, false, now.plusNanos(cutoffOffset),
                now.plusNanos(startOffset), true), now));
    }

    @Test
    void doesNotReplaceExistingNullTimeBehaviorWithAFallback() {
        VaccinationCampaign campaign = new VaccinationCampaign(
                "NULL-TIME", "Null-time booking test", "Valid campaign fixture",
                new VaccineType("TEST-VACCINE", "Test vaccine"),
                new TargetGroup("TEST-GROUP", "Test target group"),
                LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 3),
                now.minusSeconds(60), now.plusSeconds(60));
        campaign.publish();
        VaccinationCenter center = new VaccinationCenter(
                "TEST-CENTER", "Test center", "Test address", "0112345678",
                CenterStatus.ACTIVE, 10, null, "UTC");
        VaccinationSession session = new VaccinationSession(
                campaign, center, now.plusSeconds(120), now.plusSeconds(240),
                now.plusSeconds(60), "UTC");
        session.schedule();
        TimeSlot slot = new TimeSlot(session, now.plusSeconds(120), now.plusSeconds(180), 1);

        assertEquals(SlotBookability.BOOKABLE, strategy.evaluate(slot, now));
        assertEquals(SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN, strategy.evaluate(slot, null));
        assertEquals(0, slot.getReservedCount());
    }

    private TimeSlot slot(boolean campaignOpen, boolean suspended, Instant cutoff, Instant start,
                          boolean available) {
        VaccinationCampaign campaign = mock(VaccinationCampaign.class);
        VaccinationSession session = mock(VaccinationSession.class);
        TimeSlot slot = mock(TimeSlot.class);
        when(slot.getSession()).thenReturn(session);
        when(session.getCampaign()).thenReturn(campaign);
        when(campaign.isBookingWindowOpenAt(any())).thenReturn(campaignOpen);
        when(session.isBookingSuspended()).thenReturn(suspended);
        when(session.getBookingCloseAt()).thenReturn(cutoff);
        when(slot.getStartTime()).thenReturn(start);
        when(slot.hasAvailableCapacity()).thenReturn(available);
        return slot;
    }
}
