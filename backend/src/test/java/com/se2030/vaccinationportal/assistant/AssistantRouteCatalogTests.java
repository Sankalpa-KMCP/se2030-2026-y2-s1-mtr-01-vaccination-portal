package com.se2030.vaccinationportal.assistant;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import com.se2030.vaccinationportal.assistant.policy.AssistantRouteMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AssistantRouteCatalog Role-Aware Authorization Tests")
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
    @DisplayName("Valid public and authenticated routes pass syntactic safety check")
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
    @DisplayName("Valid parametric routes pass syntactic safety check")
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
    @DisplayName("Anonymous caller can ONLY access public routes")
    void testAnonymousRouteAuthorization() {
        Set<String> noAuthorities = Collections.emptySet();

        // Public routes: must succeed
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/", noAuthorities));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/about", noAuthorities));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/how-it-works", noAuthorities));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/verify-certificate", noAuthorities));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/login", noAuthorities));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/register/citizen", noAuthorities));

        // Citizen routes: must FAIL for anonymous
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/citizen", noAuthorities));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/appointments/book", noAuthorities));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/screening", noAuthorities));

        // Staff / Admin routes: must FAIL for anonymous
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff", noAuthorities));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/admin", noAuthorities));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/campaigns", noAuthorities));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/inventory", noAuthorities));
    }

    @Test
    @DisplayName("Citizen user can access public and citizen routes, but CANNOT access staff/admin routes")
    void testCitizenRouteAuthorization() {
        Set<String> citizenRoles = Set.of(AssistantRouteCatalog.ROLE_CITIZEN);

        // Public routes: permitted
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/", citizenRoles));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/about", citizenRoles));

        // Citizen routes: permitted
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/citizen", citizenRoles));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/citizen/profile", citizenRoles));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/appointments/book", citizenRoles));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/appointments/mine", citizenRoles));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/screening", citizenRoles));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/campaigns/camp-1", citizenRoles));

        // Staff / Admin routes: strictly prohibited for citizens
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/vaccination-drafts", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/reviews", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/admin", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/admin/staff-requests", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/campaigns", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/inventory", citizenRoles));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/checkin/queue", citizenRoles));
    }

    @Test
    @DisplayName("Staff users can only access routes authorized for their specific roles")
    void testStaffRouteAuthorization() {
        Set<String> vaccinator = Set.of(AssistantRouteCatalog.ROLE_VACCINATOR);

        // Vaccinator can access staff dashboard, drafts, check-in queue
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff", vaccinator));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/vaccination-drafts", vaccinator));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/checkin/queue", vaccinator));

        // Vaccinator cannot access admin or inventory routes
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/admin", vaccinator));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/admin/staff-requests", vaccinator));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/inventory", vaccinator));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/campaigns", vaccinator));

        Set<String> inventoryOfficer = Set.of(AssistantRouteCatalog.ROLE_INVENTORY_OFFICER);
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/inventory", inventoryOfficer));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff", inventoryOfficer));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/vaccination-drafts", inventoryOfficer));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/admin", inventoryOfficer));
    }

    @Test
    @DisplayName("Screening routes are authorized for Healthcare Officer and Super Admin, but strictly NOT for Admin")
    void testScreeningRouteRoleAuthorization() {
        Set<String> healthcareOfficer = Set.of(AssistantRouteCatalog.ROLE_HEALTHCARE_OFFICER);
        Set<String> adminOnly = Set.of(AssistantRouteCatalog.ROLE_ADMIN);
        Set<String> citizen = Set.of(AssistantRouteCatalog.ROLE_CITIZEN);

        // Healthcare Officer is authorized for screening review and configuration routes
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/reviews", healthcareOfficer));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/questions", healthcareOfficer));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/questionnaires", healthcareOfficer));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/reviews/rev-101", healthcareOfficer));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/history/sub-789", healthcareOfficer));

        // Admin-only caller must NOT be authorized for screening routes (matching frontend route guards)
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/reviews", adminOnly));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/questions", adminOnly));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/questionnaires", adminOnly));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/reviews/rev-101", adminOnly));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/history/sub-789", adminOnly));

        // Citizen caller must NOT be authorized for staff screening routes
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/screening/reviews", citizen));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/questions", citizen));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/screening/questionnaires", citizen));

        // Unrelated Admin routes remain authorized for Admin
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/admin", adminOnly));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/admin/staff-requests", adminOnly));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/campaigns", adminOnly));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/center-operations", adminOnly));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/sessions", adminOnly));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/analytics", adminOnly));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/inventory", adminOnly));
    }

    @Test
    @DisplayName("Super Admin inherits all staff and management roles, but never citizen self-service identity")
    void testSuperAdminAuthorization() {
        Set<String> superAdmin = Set.of(AssistantRouteCatalog.ROLE_SUPER_ADMIN);

        // Staff, operational and admin routes are accessible
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/admin", superAdmin));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff", superAdmin));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/staff/vaccination-drafts", superAdmin));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/campaigns", superAdmin));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/inventory", superAdmin));
        assertTrue(AssistantRouteCatalog.isAuthorizedRoute("/app/checkin/queue", superAdmin));

        // Citizen private self-service routes are NOT inherited by Super Admin
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/citizen", superAdmin));
        assertFalse(AssistantRouteCatalog.isAuthorizedRoute("/app/appointments/mine", superAdmin));
    }

    @Test
    @DisplayName("Route registry consistency check against frontend application routes")
    void testRouteRegistryConsistency() {
        // Every registered route in AssistantRouteCatalog must have a non-blank label and valid path/pattern
        assertFalse(AssistantRouteCatalog.getRegistry().isEmpty());

        for (AssistantRouteMetadata meta : AssistantRouteCatalog.getRegistry()) {
            assertNotNull(meta.defaultLabel());
            assertFalse(meta.defaultLabel().isBlank());

            if (meta.parameterized()) {
                assertNotNull(meta.pattern());
            } else {
                assertNotNull(meta.path());
                assertTrue(meta.path().startsWith("/"));
            }
        }
    }
}
