package com.se2030.vaccinationportal.assistant.policy;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Constraint validator for @SafeRoute.
 * Returns true if value is null or empty (optional field), otherwise verifies allowlisted path.
 */
public class SafeRouteValidator implements ConstraintValidator<SafeRoute, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Optional field
        }
        return AssistantRouteCatalog.isAllowedRoute(value);
    }
}
