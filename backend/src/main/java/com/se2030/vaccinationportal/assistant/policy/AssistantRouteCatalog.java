package com.se2030.vaccinationportal.assistant.policy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Curated allowlist of valid, internal front-end application routes.
 * Used to validate user currentPath and restrict AI suggestedActions.
 */
public final class AssistantRouteCatalog {

    private AssistantRouteCatalog() {
        // static utility
    }

    private static final Set<String> EXACT_ROUTES;
    private static final List<Pattern> PARAMETRIC_ROUTE_PATTERNS;

    static {
        Set<String> exact = new LinkedHashSet<>();
        // Public routes
        exact.add("/");
        exact.add("/how-it-works");
        exact.add("/about");
        exact.add("/verify-certificate");
        exact.add("/login");
        exact.add("/login/citizen");
        exact.add("/login/staff");
        exact.add("/register/citizen");
        exact.add("/register/staff");
        exact.add("/verify-email");
        exact.add("/forgot-password");
        exact.add("/reset-password");

        // Authenticated routes
        exact.add("/app");
        exact.add("/app/overview");
        exact.add("/app/not-authorized");
        exact.add("/app/citizen");
        exact.add("/app/citizen/profile");
        exact.add("/app/citizen/vaccinations");
        exact.add("/app/vaccinations");
        exact.add("/app/screening");
        exact.add("/app/screening/eligibility");
        exact.add("/app/screening/history");
        exact.add("/app/appointments/book");
        exact.add("/app/appointments/mine");
        exact.add("/app/appointments/waitlist");
        exact.add("/app/staff");
        exact.add("/app/staff/vaccination-drafts");
        exact.add("/app/staff/screening/reviews");
        exact.add("/app/admin");
        exact.add("/app/admin/staff-requests");
        exact.add("/app/campaigns");
        exact.add("/app/center-operations");
        exact.add("/app/sessions");
        exact.add("/app/analytics");
        exact.add("/app/checkin/queue");
        exact.add("/app/inventory");
        exact.add("/app/screening/questions");
        exact.add("/app/screening/questionnaires");

        EXACT_ROUTES = Collections.unmodifiableSet(exact);

        // Parametric routes (e.g. /app/screening/campaigns/123, /app/screening/history/456, etc.)
        PARAMETRIC_ROUTE_PATTERNS = List.of(
                Pattern.compile("^/app/screening/campaigns/[a-zA-Z0-9_-]+$"),
                Pattern.compile("^/app/screening/history/[a-zA-Z0-9_-]+$"),
                Pattern.compile("^/app/screening/history/[a-zA-Z0-9_-]+/correct$"),
                Pattern.compile("^/app/staff/screening/reviews/[a-zA-Z0-9_-]+$"),
                Pattern.compile("^/app/staff/screening/history/[a-zA-Z0-9_-]+$")
        );
    }

    /**
     * Checks if the given path is a safe, allowed internal frontend route.
     * Rejects null, empty, external URLs, protocol-relative URLs, directory traversal,
     * javascript schemes, and unregistered routes.
     */
    public static boolean isAllowedRoute(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        String trimmed = path.trim();

        // Must start with single slash and cannot start with // (protocol relative)
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return false;
        }

        // Reject backslashes, colon (e.g. javascript: or http:), directory traversal, or url-encoded variations
        if (trimmed.contains("\\") || trimmed.contains(":") || trimmed.contains("..")
                || trimmed.contains("%") || trimmed.contains("\n") || trimmed.contains("\r")
                || trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("\"")
                || trimmed.contains("'")) {
            return false;
        }

        // Exact match
        if (EXACT_ROUTES.contains(trimmed)) {
            return true;
        }

        // Parametric match
        for (Pattern pattern : PARAMETRIC_ROUTE_PATTERNS) {
            if (pattern.matcher(trimmed).matches()) {
                return true;
            }
        }

        return false;
    }

    public static Set<String> getExactRoutes() {
        return EXACT_ROUTES;
    }
}
