package com.se2030.vaccinationportal.shared.auth.controller;

import org.springframework.http.ResponseCookie;

/**
 * Central builder for the canonical §17.1 refresh-token cookie: HttpOnly, SameSite=Lax,
 * Secure per configuration, scoped to the auth endpoints so browser JS never sees the
 * token and other requests never carry it. Clearing must mirror the issued attributes.
 */
public final class RefreshTokenCookieSupport {

    public static final String COOKIE_PATH = "/api/v1/auth";

    private RefreshTokenCookieSupport() {
    }

    public static ResponseCookie build(String cookieName, String rawRefreshToken, long maxAgeSeconds, boolean secure) {
        return baseCookie(cookieName, secure)
                .value(rawRefreshToken)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public static ResponseCookie clear(String cookieName, boolean secure) {
        return baseCookie(cookieName, secure)
                .value("")
                .maxAge(0)
                .build();
    }

    private static ResponseCookie.ResponseCookieBuilder baseCookie(String cookieName, boolean secure) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH);
    }
}
