package com.se2030.vaccinationportal.assistant.policy;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that an optional string represents a safe, allowlisted internal application path.
 */
@Documented
@Constraint(validatedBy = SafeRouteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SafeRoute {
    String message() default "currentPath must be an allowlisted internal portal route";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
