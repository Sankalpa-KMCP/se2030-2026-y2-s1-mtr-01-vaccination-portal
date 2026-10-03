package com.se2030.vaccinationportal.shared.auth.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * Generates the six-digit email verification OTP.
 * Codes are exactly six decimal digits, including leading zeros, and are drawn
 * from {@link SecureRandom}. Codes exist only in memory at generation time:
 * persistence stores a password-hash and delivery receives the value through a
 * single in-memory event, so no code is ever logged or persisted in plaintext.
 */
@Service
public class EmailOtpGenerator {

    private static final int OTP_UPPER_BOUND = 1_000_000;

    private final SecureRandom secureRandom;

    public EmailOtpGenerator() {
        this(new SecureRandom());
    }

    EmailOtpGenerator(SecureRandom secureRandom) {
        this.secureRandom = Objects.requireNonNull(secureRandom, "SecureRandom must not be null");
    }

    /** @return a cryptographically random, six decimal digit code such as "004281". */
    public String generate() {
        return format(secureRandom.nextInt(OTP_UPPER_BOUND));
    }

    static String format(int value) {
        return String.format("%06d", value);
    }
}
