package com.se2030.vaccinationportal.appointment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaitlistStatusModelTests {

    @Test
    @DisplayName("WaitlistStatus enum contains exactly the seven canonical statuses")
    void testEnumValues() {
        WaitlistStatus[] statuses = WaitlistStatus.values();
        assertEquals(7, statuses.length, "WaitlistStatus must contain exactly 7 statuses per canonical section 7.4");

        assertEquals(WaitlistStatus.WAITING, WaitlistStatus.valueOf("WAITING"));
        assertEquals(WaitlistStatus.PROMOTED, WaitlistStatus.valueOf("PROMOTED"));
        assertEquals(WaitlistStatus.SKIPPED_INELIGIBLE, WaitlistStatus.valueOf("SKIPPED_INELIGIBLE"));
        assertEquals(WaitlistStatus.SKIPPED_DUPLICATE_APPOINTMENT, WaitlistStatus.valueOf("SKIPPED_DUPLICATE_APPOINTMENT"));
        assertEquals(WaitlistStatus.CANCELLED, WaitlistStatus.valueOf("CANCELLED"));
        assertEquals(WaitlistStatus.SESSION_CANCELLED, WaitlistStatus.valueOf("SESSION_CANCELLED"));
        assertEquals(WaitlistStatus.EXPIRED, WaitlistStatus.valueOf("EXPIRED"));
    }
}
