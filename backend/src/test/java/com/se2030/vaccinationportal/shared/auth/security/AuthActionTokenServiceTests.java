package com.se2030.vaccinationportal.shared.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthActionTokenServiceTests {

    private final AuthActionTokenService service = new AuthActionTokenService();

    @Test
    @DisplayName("generateToken generates 256-bit URL-safe Base64 token")
    void generateToken_returnsUrlSafeToken() {
        String token1 = service.generateToken();
        String token2 = service.generateToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertEquals(43, token1.length(), "32 random bytes without padding encode to 43 base64 characters");
        assertFalse(token1.contains("+"));
        assertFalse(token1.contains("/"));
        assertFalse(token1.contains("="));
    }

    @Test
    @DisplayName("hashToken produces deterministic 64-character lowercase SHA-256 hex string")
    void hashToken_deterministicSha256Hex() {
        String token = "sample-token-value-12345";
        String hash1 = service.hashToken(token);
        String hash2 = service.hashToken(token);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length());
        assertTrue(hash1.matches("^[0-9a-f]{64}$"));

        String hashOther = service.hashToken("different-token");
        assertNotEquals(hash1, hashOther);
    }

    @Test
    @DisplayName("hashToken rejects null or blank input")
    void hashToken_rejectsNullOrBlank() {
        assertThrows(IllegalArgumentException.class, () -> service.hashToken(null));
        assertThrows(IllegalArgumentException.class, () -> service.hashToken(""));
        assertThrows(IllegalArgumentException.class, () -> service.hashToken("   "));
    }

    @Test
    @DisplayName("constantTimeEquals behaves correctly for matching and non-matching hashes")
    void constantTimeEquals_verifiesEquivalence() {
        String hash = service.hashToken("test");
        assertTrue(service.constantTimeEquals(hash, hash));
        assertFalse(service.constantTimeEquals(hash, service.hashToken("other")));
        assertFalse(service.constantTimeEquals(hash, null));
        assertFalse(service.constantTimeEquals(null, hash));
    }
}
