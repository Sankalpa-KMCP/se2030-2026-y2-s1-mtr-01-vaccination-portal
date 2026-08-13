package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CampaignStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying exact CampaignStatus enum values and lifecycle helper rules
 * defined in canonical specification WBVP-SE2030 section 6.6 / E.5.
 */
class CampaignStatusTests {

    @Test
    @DisplayName("CampaignStatus must contain exactly the 8 canonical status values")
    void campaignStatusContainsExactCanonicalValues() {
        CampaignStatus[] values = CampaignStatus.values();
        assertEquals(8, values.length, "CampaignStatus must define exactly 8 values per canonical baseline");

        List<String> expectedNames = List.of(
                "DRAFT",
                "SCHEDULED",
                "PUBLISHED",
                "ACTIVE",
                "SUSPENDED",
                "COMPLETED",
                "CANCELLED",
                "ARCHIVED"
        );

        List<String> actualNames = Arrays.stream(values)
                .map(Enum::name)
                .toList();

        assertEquals(expectedNames, actualNames, "Enum names must match canonical values in exact specification order");
    }

    @Test
    @DisplayName("CampaignStatus intake permission matches MF02-BR-03 rules")
    void intakePermittedOnlyForPublishedAndActive() {
        assertTrue(CampaignStatus.PUBLISHED.isIntakePermitted(), "PUBLISHED campaigns must permit intake");
        assertTrue(CampaignStatus.ACTIVE.isIntakePermitted(), "ACTIVE campaigns must permit intake");

        assertFalse(CampaignStatus.DRAFT.isIntakePermitted(), "DRAFT must not permit booking intake");
        assertFalse(CampaignStatus.SCHEDULED.isIntakePermitted(), "SCHEDULED must not permit booking intake");
        assertFalse(CampaignStatus.SUSPENDED.isIntakePermitted(), "SUSPENDED must block new bookings (MF02-BR-03)");
        assertFalse(CampaignStatus.COMPLETED.isIntakePermitted(), "COMPLETED must block new bookings");
        assertFalse(CampaignStatus.CANCELLED.isIntakePermitted(), "CANCELLED must block new bookings");
        assertFalse(CampaignStatus.ARCHIVED.isIntakePermitted(), "ARCHIVED must block new bookings");
    }

    @Test
    @DisplayName("Operationally closed and archivable statuses remain distinct")
    void closedAndArchivableStatusesAreDistinguished() {
        assertTrue(CampaignStatus.COMPLETED.isOperationallyClosed());
        assertTrue(CampaignStatus.CANCELLED.isOperationallyClosed());
        assertTrue(CampaignStatus.ARCHIVED.isOperationallyClosed());

        assertTrue(CampaignStatus.COMPLETED.isArchivable());
        assertTrue(CampaignStatus.CANCELLED.isArchivable());
        assertFalse(CampaignStatus.ARCHIVED.isArchivable(), "ARCHIVED has no canonical outgoing transition");

        assertFalse(CampaignStatus.DRAFT.isOperationallyClosed());
        assertFalse(CampaignStatus.SCHEDULED.isOperationallyClosed());
        assertFalse(CampaignStatus.PUBLISHED.isOperationallyClosed());
        assertFalse(CampaignStatus.ACTIVE.isOperationallyClosed());
        assertFalse(CampaignStatus.SUSPENDED.isOperationallyClosed());
    }
}
