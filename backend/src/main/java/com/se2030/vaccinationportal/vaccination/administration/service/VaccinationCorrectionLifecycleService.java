package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;


import com.se2030.vaccinationportal.vaccination.certificate.CertificateImpactPolicy;
import com.se2030.vaccinationportal.vaccination.administration.dto.ApplyVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.administration.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.administration.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus;
import com.se2030.vaccinationportal.vaccination.certificate.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.certificate.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationCorrectionValueCodec;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically applies an append-only correction and coordinates any required certificate reissue. */
@Service
public class VaccinationCorrectionLifecycleService {

    private final EffectiveVaccinationRecordService effectiveRecordService;
    private final VaccinationCorrectionService correctionService;
    private final CertificateImpactPolicy certificateImpactPolicy;
    private final DigitalCertificateService certificateService;
    private final VaccinationCorrectionValueCodec valueCodec;

    public VaccinationCorrectionLifecycleService(
            EffectiveVaccinationRecordService effectiveRecordService,
            VaccinationCorrectionService correctionService,
            CertificateImpactPolicy certificateImpactPolicy,
            DigitalCertificateService certificateService
    ) {
        this(effectiveRecordService, correctionService, certificateImpactPolicy, certificateService, new VaccinationCorrectionValueCodec(Clock.systemUTC()));
    }

    @Autowired
    public VaccinationCorrectionLifecycleService(
            EffectiveVaccinationRecordService effectiveRecordService,
            VaccinationCorrectionService correctionService,
            CertificateImpactPolicy certificateImpactPolicy,
            DigitalCertificateService certificateService,
            VaccinationCorrectionValueCodec valueCodec
    ) {
        this.effectiveRecordService = effectiveRecordService;
        this.correctionService = correctionService;
        this.certificateImpactPolicy = certificateImpactPolicy;
        this.certificateService = certificateService;
        this.valueCodec = valueCodec;
    }

    @Transactional
    public VaccinationCorrectionLifecycleResponse applyApprovedCorrection(
            Long vaccinationRecordId,
            Long correctionId,
            Long actorId
    ) {
        return applyApprovedCorrection(vaccinationRecordId, correctionId, actorId, null);
    }

    @Transactional
    public VaccinationCorrectionLifecycleResponse applyApprovedCorrection(
            Long vaccinationRecordId,
            Long correctionId,
            Long actorId,
            ApplyVaccinationCorrectionRequest applyRequest
    ) {
        VaccinationCorrection correction = correctionService.getCorrectionForUpdate(vaccinationRecordId, correctionId);
        if (correction.getStatus() != com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus.APPROVED) {
            throw new com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException(
                    "Only APPROVED corrections can be applied; current status is: " + correction.getStatus()
            );
        }

        EffectiveVaccinationRecordSnapshot beforeSnapshot =
                effectiveRecordService.getEffectiveRecord(vaccinationRecordId);

        if (applyRequest != null && applyRequest.fieldName() != null && !applyRequest.fieldName().trim().isEmpty()) {
            String requestedField = applyRequest.fieldName().trim();
            if ("vaccineBatchId".equalsIgnoreCase(requestedField) || "VACCINE_BATCH_ID".equalsIgnoreCase(requestedField)) {
                throw new UnsupportedVaccinationCorrectionFieldException(
                        "Vaccine batch corrections are not supported through the vaccination correction workflow because batch changes require inventory reconciliation."
                );
            }
            VaccinationCorrectionField field = VaccinationCorrectionField.fromApiName(requestedField);
            if (field == null || !field.isSupportedForCorrection()) {
                throw new UnsupportedVaccinationCorrectionFieldException(
                        "Unsupported vaccination correction field: " + applyRequest.fieldName()
                );
            }
            String effectiveOldValue = valueCodec.valueFrom(beforeSnapshot, field);
            String newValue = valueCodec.normalizeRequestedValue(field, applyRequest.newValue());
            correction.updateCorrection(field, effectiveOldValue, newValue);
        }

        if (correction.getFieldName() == VaccinationCorrectionField.VACCINE_BATCH_ID) {
            throw new UnsupportedVaccinationCorrectionFieldException(
                    "Vaccine batch corrections are not supported through the vaccination correction workflow because batch changes require inventory reconciliation."
            );
        }

        correction.apply();
        EffectiveVaccinationRecordSnapshot afterSnapshot =
                effectiveRecordService.getEffectiveRecord(vaccinationRecordId);

        boolean certificateImpacted =
                certificateImpactPolicy.hasCertificateVisibleChange(beforeSnapshot, afterSnapshot);
        if (!certificateImpacted || !certificateService.hasCurrentValidCertificate(vaccinationRecordId)) {
            return response(correction, certificateImpacted, null);
        }

        CertificateReissueResponse reissue = certificateService.reissueCertificate(
                vaccinationRecordId,
                new ReissueDigitalCertificateRequest(
                        correction.getReason(),
                        actorId != null ? actorId : correction.getCorrectedBy()
                )
        );
        return response(correction, true, reissue);
    }

    private VaccinationCorrectionLifecycleResponse response(
            VaccinationCorrection correction,
            boolean certificateImpacted,
            CertificateReissueResponse certificateReissue
    ) {
        return new VaccinationCorrectionLifecycleResponse(
                VaccinationCorrectionMapper.toResponse(correction),
                certificateImpacted,
                certificateReissue != null,
                certificateReissue
        );
    }
}
