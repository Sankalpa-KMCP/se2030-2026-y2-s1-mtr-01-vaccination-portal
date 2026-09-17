package com.se2030.vaccinationportal.shared.auth.security;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service for generating cryptographically secure action tokens
 * and computing their SHA-256 hashes for database persistence.
 * Canonical Section 12.2 and Section 17.1.
 */
@Service
public class AuthActionTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a 256-bit cryptographically secure URL-safe token.
     *
     * @return 43-character URL-safe base64 string.
     */
    public String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Computes the SHA-256 hexadecimal hash of the supplied plaintext token.
     *
     * @param rawToken plaintext token.
     * @return 64-character lowercase hexadecimal hash.
     */
    public String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Raw token must not be null or blank");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Constant-time comparison between two SHA-256 hash strings to mitigate timing attacks.
     *
     * @param hashA first hash string
     * @param hashB second hash string
     * @return true if equal, false otherwise
     */
    public boolean constantTimeEquals(String hashA, String hashB) {
        if (hashA == null || hashB == null) {
            return false;
        }
        byte[] bytesA = hashA.getBytes(StandardCharsets.UTF_8);
        byte[] bytesB = hashB.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(bytesA, bytesB);
    }
}
