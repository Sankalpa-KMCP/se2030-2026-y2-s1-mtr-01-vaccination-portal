package com.se2030.vaccinationportal.vaccination.administration.service;

import com.se2030.vaccinationportal.vaccination.certificate.service.DigitalCertificateService;
import com.se2030.vaccinationportal.vaccination.query.service.EffectiveVaccinationRecordService;


import com.se2030.vaccinationportal.vaccination.certificate.CertificateImpactPolicy;
import com.se2030.vaccinationportal.vaccination.certificate.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.administration.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.certificate.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.administration.dto.VaccinationCorrectionLifecycleResponse;
import com.se2030.vaccinationportal.vaccination.administration.mapper.VaccinationCorrectionMapper;
import com.se2030.vaccinationportal.vaccination.query.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrection;
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
        if (correction.getStatus() != com.se2030.vaccinationportal.vaccination.administration.model.VaccinationCorrectionStatus.APPROVED) {
            throw new com.se2030.vaccinationportal.vaccination.administration.exception.InvalidVaccinationCorrectionException(
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
