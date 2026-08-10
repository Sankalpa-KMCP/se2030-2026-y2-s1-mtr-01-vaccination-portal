package com.se2030.vaccinationportal.campaign;

import com.se2030.vaccinationportal.campaign.model.DoseCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying exact DoseCategory canonical values.
 */
class DoseCategoryTests {

    @Test
    @DisplayName("DoseCategory must contain exactly the 2 canonical values: PRIMARY and BOOSTER")
    void doseCategoryContainsExactCanonicalValues() {
        DoseCategory[] values = DoseCategory.values();
        assertEquals(2, values.length, "DoseCategory must define exactly 2 values per canonical baseline");

        List<String> expectedNames = List.of("PRIMARY", "BOOSTER");
        List<String> actualNames = Arrays.stream(values)
                .map(Enum::name)
                .toList();

        assertEquals(expectedNames, actualNames, "DoseCategory enum names must match canonical values exactly");
    }
}
