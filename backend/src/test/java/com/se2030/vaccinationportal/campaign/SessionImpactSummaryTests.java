package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.service.SessionImpactSummary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionImpactSummaryTests {

    @Test
    void validCountsInitializeSummarySuccessfully() {
        SessionImpactSummary summary = new SessionImpactSummary(15, 3);

        assertAll(
                () -> assertEquals(15, summary.affectedAppointmentCount()),
                () -> assertEquals(3, summary.affectedWaitlistCount())
        );
    }

    @Test
    void zeroCountsPermitted() {
        SessionImpactSummary summary = new SessionImpactSummary(0, 0);

        assertAll(
                () -> assertEquals(0, summary.affectedAppointmentCount()),
                () -> assertEquals(0, summary.affectedWaitlistCount())
        );
    }

    @Test
    void negativeAppointmentCountRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionImpactSummary(-1, 0));
        assertEquals("Affected appointment count cannot be negative", ex.getMessage());
    }

    @Test
    void negativeWaitlistCountRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new SessionImpactSummary(0, -1));
        assertEquals("Affected waitlist count cannot be negative", ex.getMessage());
    }

    @Test
    void recordEqualityAndHashCode() {
        SessionImpactSummary s1 = new SessionImpactSummary(5, 2);
        SessionImpactSummary s2 = new SessionImpactSummary(5, 2);

        assertAll(
                () -> assertEquals(s1, s2),
                () -> assertEquals(s1.hashCode(), s2.hashCode())
        );
    }
}
