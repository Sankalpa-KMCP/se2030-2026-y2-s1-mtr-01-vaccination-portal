package com.se2030.vaccinationportal.campaign.dto.request;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Immutable trusted actor context representing the authenticated caller in operational campaign services.
 * Decoupled from Spring Security runtime types for testability.
 */
public record AuthenticatedActor(
        Long userId,
        Set<String> roles
) {
    public AuthenticatedActor {
        roles = roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet();
    }

    public boolean hasRole(String roleCode) {
        return roleCode != null && roles.contains(roleCode);
    }
}
