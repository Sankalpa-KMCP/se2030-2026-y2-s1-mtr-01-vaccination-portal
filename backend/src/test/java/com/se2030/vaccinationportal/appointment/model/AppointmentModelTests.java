package com.se2030.vaccinationportal.appointment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AppointmentModelTests {

    @Test
    @DisplayName("Appointment entity initializes fields correctly")
    void testAppointmentInitialization() {
        Instant now = Instant.now();
        Appointment appointment = new Appointment(100L, 200L, 300L, AppointmentStatus.CONFIRMED, now);

        assertEquals(100L, appointment.getCitizenId());
        assertEquals(200L, appointment.getCampaignId());
        assertEquals(300L, appointment.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertEquals(now, appointment.getBookedAt());
        assertNull(appointment.getCancelledAt());
        assertEquals(0L, appointment.getLockVersion());
    }

    @Test
    @DisplayName("Appointment default constructor and pre-persist lifecycle work as expected")
    void testPrePersistLifecycle() {
        Appointment appointment = new Appointment(101L, 201L, 301L);
        appointment.onCreate();

        assertEquals(101L, appointment.getCitizenId());
        assertEquals(201L, appointment.getCampaignId());
        assertEquals(301L, appointment.getSlotId());
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertNotNull(appointment.getBookedAt());
    }

    @Test
    @DisplayName("Package-private status mutation updates status")
    void testPackagePrivateStatusMutation() {
        Appointment appointment = new Appointment(102L, 202L, 302L);
        appointment.setStatus(AppointmentStatus.CHECKED_IN);
        assertEquals(AppointmentStatus.CHECKED_IN, appointment.getStatus());

        Instant cancelTime = Instant.now();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(cancelTime);
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals(cancelTime, appointment.getCancelledAt());
    }
}
