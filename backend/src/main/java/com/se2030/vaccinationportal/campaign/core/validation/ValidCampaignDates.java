package com.se2030.vaccinationportal.campaign.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CampaignDatesValidator.class)
public @interface ValidCampaignDates {
    String message() default "Campaign dates are invalid";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
