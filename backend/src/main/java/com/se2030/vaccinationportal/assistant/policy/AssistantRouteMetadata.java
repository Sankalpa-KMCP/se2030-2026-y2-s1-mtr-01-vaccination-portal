package com.se2030.vaccinationportal.assistant.policy;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Metadata defining a recognized internal application route, including
 * classification, allowed roles, label, and parametric route characteristics.
 */
public record AssistantRouteMetadata(
        String path,
        Pattern pattern,
        boolean isPublic,
        Set<String> allowedRoles,
        String defaultLabel,
        boolean parameterized
) {
    public AssistantRouteMetadata {
        Objects.requireNonNull(defaultLabel, "defaultLabel must not be null");
        if (allowedRoles == null) {
            allowedRoles = Collections.emptySet();
        } else {
            allowedRoles = Collections.unmodifiableSet(allowedRoles);
        }
    }

    public static AssistantRouteMetadata ofExact(String path, boolean isPublic, Set<String> allowedRoles, String defaultLabel) {
        return new AssistantRouteMetadata(path, null, isPublic, allowedRoles, defaultLabel, false);
    }

    public static AssistantRouteMetadata ofParametric(String patternRegex, Set<String> allowedRoles, String defaultLabel) {
        return new AssistantRouteMetadata(null, Pattern.compile(patternRegex), false, allowedRoles, defaultLabel, true);
    }

    public boolean matches(String targetPath) {
        if (targetPath == null || targetPath.isBlank()) {
            return false;
        }
        if (path != null) {
            return path.equals(targetPath);
        }
        if (pattern != null) {
            return pattern.matcher(targetPath).matches();
        }
        return false;
    }
}
