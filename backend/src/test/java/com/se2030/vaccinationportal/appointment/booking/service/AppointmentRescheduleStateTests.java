package com.se2030.vaccinationportal.appointment.booking.service;

import com.se2030.vaccinationportal.appointment.exception.InvalidAppointmentStateException;
import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.CitizenCampaignBookingGuard;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.CitizenCampaignBookingGuardRepository;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotBookability;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotReservationResult;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppointmentRescheduleStateTests {
    private final AppointmentRepository appointments = mock(AppointmentRepository.class);
    private final CitizenCampaignBookingGuardRepository guards = mock(CitizenCampaignBookingGuardRepository.class);
    private final SlotCapacityPort slots = mock(SlotCapacityPort.class);
    private final Instant now = Instant.parse("2030-01-01T08:00:00Z");
    private Appointment appointment;
    private AppointmentRescheduleService service;

    @BeforeEach
    void setUp() {
        appointment = new Appointment(1L, 2L, 3L, AppointmentStatus.CHECKED_IN, now);
        when(appointments.findById(9L)).thenReturn(Optional.of(appointment));
        when(slots.getSlotStartTime(3L)).thenReturn(now.plus(3, ChronoUnit.DAYS));
        service = new AppointmentRescheduleService(appointments, guards, slots, null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void checkedInAppointmentCannotMoveEvenWithStaffOverride(boolean staffOverride) {
        var error = assertThrows(InvalidAppointmentStateException.class,
                () -> service.rescheduleAppointment(9L, staffOverride ? null : 1L, 4L,
                        staffOverride, staffOverride ? "Citizen requested a different slot" : null, now));
        assertEquals("Reverse check-in before rescheduling this appointment", error.getMessage());
        assertEquals(3L, appointment.getSlotId());
        assertEquals(AppointmentStatus.CHECKED_IN, appointment.getStatus());
        verifyNoInteractions(guards);
        verify(slots, never()).reserveCapacityForNewBooking(anyLong(), any());
        verify(slots, never()).releaseCapacity(anyLong());
        verify(appointments, never()).save(any());
    }

    @Test
    void confirmedAppointmentCanMoveAfterItsCheckInWasReversed() {
        appointment.reverseCheckIn();
        when(guards.findByCitizenIdAndCampaignIdForUpdate(1L, 2L))
                .thenReturn(Optional.of(new CitizenCampaignBookingGuard(1L, 2L)));
        when(slots.isSlotInCampaign(4L, 2L)).thenReturn(true);
        when(slots.evaluateBookabilityForNewBooking(4L, now)).thenReturn(SlotBookability.BOOKABLE);
        when(slots.reserveCapacityForNewBooking(4L, now)).thenReturn(SlotReservationResult.SUCCESS);
        when(slots.releaseCapacity(3L)).thenReturn(true);
        when(appointments.save(appointment)).thenReturn(appointment);

        Appointment result = service.rescheduleAppointment(9L, 1L, 4L, now);
        assertEquals(4L, result.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, result.getStatus());
        var capacityOrder = inOrder(slots);
        capacityOrder.verify(slots).reserveCapacityForNewBooking(4L, now);
        capacityOrder.verify(slots).releaseCapacity(3L);
    }
}
