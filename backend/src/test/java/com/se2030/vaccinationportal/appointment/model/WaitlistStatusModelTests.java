package com.se2030.vaccinationportal.appointment.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaitlistStatusModelTests {

    @Test
    @DisplayName("WaitlistStatus enum contains expected statuses including reconciliation status")
    void testEnumValues() {
        WaitlistStatus[] statuses = WaitlistStatus.values();
        assertEquals(8, statuses.length, "WaitlistStatus must contain exactly 8 statuses including SKIPPED_MALFORMED");

        assertEquals(WaitlistStatus.WAITING, WaitlistStatus.valueOf("WAITING"));
        assertEquals(WaitlistStatus.PROMOTED, WaitlistStatus.valueOf("PROMOTED"));
        assertEquals(WaitlistStatus.SKIPPED_INELIGIBLE, WaitlistStatus.valueOf("SKIPPED_INELIGIBLE"));
        assertEquals(WaitlistStatus.SKIPPED_DUPLICATE_APPOINTMENT, WaitlistStatus.valueOf("SKIPPED_DUPLICATE_APPOINTMENT"));
        assertEquals(WaitlistStatus.SKIPPED_MALFORMED, WaitlistStatus.valueOf("SKIPPED_MALFORMED"));
        assertEquals(WaitlistStatus.CANCELLED, WaitlistStatus.valueOf("CANCELLED"));
        assertEquals(WaitlistStatus.SESSION_CANCELLED, WaitlistStatus.valueOf("SESSION_CANCELLED"));
        assertEquals(WaitlistStatus.EXPIRED, WaitlistStatus.valueOf("EXPIRED"));
    }
}
