package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying domain invariants, validation rules, and code normalization
 * for {@link VaccinationCenter} (WBVP-SE2030 section 6.1 / 12).
 */
class VaccinationCenterModelTests {

    @Test
    @DisplayName("Valid vaccination center passes validation and normalizes code")
    void validVaccinationCenterPassesValidation() {
        VaccinationCenter center = new VaccinationCenter(
                "  ctr-ncmc  ",
                "National Children's Medical Center",
                "123 Healthcare Ave, Colombo 07",
                "+94 11 2345678",
                CenterStatus.ACTIVE,
                150,
                50
        );

        assertEquals("CTR-NCMC", center.getCode());
        assertEquals("National Children's Medical Center", center.getName());
        assertEquals("123 Healthcare Ave, Colombo 07", center.getAddress());
        assertEquals("+94 11 2345678", center.getContact());
        assertEquals(CenterStatus.ACTIVE, center.getCenterStatus());
        assertEquals(150, center.getSafeConcurrentOccupancy());
        assertEquals(50, center.getSessionServiceCeiling());
    }

    @Test
    @DisplayName("Null sessionServiceCeiling is permitted")
    void nullSessionServiceCeilingPermitted() {
        VaccinationCenter center = new VaccinationCenter(
                "CTR-COLOMBO-NORTH",
                "Colombo North General Hospital",
                "456 Hospital Rd, Colombo",
                "info@northgh.lk",
                CenterStatus.ACTIVE,
                200,
                null
        );

        assertNull(center.getSessionServiceCeiling());
        assertEquals(200, center.getSafeConcurrentOccupancy());
    }

    @Test
    @DisplayName("Blank or null code is rejected")
    void blankOrNullCodeRejected() {
        IllegalArgumentException exNull = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter(null, "Name", "Address", "Contact", CenterStatus.ACTIVE, 100, null)
        );
        assertTrue(exNull.getMessage().contains("Center code must not be null or blank"));

        IllegalArgumentException exBlank = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("   ", "Name", "Address", "Contact", CenterStatus.ACTIVE, 100, null)
        );
        assertTrue(exBlank.getMessage().contains("Center code must not be null or blank"));
    }

    @Test
    @DisplayName("Blank or null name is rejected")
    void blankOrNullNameRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "   ", "Address", "Contact", CenterStatus.ACTIVE, 100, null)
        );
        assertTrue(ex.getMessage().contains("Center name is required"));
    }

    @Test
    @DisplayName("Blank or null address is rejected")
    void blankOrNullAddressRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "   ", "Contact", CenterStatus.ACTIVE, 100, null)
        );
        assertTrue(ex.getMessage().contains("Address is required"));
    }

    @Test
    @DisplayName("Blank or null contact is rejected")
    void blankOrNullContactRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "Address", "   ", CenterStatus.ACTIVE, 100, null)
        );
        assertTrue(ex.getMessage().contains("Contact is required"));
    }

    @Test
    @DisplayName("Null centerStatus is rejected")
    void nullCenterStatusRejected() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "Address", "Contact", null, 100, null)
        );
        assertTrue(ex.getMessage().contains("Center status is required"));
    }

    @Test
    @DisplayName("Non-positive safeConcurrentOccupancy is rejected")
    void nonPositiveSafeConcurrentOccupancyRejected() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "Address", "Contact", CenterStatus.ACTIVE, 0, null)
        );
        assertTrue(exZero.getMessage().contains("Safe concurrent occupancy must be a positive integer"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "Address", "Contact", CenterStatus.ACTIVE, -10, null)
        );
        assertTrue(exNeg.getMessage().contains("Safe concurrent occupancy must be a positive integer"));
    }

    @Test
    @DisplayName("Non-positive sessionServiceCeiling is rejected when specified")
    void nonPositiveSessionServiceCeilingRejected() {
        IllegalArgumentException exZero = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "Address", "Contact", CenterStatus.ACTIVE, 100, 0)
        );
        assertTrue(exZero.getMessage().contains("Session service ceiling must be a positive integer when specified"));

        IllegalArgumentException exNeg = assertThrows(IllegalArgumentException.class, () ->
                new VaccinationCenter("CTR-01", "Name", "Address", "Contact", CenterStatus.ACTIVE, 100, -5)
        );
        assertTrue(exNeg.getMessage().contains("Session service ceiling must be a positive integer when specified"));
    }
}
