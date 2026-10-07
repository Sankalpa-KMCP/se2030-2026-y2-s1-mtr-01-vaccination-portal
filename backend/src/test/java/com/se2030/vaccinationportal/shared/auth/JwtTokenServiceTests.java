package com.se2030.vaccinationportal.shared.auth;

import com.se2030.vaccinationportal.shared.auth.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTests {

    private static final String TEST_SECRET = "TestSecretKeyForJwtSigningMustBeAtLeast256BitsLongForHmacSha256Security1234567890";
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(TEST_SECRET, 15);
    }

    @Test
    @DisplayName("generateToken issues token with canonical 15-minute expiration")
    void generateToken_has15MinuteExpiration() {
        long beforeSeconds = System.currentTimeMillis() / 1000;
        String token = jwtTokenService.generateToken(101L, "user@portal.gov", List.of("ROLE_CITIZEN"));
        long afterSeconds = System.currentTimeMillis() / 1000;

        io.jsonwebtoken.Claims claims = jwtTokenService.extractAllClaims(token);
        long issuedAtSeconds = claims.getIssuedAt().getTime() / 1000;
        long expirationSeconds = claims.getExpiration().getTime() / 1000;
        long durationMinutes = (expirationSeconds - issuedAtSeconds) / 60;

        assertEquals(15, durationMinutes, "Token must expire exactly 15 minutes after issuance");
        assertTrue(issuedAtSeconds >= beforeSeconds && issuedAtSeconds <= afterSeconds);
    }

    @Test
    @DisplayName("generateToken and validateToken succeed for valid input")
    void generateAndValidateToken_success() {
        String token = jwtTokenService.generateToken(101L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));

        assertNotNull(token);
        assertTrue(jwtTokenService.validateToken(token));
        assertEquals(101L, jwtTokenService.extractUserId(token));
        assertEquals("manager@portal.gov", jwtTokenService.extractEmail(token));
        assertEquals(List.of("ROLE_CENTER_MANAGER"), jwtTokenService.extractRoles(token));
    }

    @Test
    @DisplayName("validateToken returns false for tampered token")
    void validateToken_tamperedSignature_returnsFalse() {
        String token = jwtTokenService.generateToken(101L, "manager@portal.gov", List.of("ROLE_CENTER_MANAGER"));
        String tampered = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtTokenService.validateToken(tampered));
    }

    @Test
    @DisplayName("validateToken returns false for malformed token string")
    void validateToken_malformedToken_returnsFalse() {
        assertFalse(jwtTokenService.validateToken("not.a.valid.jwt.token"));
        assertFalse(jwtTokenService.validateToken(""));
        assertFalse(jwtTokenService.validateToken(null));
    }

    @Test
    @DisplayName("validateToken returns false for expired token")
    void validateToken_expiredToken_returnsFalse() {
        // Service with negative expiration duration produces an already-expired token
        JwtTokenService expiredService = new JwtTokenService(TEST_SECRET, -10);
        String expiredToken = expiredService.generateToken(102L, "expired@portal.gov", List.of("ROLE_CITIZEN"));

        assertFalse(jwtTokenService.validateToken(expiredToken));
    }

    @Test
    @DisplayName("constructor fails when secret is null or shorter than 256 bits (32 bytes)")
    void constructor_shortSecret_throwsIllegalStateException() {
        JwtTokenService invalidService = new JwtTokenService("too-short-secret", 60);
        assertThrows(IllegalStateException.class, () -> invalidService.generateToken(1L, "user@portal.gov", List.of()));
    }
}
