package com.se2030.vaccinationportal.campaign.center;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying exact CenterStatus canonical values.
 */
class CenterStatusTests {

    @Test
    @DisplayName("CenterStatus must contain exactly the 2 canonical values: ACTIVE and INACTIVE")
    void centerStatusContainsExactCanonicalValues() {
        CenterStatus[] values = CenterStatus.values();
        assertEquals(2, values.length, "CenterStatus must define exactly 2 values per canonical specification");

        List<String> expectedNames = List.of("ACTIVE", "INACTIVE");
        List<String> actualNames = Arrays.stream(values)
                .map(Enum::name)
                .toList();

        assertEquals(expectedNames, actualNames, "Enum names must match canonical values in exact specification order");
    }
}
