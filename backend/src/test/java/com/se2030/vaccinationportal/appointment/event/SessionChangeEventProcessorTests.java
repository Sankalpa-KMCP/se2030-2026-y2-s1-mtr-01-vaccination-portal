package com.se2030.vaccinationportal.appointment.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se2030.vaccinationportal.appointment.persistence.model.Appointment;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentChangeType;
import com.se2030.vaccinationportal.appointment.persistence.model.AppointmentStatus;
import com.se2030.vaccinationportal.appointment.persistence.model.ModuleEventConsumption;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistEntry;
import com.se2030.vaccinationportal.appointment.persistence.model.WaitlistStatus;
import com.se2030.vaccinationportal.appointment.persistence.repository.AppointmentRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.ModuleEventConsumptionRepository;
import com.se2030.vaccinationportal.appointment.persistence.repository.WaitlistRepository;
import com.se2030.vaccinationportal.campaign.api.capacity.SlotCapacityPort;
import com.se2030.vaccinationportal.campaign.api.session.SessionChangeNotice;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionChangeEventProcessorTests {
    private ModuleEventConsumptionRepository consumptions;
    private AppointmentRepository appointments;
    private WaitlistRepository waitlists;
    private SlotCapacityPort capacity;
    private Mf03HistoryRecorder history;
    private SessionChangeEventProcessor processor;
    private final AtomicReference<ModuleEventConsumption> storedConsumption = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        consumptions = mock(ModuleEventConsumptionRepository.class);
        appointments = mock(AppointmentRepository.class);
        waitlists = mock(WaitlistRepository.class);
        capacity = mock(SlotCapacityPort.class);
        history = mock(Mf03HistoryRecorder.class);
        processor = new SessionChangeEventProcessor(consumptions, appointments, waitlists, capacity, history);
        when(consumptions.findByConsumerCodeAndSourceEventId(anyString(), anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(storedConsumption.get()));
        when(consumptions.saveAndFlush(any())).thenAnswer(invocation -> {
            ModuleEventConsumption value = invocation.getArgument(0);
            storedConsumption.set(value);
            return value;
        });
        when(consumptions.save(any())).thenAnswer(invocation -> {
            ModuleEventConsumption value = invocation.getArgument(0);
            storedConsumption.set(value);
            return value;
        });
    }

    @Test
    void cancelledSession_closesActiveRecordsAndSecondDeliveryIsNoOp() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getSlotId()).thenReturn(301L);
        when(appointment.getCitizenId()).thenReturn(101L);
        when(appointment.getId()).thenReturn(201L);
        when(appointments.save(appointment)).thenReturn(appointment);
        when(appointments.findBySlotIdInAndStatusIn(List.of(301L),
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CHECKED_IN)))
                .thenReturn(List.of(appointment));
        when(capacity.releaseCapacity(301L)).thenReturn(true);

        WaitlistEntry waitlist = new WaitlistEntry(102L, 501L, 401L);
        when(waitlists.findBySessionIdAndStatusOrderByJoinedAtAscIdAsc(
                401L, WaitlistStatus.WAITING)).thenReturn(List.of(waitlist));
        when(waitlists.save(waitlist)).thenReturn(waitlist);

        SessionChangeNotice notice = notice("CANCELLED");
        processor.process(notice);
        processor.process(notice);

        verify(capacity, times(1)).releaseCapacity(301L);
        verify(appointment, times(1)).cancel(notice.createdAt());
        verify(history, times(1)).appointmentFromSessionChange(
                appointment, AppointmentChangeType.SESSION_CANCELLED, 701L,
                "Clinic unavailable", "44", notice.createdAt());
        verify(history, times(1)).waitlistFromSessionChange(
                waitlist, WaitlistStatus.WAITING, 701L,
                "Clinic unavailable", "44", notice.createdAt());
        org.junit.jupiter.api.Assertions.assertEquals(
                WaitlistStatus.SESSION_CANCELLED, waitlist.getStatus());
    }

    @Test
    void rescheduledSession_keepsBookingAndRecordsReviewNotification() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getSlotId()).thenReturn(301L);
        when(appointment.getCitizenId()).thenReturn(101L);
        when(appointment.getId()).thenReturn(201L);
        when(appointments.findBySlotIdInAndStatusIn(any(), any())).thenReturn(List.of(appointment));
        when(waitlists.findBySessionIdAndStatusOrderByJoinedAtAscIdAsc(anyLong(), any()))
                .thenReturn(List.of());

        processor.process(notice("RESCHEDULED"));

        verify(appointment, never()).cancel(any());
        verify(capacity, never()).releaseCapacity(anyLong());
        verify(history).appointmentFromSessionChange(
                appointment, AppointmentChangeType.SESSION_CHANGE_RECORDED, 701L,
                "Clinic unavailable", "44", Instant.parse("2026-10-08T09:00:00Z"));
        verify(history).notification(any(),
                org.mockito.ArgumentMatchers.eq(AppointmentChangeType.SESSION_CHANGE_RECORDED),
                org.mockito.ArgumentMatchers.contains("Please review"));
    }

    private SessionChangeNotice notice(String type) {
        return new SessionChangeNotice(701L, 401L, List.of(301L), type, 11L, 12L,
                Instant.parse("2026-10-10T09:00:00Z"),
                Instant.parse("2026-10-11T09:00:00Z"),
                "Clinic unavailable", "44", Instant.parse("2026-10-08T09:00:00Z"));
    }
}
