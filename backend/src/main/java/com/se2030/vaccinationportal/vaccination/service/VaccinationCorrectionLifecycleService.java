package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.vaccination.certificate.CertificateImpactPolicy;
import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically applies an append-only correction and coordinates any required certificate reissue. */
@Service
public class VaccinationCorrectionLifecycleService {

    private final EffectiveVaccinationRecordService effectiveRecordService;
    private final VaccinationCorrectionService correctionService;
    private final CertificateImpactPolicy certificateImpactPolicy;
    private final DigitalCertificateService certificateService;

    public VaccinationCorrectionLifecycleService(
            EffectiveVaccinationRecordService effectiveRecordService,
            VaccinationCorrectionService correctionService,
            CertificateImpactPolicy certificateImpactPolicy,
            DigitalCertificateService certificateService
    ) {
        this.effectiveRecordService = effectiveRecordService;
        this.correctionService = correctionService;
        this.certificateImpactPolicy = certificateImpactPolicy;
        this.certificateService = certificateService;
    }

    @Transactional
    public VaccinationCorrectionLifecycleResponse applyApprovedCorrection(
            Long vaccinationRecordId,
            Long correctionId,
            Long actorId
    ) {
        VaccinationCorrection correction = correctionService.getCorrectionById(vaccinationRecordId, correctionId);
        if (correction.getStatus() != com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.APPROVED) {
            throw new com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationCorrectionException(
                    "Only APPROVED corrections can be applied; current status is: " + correction.getStatus()
            );
        }

        EffectiveVaccinationRecordSnapshot beforeSnapshot =
                effectiveRecordService.getEffectiveRecord(vaccinationRecordId);
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

    @Transactional
    public VaccinationCorrectionLifecycleResponse applyCorrection(
            Long vaccinationRecordId,
            CreateVaccinationCorrectionRequest request
    ) {
        EffectiveVaccinationRecordSnapshot beforeSnapshot =
                effectiveRecordService.getEffectiveRecord(vaccinationRecordId);
        VaccinationCorrection correction = correctionService.createCorrection(vaccinationRecordId, request);
        if (correction.getStatus() == com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.PENDING) {
            Long approverId = request != null && request.correctedBy() != null ? request.correctedBy() + 1000L : 9999L;
            correction.approve(approverId, java.time.Instant.now(), "Direct application");
        }
        if (correction.getStatus() == com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.APPROVED) {
            correction.apply();
        }
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
                        correction.getCorrectedBy()
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
