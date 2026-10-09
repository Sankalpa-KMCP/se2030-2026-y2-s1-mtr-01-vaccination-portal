package com.se2030.vaccinationportal.vaccination.administration.strategy;

import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionType;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationRecord;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Non-clinical issue-reporting policy for citizen certificate requests. */
@Component
public class CitizenCertificateIssueStrategy implements CorrectionRequestStrategy {

    private static final Set<VaccinationCorrectionField> CITIZEN_ALLOWED_PROPOSAL_FIELDS = EnumSet.of(
            VaccinationCorrectionField.AUTHORIZED_NOTES
    );

    @Override
    public boolean supports(VaccinationCorrectionType type) {
        return type == VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE;
    }

    @Override
    public CreateVaccinationCorrectionRequest prepareCreate(
            VaccinationRecord record,
            CreateVaccinationCorrectionRequest request,
            Long actorId
    ) {
        if (record.getCitizenId() == null || !record.getCitizenId().equals(actorId)) {
            throw new AccessDeniedException(
                    "Citizens may only request corrections for their own vaccination records"
            );
        }

        String fieldName = request != null ? trimToNull(request.fieldName()) : null;
        if (fieldName != null) {
            VaccinationCorrectionField field = VaccinationCorrectionField.fromApiName(fieldName);
            if (field == null) {
                throw new UnsupportedVaccinationCorrectionFieldException(
                        "Unsupported vaccination correction field: " + request.fieldName()
                );
            }
            if (!CITIZEN_ALLOWED_PROPOSAL_FIELDS.contains(field)) {
                throw new InvalidVaccinationCorrectionException(
                        "Citizens may only request corrections for certificate display issues, not clinical vaccination data or inventory batches"
                );
            }
        }

        String reason = request != null ? request.reason() : null;
        String category = request != null ? trimToNull(request.category()) : null;
        if (category != null && reason != null && !reason.startsWith("[")) {
            reason = "[" + category + "] " + reason;
        }

        return new CreateVaccinationCorrectionRequest(
                fieldName != null ? fieldName : VaccinationCorrectionField.AUTHORIZED_NOTES.getApiName(),
                request != null ? request.newValue() : null,
                reason,
                actorId,
                category,
                VaccinationCorrectionType.CITIZEN_CERTIFICATE_ISSUE.name()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
