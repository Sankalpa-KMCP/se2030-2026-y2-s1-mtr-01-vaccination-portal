package com.se2030.vaccinationportal.campaign.core.service;

import com.se2030.vaccinationportal.campaign.core.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.core.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.session.planning.repository.VaccinationSessionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CampaignCurrentDayTests {
    private final LocalDate today = LocalDate.of(2026, 10, 9);
    private final Instant midnight = Instant.parse("2026-10-08T18:30:00Z");
    private VaccinationCampaignRepository repository;
    private VaccinationCampaignService service;

    @BeforeEach
    void setUp() {
        repository = mock(VaccinationCampaignRepository.class);
        service = new VaccinationCampaignService(repository, null, null,
                mock(VaccinationSessionRepository.class), null, null, null, null, null, null,
                Clock.fixed(Instant.parse("2026-10-08T20:00:00Z"), ZoneOffset.UTC));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private VaccinationCampaign create(LocalDate start, LocalDate end, Instant open, Instant close) {
        return service.createCampaign("DAY-CHECK", "Day check", null,
                (com.se2030.vaccinationportal.campaign.reference.model.VaccineType) null,
                (com.se2030.vaccinationportal.campaign.reference.model.TargetGroup) null,
                start, end, open, close);
    }

    @Test
    void todayAndEarlierTimesTodayAreAllowed() {
        assertNotNull(create(today, today.plusDays(1), midnight, midnight.plusSeconds(3600)));
        verify(repository).save(any());
    }

    @Test
    void rejectsYesterdayServiceStartBeforeSaving() {
        assertEquals("Service start date cannot be before today", assertThrows(IllegalArgumentException.class,
                () -> create(today.minusDays(1), today.plusDays(1), midnight, midnight.plusSeconds(3600))).getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void rejectsBookingOpenOneSecondBeforeLocalMidnight() {
        assertEquals("Citizen booking open date cannot be before today", assertThrows(IllegalArgumentException.class,
                () -> create(today, today.plusDays(1), midnight.minusSeconds(1), midnight.plusSeconds(3600))).getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void historicalDatesCanRemainUnchangedOnUpdate() {
        VaccinationCampaign existing = new VaccinationCampaign("OLD", "Old", null, null, null,
                today.minusDays(5), today.plusDays(1), midnight.minusSeconds(86400), midnight.plusSeconds(3600));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        assertNotNull(service.updateCampaign(1L, "Updated", null, existing.getServiceStartDate(),
                existing.getServiceEndDate(), existing.getBookingOpenAt(), existing.getBookingCloseAt()));
        assertEquals("Updated", existing.getName());
    }

    @Test
    void cannotChangeServiceStartToAnotherPastDate() {
        VaccinationCampaign existing = new VaccinationCampaign("OLD", "Old", null, null, null,
                today.minusDays(5), today.plusDays(1), midnight, midnight.plusSeconds(3600));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        assertEquals("Service start date cannot be before today", assertThrows(IllegalArgumentException.class,
                () -> service.updateCampaign(1L, "Updated", null, today.minusDays(1), existing.getServiceEndDate(),
                        existing.getBookingOpenAt(), existing.getBookingCloseAt())).getMessage());
        assertEquals("Old", existing.getName());
        verify(repository, never()).save(any());
    }

    @Test
    void cannotChangeServiceEndToAPastDate() {
        VaccinationCampaign existing = new VaccinationCampaign("OLD", "Old", null, null, null,
                today.minusDays(5), today.plusDays(1), midnight.minusSeconds(86400 * 3), midnight.minusSeconds(86400 * 2));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        assertEquals("Service end date cannot be before today", assertThrows(IllegalArgumentException.class,
                () -> service.updateCampaign(1L, "Updated", null, existing.getServiceStartDate(), today.minusDays(1),
                        existing.getBookingOpenAt(), existing.getBookingCloseAt())).getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void cannotChangeBookingCloseToAPastDate() {
        VaccinationCampaign existing = new VaccinationCampaign("OLD", "Old", null, null, null,
                today.minusDays(5), today.plusDays(1), midnight.minusSeconds(86400 * 3), midnight.plusSeconds(3600));
        when(repository.findByIdForUpdate(1L)).thenReturn(Optional.of(existing));
        assertEquals("Citizen booking close date cannot be before today", assertThrows(IllegalArgumentException.class,
                () -> service.updateCampaign(1L, "Updated", null, existing.getServiceStartDate(), existing.getServiceEndDate(),
                        existing.getBookingOpenAt(), midnight.minusSeconds(1))).getMessage());
        verify(repository, never()).save(any());
    }
}
