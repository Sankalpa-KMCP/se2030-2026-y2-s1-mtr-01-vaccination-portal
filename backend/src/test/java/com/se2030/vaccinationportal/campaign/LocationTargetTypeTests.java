package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.LocationTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying exact LocationTargetType canonical values.
 */
class LocationTargetTypeTests {

    @Test
    @DisplayName("LocationTargetType must contain exactly the 4 canonical values: PROVINCE, DISTRICT, HEALTH_REGION, CENTER")
    void locationTargetTypeContainsExactCanonicalValues() {
        LocationTargetType[] values = LocationTargetType.values();
        assertEquals(4, values.length, "LocationTargetType must define exactly 4 values per canonical specification");

        List<String> expectedNames = List.of("PROVINCE", "DISTRICT", "HEALTH_REGION", "CENTER");
        List<String> actualNames = Arrays.stream(values)
                .map(Enum::name)
                .toList();

        assertEquals(expectedNames, actualNames, "Enum names must match canonical values in exact specification order");
    }
}
