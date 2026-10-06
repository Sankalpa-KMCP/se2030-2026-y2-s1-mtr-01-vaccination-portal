package com.se2030.vaccinationportal.vaccination.certificate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CertificateImpactPolicyTests {

    private final CertificateImpactPolicy policy = new CertificateImpactPolicy();

    @Test
    void policyClassifiesSupportedCorrectionFieldsFromCanonicalPublicData() {
        assertTrue(policy.isCertificateImpacting(VaccinationCorrectionField.VACCINE_BATCH_ID));
        assertTrue(policy.isCertificateImpacting(VaccinationCorrectionField.DOSE_NUMBER));
        assertTrue(policy.isCertificateImpacting(VaccinationCorrectionField.ADMINISTRATION_TIME));
        assertFalse(policy.isCertificateImpacting(VaccinationCorrectionField.NEXT_DOSE_DATE));
        assertFalse(policy.isCertificateImpacting(VaccinationCorrectionField.AUTHORIZED_NOTES));
    }

    @Test
    void visibleComparisonIgnoresNotesAndNextDoseDate() {
        EffectiveVaccinationRecordSnapshot before = snapshot(56L, 1, "2026-09-01T08:00:00Z", "Note");
        EffectiveVaccinationRecordSnapshot after = new EffectiveVaccinationRecordSnapshot(
                before.vaccinationRecordId(), before.appointmentId(), before.citizenId(), before.campaignId(),
                before.centerId(), before.vaccinatorId(), before.vaccineBatchId(), before.doseNumber(),
                before.administrationTime(), LocalDate.parse("2026-11-01"), "Changed note", before.finalizedAt()
        );

        assertFalse(policy.hasCertificateVisibleChange(before, after));
    }

    @Test
    void visibleComparisonDetectsBatchDoseAndAdministrationChanges() {
        EffectiveVaccinationRecordSnapshot baseline = snapshot(56L, 1, "2026-09-01T08:00:00Z", "Note");

        assertTrue(policy.hasCertificateVisibleChange(
                baseline,
                snapshot(78L, 1, "2026-09-01T08:00:00Z", "Note")
        ));
        assertTrue(policy.hasCertificateVisibleChange(
                baseline,
                snapshot(56L, 2, "2026-09-01T08:00:00Z", "Note")
        ));
        assertTrue(policy.hasCertificateVisibleChange(
                baseline,
                snapshot(56L, 1, "2026-09-01T09:00:00Z", "Note")
        ));
    }

    private EffectiveVaccinationRecordSnapshot snapshot(
            Long batchId,
            Integer doseNumber,
            String administeredAt,
            String notes
    ) {
        return new EffectiveVaccinationRecordSnapshot(
                101L, 1001L, 2001L, 3001L, 4001L, 91L, batchId, doseNumber,
                Instant.parse(administeredAt), LocalDate.parse("2026-10-01"), notes,
                Instant.parse("2026-09-01T08:05:00Z")
        );
    }
}
