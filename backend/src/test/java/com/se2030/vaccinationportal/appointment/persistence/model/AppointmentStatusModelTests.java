package com.se2030.vaccinationportal.appointment.persistence.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppointmentStatusModelTests {

    @Test
    @DisplayName("AppointmentStatus enum contains exactly the five canonical statuses")
    void testEnumValues() {
        AppointmentStatus[] statuses = AppointmentStatus.values();
        assertEquals(5, statuses.length, "AppointmentStatus must contain exactly 5 statuses per canonical section 7.4");

        assertEquals(AppointmentStatus.CONFIRMED, AppointmentStatus.valueOf("CONFIRMED"));
        assertEquals(AppointmentStatus.CHECKED_IN, AppointmentStatus.valueOf("CHECKED_IN"));
        assertEquals(AppointmentStatus.COMPLETED, AppointmentStatus.valueOf("COMPLETED"));
        assertEquals(AppointmentStatus.CANCELLED, AppointmentStatus.valueOf("CANCELLED"));
        assertEquals(AppointmentStatus.NO_SHOW, AppointmentStatus.valueOf("NO_SHOW"));
    }
}
