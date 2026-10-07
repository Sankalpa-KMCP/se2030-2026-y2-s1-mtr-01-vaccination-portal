package com.se2030.vaccinationportal.shared.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailOtpGeneratorTests {

    private static final Pattern SIX_DIGITS = Pattern.compile("\\d{6}");

    @Test
    @DisplayName("generated codes are always exactly six decimal digits")
    void generate_alwaysSixDigits() {
        EmailOtpGenerator generator = new EmailOtpGenerator(new SecureRandom());
        for (int i = 0; i < 500; i++) {
            String otp = generator.generate();
            assertTrue(SIX_DIGITS.matcher(otp).matches(),
                    "Generated code must be six decimal digits but was [" + otp + "]");
        }
    }

    @Test
    @DisplayName("leading-zero codes are representable end to end")
    void format_preservesLeadingZeros() {
        assertEquals("000000", EmailOtpGenerator.format(0));
        assertEquals("000019", EmailOtpGenerator.format(19));
        assertEquals("004281", EmailOtpGenerator.format(4281));
        assertEquals("713902", EmailOtpGenerator.format(713902));
        assertEquals("999999", EmailOtpGenerator.format(999_999));
    }

    @Test
    @DisplayName("formatting stays within the six digit domain for every drawable value")
    void format_neverExceedsSixDigits() {
        assertEquals(6, EmailOtpGenerator.format(999_999).length());
        assertEquals(6, EmailOtpGenerator.format(0).length());
    }
}
