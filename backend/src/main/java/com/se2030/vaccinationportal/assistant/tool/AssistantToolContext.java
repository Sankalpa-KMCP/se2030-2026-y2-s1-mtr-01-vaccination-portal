package com.se2030.vaccinationportal.assistant.tool;

import com.se2030.vaccinationportal.assistant.policy.AssistantRouteCatalog;
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;

import java.util.Collections;
import java.util.Set;

/**
 * Execution context for assistant personalized tools, derived strictly from server-verified principal.
 */
public record AssistantToolContext(
        String requestId,
        UserPrincipal principal,
        Set<String> authorities
) {
    public AssistantToolContext {
        if (authorities == null) {
            authorities = Collections.emptySet();
        }
    }

    public Long getCitizenId() {
        return getUserId();
    }

    public Long getUserId() {
        return principal != null ? principal.getUserId() : null;
    }

    public boolean isCitizen() {
        return principal != null && authorities != null && authorities.contains("ROLE_CITIZEN");
    }

    public boolean isStaff() {
        return principal != null
                && authorities != null
                && (authorities.stream().anyMatch(AssistantRouteCatalog.STAFF_DASHBOARD_ROLES::contains)
                        || authorities.stream().anyMatch(AssistantRouteCatalog.ADMIN_ROLES::contains));
    }

    public boolean supportsPersonalizedTools() {
        return isCitizen() || isStaff();
    }
}
