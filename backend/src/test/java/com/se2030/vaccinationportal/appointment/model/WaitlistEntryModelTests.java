package com.se2030.vaccinationportal.appointment.model;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WaitlistEntryModelTests {

    @Test
    @DisplayName("WaitlistEntry initializes correctly with constructor defaults")
    void testConstructorAndGetters() {
        Long citizenId = 1001L;
        Long campaignId = 2001L;
        Long sessionId = 3001L;
        Instant now = Instant.now();

        WaitlistEntry entry = new WaitlistEntry(citizenId, campaignId, sessionId, WaitlistStatus.WAITING, now);

        assertEquals(citizenId, entry.getCitizenId());
        assertEquals(campaignId, entry.getCampaignId());
        assertEquals(sessionId, entry.getSessionId());
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());
        assertEquals(now, entry.getJoinedAt());
        assertEquals(0L, entry.getLockVersion());
    }

    @Test
    @DisplayName("WaitlistEntry transitionStatus mutates status correctly")
    void testTransitionStatus() {
        WaitlistEntry entry = new WaitlistEntry(1001L, 2001L, 3001L);
        assertEquals(WaitlistStatus.WAITING, entry.getStatus());

        entry.transitionStatus(WaitlistStatus.PROMOTED);
        assertEquals(WaitlistStatus.PROMOTED, entry.getStatus());

        assertThrows(IllegalArgumentException.class, () -> entry.transitionStatus(null));
    }
}
