package com.se2030.vaccinationportal.appointment.persistence.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CitizenCampaignBookingGuardModelTests {

    @Test
    @DisplayName("CitizenCampaignBookingGuard entity initializes fields correctly")
    void testGuardInitialization() {
        CitizenCampaignBookingGuard guard = new CitizenCampaignBookingGuard(150L, 250L);
        guard.onCreate();

        assertEquals(150L, guard.getCitizenId());
        assertEquals(250L, guard.getCampaignId());
        assertEquals(0L, guard.getLockVersion());
        assertNotNull(guard.getCreatedAt());
    }
}
