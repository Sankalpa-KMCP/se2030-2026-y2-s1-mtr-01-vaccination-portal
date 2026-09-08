package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for append-only corrections to finalized vaccination history. */
@Service
@Transactional
public class VaccinationCorrectionService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final VaccinationCorrectionRepository correctionRepository;
    private final Clock clock;

    public VaccinationCorrectionService(
            VaccinationRecordRepository vaccinationRecordRepository,
            VaccinationCorrectionRepository correctionRepository,
            Clock clock
    ) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.correctionRepository = correctionRepository;
        this.clock = clock;
    }

    public VaccinationCorrection createCorrection(
            Long vaccinationRecordId,
            CreateVaccinationCorrectionRequest request
    ) {
        VaccinationRecord record = findRecord(vaccinationRecordId);
        validateRequest(request);

        VaccinationCorrectionField field = VaccinationCorrectionField.fromApiName(request.fieldName());
        if (field == null) {
            throw new UnsupportedVaccinationCorrectionFieldException(
                    "Unsupported vaccination correction field: " + request.fieldName()
            );
        }

        String originalValue = originalValue(record, field);
        var latestCorrection = correctionRepository
                .findFirstByVaccinationRecordIdAndFieldNameOrderByCorrectedAtDescIdDesc(
                        record.getId(),
                        field
                );
        String effectiveOldValue = latestCorrection.isPresent()
                ? latestCorrection.get().getNewValue()
                : originalValue;
        String newValue = normalizeNewValue(field, request.newValue());
        if (Objects.equals(effectiveOldValue, newValue)) {
            throw new InvalidVaccinationCorrectionException(
                    "Corrected value must differ from the current effective value"
            );
        }

        VaccinationCorrection correction = VaccinationCorrectionMapper.toEntity(
                record.getId(),
                field,
                effectiveOldValue,
                newValue,
                request,
                clock.instant()
        );
        return correctionRepository.save(correction);
    }

    @Transactional(readOnly = true)
    public List<VaccinationCorrection> getCorrectionHistory(Long vaccinationRecordId) {
        VaccinationRecord record = findRecord(vaccinationRecordId);
        return correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(record.getId());
    }

    private VaccinationRecord findRecord(Long vaccinationRecordId) {
        return vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
    }

    private void validateRequest(CreateVaccinationCorrectionRequest request) {
        if (request == null) {
            throw new InvalidVaccinationCorrectionException("Correction request is required");
        }
        if (request.reason() == null || request.reason().trim().isEmpty()) {
            throw new InvalidVaccinationCorrectionException("Correction reason is required");
        }
        if (request.reason().trim().length() > 1000) {
            throw new InvalidVaccinationCorrectionException(
                    "Correction reason must not exceed 1000 characters"
            );
        }
        if (request.correctedBy() == null || request.correctedBy() <= 0) {
            throw new InvalidVaccinationCorrectionException("Corrected-by ID must be positive");
        }
    }

    private String originalValue(VaccinationRecord record, VaccinationCorrectionField field) {
        return switch (field) {
            case VACCINE_BATCH_ID -> record.getVaccineBatchId().toString();
            case DOSE_NUMBER -> record.getDoseNumber().toString();
            case ADMINISTRATION_TIME -> record.getAdministrationTime().toString();
            case NEXT_DOSE_DATE -> valueOrNull(record.getNextDoseDate());
            case AUTHORIZED_NOTES -> record.getAuthorizedNotes();
        };
    }

    private String normalizeNewValue(VaccinationCorrectionField field, String value) {
        return switch (field) {
            case VACCINE_BATCH_ID -> normalizePositiveLong(value, "Vaccine batch ID");
            case DOSE_NUMBER -> normalizePositiveInteger(value, "Dose number");
            case ADMINISTRATION_TIME -> normalizeAdministrationTime(value);
            case NEXT_DOSE_DATE -> normalizeOptionalDate(value);
            case AUTHORIZED_NOTES -> normalizeOptionalNotes(value);
        };
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
