package com.se2030.vaccinationportal.vaccination.validation;

import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Component;

/** Canonical conversion between typed effective values and correction audit strings. */
@Component
public class VaccinationCorrectionValueCodec {

    private final Clock clock;

    public VaccinationCorrectionValueCodec(Clock clock) {
        this.clock = clock;
    }

    public String valueFrom(
            EffectiveVaccinationRecordSnapshot snapshot,
            VaccinationCorrectionField field
    ) {
        return switch (field) {
            case VACCINE_BATCH_ID -> snapshot.vaccineBatchId().toString();
            case DOSE_NUMBER -> snapshot.doseNumber().toString();
            case ADMINISTRATION_TIME -> snapshot.administrationTime().toString();
            case NEXT_DOSE_DATE -> valueOrNull(snapshot.nextDoseDate());
            case AUTHORIZED_NOTES -> snapshot.authorizedNotes();
        };
    }

    public String normalizeRequestedValue(VaccinationCorrectionField field, String value) {
        return switch (field) {
            case VACCINE_BATCH_ID -> normalizePositiveLong(value, "Vaccine batch ID");
            case DOSE_NUMBER -> normalizePositiveInteger(value, "Dose number");
            case ADMINISTRATION_TIME -> normalizeAdministrationTime(value);
            case NEXT_DOSE_DATE -> normalizeOptionalDate(value);
            case AUTHORIZED_NOTES -> normalizeOptionalNotes(value);
        };
    }

    public EffectiveVaccinationRecordSnapshot apply(
            EffectiveVaccinationRecordSnapshot snapshot,
            VaccinationCorrectionField field,
        String normalizedValue
    ) {
        try {
            return switch (field) {
                case VACCINE_BATCH_ID -> new EffectiveVaccinationRecordSnapshot(
                        snapshot.vaccinationRecordId(), snapshot.appointmentId(), snapshot.citizenId(),
                        snapshot.campaignId(), snapshot.centerId(), snapshot.vaccinatorId(),
                        Long.valueOf(normalizedValue), snapshot.doseNumber(), snapshot.administrationTime(),
                        snapshot.nextDoseDate(), snapshot.authorizedNotes(), snapshot.finalizedAt()
                );
                case DOSE_NUMBER -> new EffectiveVaccinationRecordSnapshot(
                        snapshot.vaccinationRecordId(), snapshot.appointmentId(), snapshot.citizenId(),
                        snapshot.campaignId(), snapshot.centerId(), snapshot.vaccinatorId(),
                        snapshot.vaccineBatchId(), Integer.valueOf(normalizedValue), snapshot.administrationTime(),
                        snapshot.nextDoseDate(), snapshot.authorizedNotes(), snapshot.finalizedAt()
                );
                case ADMINISTRATION_TIME -> new EffectiveVaccinationRecordSnapshot(
                        snapshot.vaccinationRecordId(), snapshot.appointmentId(), snapshot.citizenId(),
                        snapshot.campaignId(), snapshot.centerId(), snapshot.vaccinatorId(),
                        snapshot.vaccineBatchId(), snapshot.doseNumber(), Instant.parse(normalizedValue),
                        snapshot.nextDoseDate(), snapshot.authorizedNotes(), snapshot.finalizedAt()
                );
                case NEXT_DOSE_DATE -> new EffectiveVaccinationRecordSnapshot(
                        snapshot.vaccinationRecordId(), snapshot.appointmentId(), snapshot.citizenId(),
                        snapshot.campaignId(), snapshot.centerId(), snapshot.vaccinatorId(),
                        snapshot.vaccineBatchId(), snapshot.doseNumber(), snapshot.administrationTime(),
                        normalizedValue == null ? null : LocalDate.parse(normalizedValue),
                        snapshot.authorizedNotes(), snapshot.finalizedAt()
                );
                case AUTHORIZED_NOTES -> new EffectiveVaccinationRecordSnapshot(
                        snapshot.vaccinationRecordId(), snapshot.appointmentId(), snapshot.citizenId(),
                        snapshot.campaignId(), snapshot.centerId(), snapshot.vaccinatorId(),
                        snapshot.vaccineBatchId(), snapshot.doseNumber(), snapshot.administrationTime(),
                        snapshot.nextDoseDate(), normalizedValue, snapshot.finalizedAt()
                );
            };
        } catch (NumberFormatException | DateTimeParseException ex) {
            throw new IllegalStateException("Persisted vaccination correction contains an invalid value", ex);
        }
    }

    private String normalizePositiveLong(String value, String name) {
        String required = requireNewValue(value, name);
        try {
            long parsed = Long.parseLong(required);
            if (parsed <= 0) {
                throw invalid(name + " must be positive");
            }
            return Long.toString(parsed);
        } catch (NumberFormatException ex) {
            throw invalid(name + " must be a positive whole number");
        }
    }

    private String normalizePositiveInteger(String value, String name) {
        String required = requireNewValue(value, name);
        try {
            int parsed = Integer.parseInt(required);
            if (parsed <= 0) {
                throw invalid(name + " must be positive");
            }
            return Integer.toString(parsed);
        } catch (NumberFormatException ex) {
            throw invalid(name + " must be a positive whole number");
        }
    }

    private String normalizeAdministrationTime(String value) {
        String required = requireNewValue(value, "Administration time");
        try {
            Instant parsed = Instant.parse(required);
            if (parsed.isAfter(clock.instant())) {
                throw invalid("Administration time cannot be in the future");
            }
            return parsed.toString();
        } catch (DateTimeParseException ex) {
            throw invalid("Administration time must be an ISO-8601 instant");
        }
    }

    private String normalizeOptionalDate(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDate.parse(normalized).toString();
        } catch (DateTimeParseException ex) {
            throw invalid("Next dose date must be an ISO-8601 date");
        }
    }

    private String normalizeOptionalNotes(String value) {
        String normalized = normalizeNullable(value);
        if (normalized != null && normalized.length() > 2000) {
            throw invalid("Authorized notes must not exceed 2000 characters");
        }
        return normalized;
    }

    private String requireNewValue(String value, String name) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw invalid(name + " is required");
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String valueOrNull(Object value) {
        return value == null ? null : value.toString();
    }

    private InvalidVaccinationCorrectionException invalid(String message) {
        return new InvalidVaccinationCorrectionException(message);
    }
}
