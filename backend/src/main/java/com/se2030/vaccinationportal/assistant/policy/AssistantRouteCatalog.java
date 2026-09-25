package com.se2030.vaccinationportal.assistant.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Curated registry and authorization engine for front-end application routes.
 * Maps route patterns directly to frontend/src/app/routes.jsx definitions and enforces
 * strict role-based route visibility for assistant suggested actions.
 */
public final class AssistantRouteCatalog {

    private AssistantRouteCatalog() {
        // static utility
    }

    public static final String ROLE_CITIZEN = "ROLE_CITIZEN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String ROLE_CAMPAIGN_OFFICER = "ROLE_CAMPAIGN_OFFICER";
    public static final String ROLE_CENTER_MANAGER = "ROLE_CENTER_MANAGER";
    public static final String ROLE_HEALTHCARE_OFFICER = "ROLE_HEALTHCARE_OFFICER";
    public static final String ROLE_INVENTORY_OFFICER = "ROLE_INVENTORY_OFFICER";
    public static final String ROLE_MANAGEMENT_USER = "ROLE_MANAGEMENT_USER";
    public static final String ROLE_VACCINATOR = "ROLE_VACCINATOR";

    /**
     * Standard staff dashboard roles as defined in authRoles.js and SecurityConfig.
     */
    public static final Set<String> STAFF_DASHBOARD_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_CAMPAIGN_OFFICER,
            ROLE_CENTER_MANAGER,
            ROLE_HEALTHCARE_OFFICER,
            ROLE_INVENTORY_OFFICER,
            ROLE_MANAGEMENT_USER,
            ROLE_VACCINATOR
    );

    public static final Set<String> CAMPAIGN_VIEW_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_CAMPAIGN_OFFICER,
            ROLE_CENTER_MANAGER,
            ROLE_MANAGEMENT_USER
    );

    public static final Set<String> CENTER_VIEW_ROLES = CAMPAIGN_VIEW_ROLES;

    public static final Set<String> SESSION_VIEW_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_CENTER_MANAGER,
            ROLE_CAMPAIGN_OFFICER
    );

    public static final Set<String> ANALYTICS_VIEW_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_CAMPAIGN_OFFICER,
            ROLE_CENTER_MANAGER
    );

    public static final Set<String> CHECK_IN_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_CENTER_MANAGER,
            ROLE_VACCINATOR
    );

    public static final Set<String> INVENTORY_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_INVENTORY_OFFICER
    );

    public static final Set<String> SCREENING_STAFF_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN,
            ROLE_HEALTHCARE_OFFICER
    );

    public static final Set<String> ADMIN_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_SUPER_ADMIN
    );

    public static final Set<String> VACCINATOR_ROLES = Set.of(
            ROLE_VACCINATOR,
            ROLE_SUPER_ADMIN
    );

    /**
     * Role hierarchy implied by SUPER_ADMIN in the project SecurityConfig.
     * Note: SUPER_ADMIN does NOT imply ROLE_CITIZEN.
     */
    private static final Set<String> SUPER_ADMIN_IMPLIED_ROLES = Set.of(
            ROLE_ADMIN,
            ROLE_CAMPAIGN_OFFICER,
            ROLE_CENTER_MANAGER,
            ROLE_HEALTHCARE_OFFICER,
            ROLE_INVENTORY_OFFICER,
            ROLE_MANAGEMENT_USER,
            ROLE_VACCINATOR
    );

    private static final List<AssistantRouteMetadata> ROUTE_REGISTRY;

    static {
        List<AssistantRouteMetadata> registry = new ArrayList<>();

        // 1. Public Routes
        registry.add(AssistantRouteMetadata.ofExact("/", true, Collections.emptySet(), "Home"));
        registry.add(AssistantRouteMetadata.ofExact("/how-it-works", true, Collections.emptySet(), "How It Works"));
        registry.add(AssistantRouteMetadata.ofExact("/about", true, Collections.emptySet(), "About Portal"));
        registry.add(AssistantRouteMetadata.ofExact("/verify-certificate", true, Collections.emptySet(), "Verify Certificate"));
        registry.add(AssistantRouteMetadata.ofExact("/login", true, Collections.emptySet(), "Sign In"));
        registry.add(AssistantRouteMetadata.ofExact("/login/citizen", true, Collections.emptySet(), "Citizen Sign In"));
        registry.add(AssistantRouteMetadata.ofExact("/login/staff", true, Collections.emptySet(), "Staff Sign In"));
        registry.add(AssistantRouteMetadata.ofExact("/register/citizen", true, Collections.emptySet(), "Citizen Registration"));
        registry.add(AssistantRouteMetadata.ofExact("/register/staff", true, Collections.emptySet(), "Staff Registration Request"));
        registry.add(AssistantRouteMetadata.ofExact("/verify-email", true, Collections.emptySet(), "Verify Email"));
        registry.add(AssistantRouteMetadata.ofExact("/forgot-password", true, Collections.emptySet(), "Forgot Password"));
        registry.add(AssistantRouteMetadata.ofExact("/reset-password", true, Collections.emptySet(), "Reset Password"));

        // 2. Common Authenticated Routes (accessible to any authenticated role)
        registry.add(AssistantRouteMetadata.ofExact("/app", false, Collections.emptySet(), "Application Portal"));
        registry.add(AssistantRouteMetadata.ofExact("/app/overview", false, Collections.emptySet(), "Portal Overview"));
        registry.add(AssistantRouteMetadata.ofExact("/app/not-authorized", false, Collections.emptySet(), "Access Restricted"));

        // 3. Citizen Routes (ROLE_CITIZEN only)
        Set<String> citizenOnly = Set.of(ROLE_CITIZEN);
        registry.add(AssistantRouteMetadata.ofExact("/app/citizen", false, citizenOnly, "Citizen Dashboard"));
        registry.add(AssistantRouteMetadata.ofExact("/app/citizen/profile", false, citizenOnly, "Citizen Profile"));
        registry.add(AssistantRouteMetadata.ofExact("/app/citizen/vaccinations", false, citizenOnly, "My Vaccinations"));
        registry.add(AssistantRouteMetadata.ofExact("/app/vaccinations", false, citizenOnly, "My Vaccinations"));
        registry.add(AssistantRouteMetadata.ofExact("/app/screening", false, citizenOnly, "Screening Campaigns"));
        registry.add(AssistantRouteMetadata.ofExact("/app/screening/eligibility", false, citizenOnly, "Eligibility Status"));
        registry.add(AssistantRouteMetadata.ofExact("/app/screening/history", false, citizenOnly, "Screening History"));
        registry.add(AssistantRouteMetadata.ofExact("/app/appointments/book", false, citizenOnly, "Book Appointment"));
        registry.add(AssistantRouteMetadata.ofExact("/app/appointments/mine", false, citizenOnly, "My Appointments"));
        registry.add(AssistantRouteMetadata.ofExact("/app/appointments/waitlist", false, citizenOnly, "My Waitlist"));

        // Parametric Citizen Routes
        registry.add(AssistantRouteMetadata.ofParametric("^/app/screening/campaigns/[a-zA-Z0-9_-]+$", citizenOnly, "Campaign Screening"));
        registry.add(AssistantRouteMetadata.ofParametric("^/app/screening/history/[a-zA-Z0-9_-]+$", citizenOnly, "Screening Submission Details"));
        registry.add(AssistantRouteMetadata.ofParametric("^/app/screening/history/[a-zA-Z0-9_-]+/correct$", citizenOnly, "Screening Correction Form"));

        // 4. Staff Routes (Specific Staff Roles)
        registry.add(AssistantRouteMetadata.ofExact("/app/staff", false, STAFF_DASHBOARD_ROLES, "Staff Dashboard"));
        registry.add(AssistantRouteMetadata.ofExact("/app/staff/vaccination-drafts", false, VACCINATOR_ROLES, "Vaccination Drafts"));
        registry.add(AssistantRouteMetadata.ofExact("/app/staff/screening/reviews", false, SCREENING_STAFF_ROLES, "Screening Review Queue"));
        registry.add(AssistantRouteMetadata.ofExact("/app/screening/questions", false, SCREENING_STAFF_ROLES, "Screening Questions Management"));
        registry.add(AssistantRouteMetadata.ofExact("/app/screening/questionnaires", false, SCREENING_STAFF_ROLES, "Screening Questionnaires Management"));

        // Parametric Staff Routes
        registry.add(AssistantRouteMetadata.ofParametric("^/app/staff/screening/reviews/[a-zA-Z0-9_-]+$", SCREENING_STAFF_ROLES, "Screening Review Details"));
        registry.add(AssistantRouteMetadata.ofParametric("^/app/staff/screening/history/[a-zA-Z0-9_-]+$", SCREENING_STAFF_ROLES, "Screening History Details"));

        // 5. Functional Operational Modules
        registry.add(AssistantRouteMetadata.ofExact("/app/campaigns", false, CAMPAIGN_VIEW_ROLES, "Campaign Management"));
        registry.add(AssistantRouteMetadata.ofExact("/app/center-operations", false, CENTER_VIEW_ROLES, "Center Operations"));
        registry.add(AssistantRouteMetadata.ofExact("/app/sessions", false, SESSION_VIEW_ROLES, "Session Management"));
        registry.add(AssistantRouteMetadata.ofExact("/app/analytics", false, ANALYTICS_VIEW_ROLES, "Operational Analytics"));
        registry.add(AssistantRouteMetadata.ofExact("/app/checkin/queue", false, CHECK_IN_ROLES, "Check-In Queue"));
        registry.add(AssistantRouteMetadata.ofExact("/app/inventory", false, INVENTORY_ROLES, "Inventory Management"));

        // 6. Admin Routes
        registry.add(AssistantRouteMetadata.ofExact("/app/admin", false, ADMIN_ROLES, "Administrator Dashboard"));
        registry.add(AssistantRouteMetadata.ofExact("/app/admin/staff-requests", false, ADMIN_ROLES, "Pending Staff Requests"));

        ROUTE_REGISTRY = Collections.unmodifiableList(registry);
    }

    /**
     * Syntactic check verifying that the path is an allowlisted internal frontend route.
     * Rejects traversal, absolute URLs, backslashes, and unknown paths.
     */
    public static boolean isAllowedRoute(String path) {
        if (!isSyntacticallySafe(path)) {
            return false;
        }

        String trimmed = path.trim();
        for (AssistantRouteMetadata meta : ROUTE_REGISTRY) {
            if (meta.matches(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Role-aware route authorization. Verifies that the path is an allowlisted internal route
     * AND that the caller with given authorities is authorized to navigate to it.
     *
     * @param path              the target route path
     * @param callerAuthorities the normalized set of GrantedAuthority strings held by the caller
     * @return true if the route is valid and authorized for the caller; false otherwise
     */
    public static boolean isAuthorizedRoute(String path, Set<String> callerAuthorities) {
        if (!isSyntacticallySafe(path)) {
            return false;
        }

        String trimmed = path.trim();
        Optional<AssistantRouteMetadata> match = findMetadata(trimmed);
        if (match.isEmpty()) {
            return false;
        }

        AssistantRouteMetadata metadata = match.get();

        // Public routes are authorized for everyone (anonymous or authenticated)
        if (metadata.isPublic()) {
            return true;
        }

        // Protected routes require an authenticated caller with authorities
        if (callerAuthorities == null || callerAuthorities.isEmpty()) {
            return false;
        }

        // Common authenticated routes (empty allowedRoles) permit any authenticated user
        if (metadata.allowedRoles().isEmpty()) {
            return true;
        }

        // Expand authorities according to the project's role hierarchy (SUPER_ADMIN)
        Set<String> expandedAuthorities = expandAuthorities(callerAuthorities);

        // Check if any caller authority matches the route's allowed roles
        for (String role : metadata.allowedRoles()) {
            if (expandedAuthorities.contains(role)) {
                return true;
            }
        }

        return false;
    }

    public static Optional<AssistantRouteMetadata> findMetadata(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        String trimmed = path.trim();
        for (AssistantRouteMetadata meta : ROUTE_REGISTRY) {
            if (meta.matches(trimmed)) {
                return Optional.of(meta);
            }
        }
        return Optional.empty();
    }

    public static List<AssistantRouteMetadata> getRegistry() {
        return ROUTE_REGISTRY;
    }

    private static boolean isSyntacticallySafe(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }

        String trimmed = path.trim();

        // Must start with single slash and cannot start with // (protocol relative)
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return false;
        }

        // Reject backslashes, colon, directory traversal, url-encoding, or control characters
        return !trimmed.contains("\\") && !trimmed.contains(":") && !trimmed.contains("..")
                && !trimmed.contains("%") && !trimmed.contains("\n") && !trimmed.contains("\r")
                && !trimmed.contains("<") && !trimmed.contains(">") && !trimmed.contains("\"")
                && !trimmed.contains("'");
    }

    private static Set<String> expandAuthorities(Set<String> authorities) {
        if (!authorities.contains(ROLE_SUPER_ADMIN)) {
            return authorities;
        }

        Set<String> expanded = new HashSet<>(authorities);
        expanded.addAll(SUPER_ADMIN_IMPLIED_ROLES);
        return expanded;
    }
}
