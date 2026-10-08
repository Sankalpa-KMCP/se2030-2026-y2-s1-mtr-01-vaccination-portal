package com.se2030.vaccinationportal.campaign.session.planning.service;

import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.core.model.CampaignStatus;
import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.session.planning.model.SessionLifecycleStatus;
import com.se2030.vaccinationportal.campaign.session.planning.model.TimeSlot;
import com.se2030.vaccinationportal.campaign.session.planning.model.VaccinationSession;
import com.se2030.vaccinationportal.campaign.session.planning.policy.DirectBookingBookabilityStrategy;
import com.se2030.vaccinationportal.campaign.session.planning.policy.SlotBookabilityStrategy;
import com.se2030.vaccinationportal.campaign.session.planning.policy.WaitlistPromotionBookabilityStrategy;
import com.se2030.vaccinationportal.campaign.session.planning.repository.TimeSlotRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TimeSlotCapacityServiceStrategyTests {
    private final TimeSlotRepository slots = mock(TimeSlotRepository.class);
    private final VaccinationSessionRepository sessions = mock(VaccinationSessionRepository.class);
    private final VaccinationCampaignRepository campaigns = mock(VaccinationCampaignRepository.class);
    private final SlotBookabilityStrategy direct = mock(SlotBookabilityStrategy.class);
    private final SlotBookabilityStrategy promotion = mock(SlotBookabilityStrategy.class);
    private final TimeSlotCapacityService service = new TimeSlotCapacityService(slots, sessions, campaigns,
            direct, promotion);
    private final Instant now = Instant.parse("2030-01-01T12:00:00Z");

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void delegatesOnceToWorkflowStrategyWithoutPersistenceOrCapacityMutation(boolean newBooking) {
        TimeSlot slot = resolvedSlot();
        when(slots.findById(1L)).thenReturn(Optional.of(slot));
        SlotBookabilityStrategy selected = newBooking ? direct : promotion;
        SlotBookabilityStrategy other = newBooking ? promotion : direct;
        when(selected.evaluate(slot, now)).thenReturn(SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN);

        assertEquals(SlotBookability.CAMPAIGN_BOOKING_NOT_OPEN, evaluate(newBooking, 1L));
        verify(selected, times(1)).evaluate(slot, now);
        verifyNoMoreInteractions(selected);
        verifyNoInteractions(other, sessions, campaigns);
        verify(slots).findById(1L);
        verifyNoMoreInteractions(slots);
        verify(slot, never()).reserve();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nullId", "missingSlot", "missingSession", "notScheduled", "missingCampaign", "missingCampaignId"})
    void sharedFailuresPreventBothStrategiesAndPreserveResolutionPrecedence(String failure) {
        Long id = "nullId".equals(failure) ? null : 1L;
        TimeSlot slot = resolvedSlot();
        VaccinationSession session = slot.getSession();
        if ("missingSession".equals(failure)) when(slot.getSession()).thenReturn(null);
        if ("notScheduled".equals(failure)) {
            when(session.getLifecycleStatus()).thenReturn(SessionLifecycleStatus.DRAFT);
            when(session.getCampaign()).thenReturn(null); // lifecycle failure must precede missing campaign
        }
        if ("missingCampaign".equals(failure)) when(session.getCampaign()).thenReturn(null);
        if ("missingCampaignId".equals(failure)) when(session.getCampaign().getId()).thenReturn(null);
        when(slots.findById(1L)).thenReturn("missingSlot".equals(failure) ? Optional.empty() : Optional.of(slot));
        SlotBookability expected = "notScheduled".equals(failure)
                ? SlotBookability.SESSION_NOT_SCHEDULED : SlotBookability.SLOT_NOT_FOUND;

        assertEquals(expected, evaluate(true, id));
        assertEquals(expected, evaluate(false, id));
        verifyNoInteractions(direct, promotion, sessions, campaigns);
        if (id == null) verifyNoInteractions(slots);
        else {
            verify(slots, times(2)).findById(1L);
            verifyNoMoreInteractions(slots);
        }
        verify(slot, never()).reserve();
    }

    @Test
    void springQualifiersWireBothRealStrategiesToTheirWorkflowMethods() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(EntityManagerFactory.class, () -> mock(EntityManagerFactory.class));
            context.registerBean(TimeSlotRepository.class, () -> slots);
            context.registerBean(VaccinationSessionRepository.class, () -> sessions);
            context.registerBean(VaccinationCampaignRepository.class, () -> campaigns);
            context.register(DirectBookingBookabilityStrategy.class, WaitlistPromotionBookabilityStrategy.class,
                    TimeSlotCapacityService.class);
            context.refresh();
            TimeSlotCapacityService wired = context.getBean(TimeSlotCapacityService.class);
            TimeSlot slot = resolvedSlot();
            VaccinationSession session = slot.getSession();
            when(session.getCampaign().getStatus()).thenReturn(CampaignStatus.ACTIVE);
            when(session.getCampaign().isBookingWindowOpenAt(now)).thenReturn(true);
            when(session.getBookingCloseAt()).thenReturn(now.plusSeconds(60));
            when(slot.getStartTime()).thenReturn(now.plusSeconds(120));
            when(slot.hasAvailableCapacity()).thenReturn(false);
            when(slots.findById(1L)).thenReturn(Optional.of(slot));

            assertEquals(2, context.getBeansOfType(SlotBookabilityStrategy.class).size());
            assertEquals(SlotBookability.NO_CAPACITY, wired.evaluateBookabilityForNewBooking(1L, now));
            assertEquals(SlotBookability.BOOKABLE, wired.evaluateBookabilityForWaitlistPromotion(1L, now));
            verify(slot, never()).reserve();
        }
    }

    private SlotBookability evaluate(boolean newBooking, Long id) {
        return newBooking ? service.evaluateBookabilityForNewBooking(id, now)
                : service.evaluateBookabilityForWaitlistPromotion(id, now);
    }

    private TimeSlot resolvedSlot() {
        TimeSlot slot = mock(TimeSlot.class);
        VaccinationSession session = mock(VaccinationSession.class);
        VaccinationCampaign campaign = mock(VaccinationCampaign.class);
        when(slot.getSession()).thenReturn(session);
        when(session.getLifecycleStatus()).thenReturn(SessionLifecycleStatus.SCHEDULED);
        when(session.getCampaign()).thenReturn(campaign);
        when(campaign.getId()).thenReturn(1L);
        return slot;
    }
}
