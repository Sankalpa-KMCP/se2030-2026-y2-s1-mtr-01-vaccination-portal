package com.se2030.vaccinationportal.campaign.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneOffset;

/** Rejects invalid date combinations before a request reaches the service. */
public class CampaignDatesValidator implements ConstraintValidator<ValidCampaignDates, CampaignDates> {
    @Override
    public boolean isValid(CampaignDates dates, ConstraintValidatorContext context) {
        if (dates == null) {
            return true;
        }
        boolean valid = true;
        // Missing values are handled separately by the request's @NotNull checks.
        if (dates.serviceStartDate() != null && dates.serviceEndDate() != null
                && !dates.serviceEndDate().isAfter(dates.serviceStartDate())) {
            addError(context, "serviceEndDate", "Service end date must be after service start date");
            valid = false;
        }
        if (dates.bookingOpenAt() != null && dates.bookingCloseAt() != null
                && !dates.bookingCloseAt().isAfter(dates.bookingOpenAt())) {
            addError(context, "bookingCloseAt", "Booking close time must be after booking open time");
            valid = false;
        }
        // Use UTC to match the existing service and campaign model checks.
        if (dates.bookingCloseAt() != null && dates.serviceEndDate() != null
                && dates.bookingCloseAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(dates.serviceEndDate())) {
            addError(context, "bookingCloseAt", "Booking close date must not be after campaign service end date");
            valid = false;
        }
        return valid;
    }

    private void addError(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
