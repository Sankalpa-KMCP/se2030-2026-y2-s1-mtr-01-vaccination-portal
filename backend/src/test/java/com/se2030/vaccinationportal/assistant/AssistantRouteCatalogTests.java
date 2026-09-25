package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("AssistantRouteCatalog Allowlist Tests")
class AssistantRouteCatalogTests {

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/how-it-works",
            "/about",
            "/verify-certificate",
            "/login",
            "/register/citizen",
            "/register/staff",
            "/verify-email",
            "/app",
            "/app/citizen",
            "/app/citizen/profile",
            "/app/screening",
            "/app/screening/eligibility",
            "/app/screening/history",
            "/app/appointments/book",
            "/app/appointments/mine",
            "/app/appointments/waitlist",
            "/app/staff",
            "/app/admin",
            "/app/campaigns",
            "/app/center-operations",
            "/app/sessions",
            "/app/checkin/queue",
            "/app/inventory"
    })
    @DisplayName("Valid public and authenticated routes are accepted")
    void testValidRoutes(String route) {
        assertTrue(AssistantRouteCatalog.isAllowedRoute(route), "Route should be allowed: " + route);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/app/screening/campaigns/campaign-123",
            "/app/screening/history/submission-999",
            "/app/screening/history/sub-456/correct",
            "/app/staff/screening/reviews/rev-101",
            "/app/staff/screening/history/sub-789"
    })
    @DisplayName("Valid parametric routes are accepted")
    void testValidParametricRoutes(String route) {
        assertTrue(AssistantRouteCatalog.isAllowedRoute(route), "Parametric route should be allowed: " + route);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://evil.com",
            "https://malicious.site/phish",
            "//evil.com/app/citizen",
            "/app/citizen/../../../etc/passwd",
            "/app/citizen%2e%2e/secret",
            "javascript:alert(1)",
            "data:text/html,<script>",
            "/app/nonexistent-route",
            "\\app\\citizen",
            "/app/citizen\n/evil",
            "",
            "   "
    })
    @DisplayName("Dangerous, external, protocol-relative, and unlisted routes are strictly rejected")
    void testRejectedRoutes(String route) {
        assertFalse(AssistantRouteCatalog.isAllowedRoute(route), "Route should be rejected: " + route);
    }

    @Test
    @DisplayName("Null route is safely rejected")
    void testNullRoute() {
        assertFalse(AssistantRouteCatalog.isAllowedRoute(null));
    }
}
