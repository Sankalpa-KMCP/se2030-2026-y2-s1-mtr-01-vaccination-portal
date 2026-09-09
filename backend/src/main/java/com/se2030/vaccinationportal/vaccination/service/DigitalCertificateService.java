package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.DigitalCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.ReissueDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.dto.RevokeDigitalCertificateRequest;
import com.se2030.vaccinationportal.vaccination.exception.CertificateAlreadyRevokedException;
import com.se2030.vaccinationportal.vaccination.exception.CertificateLifecycleConflictException;
import com.se2030.vaccinationportal.vaccination.exception.CertificateVerificationNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateVerificationTokenException;
import com.se2030.vaccinationportal.vaccination.exception.InvalidCertificateLifecycleException;
import com.se2030.vaccinationportal.vaccination.exception.NoValidDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.mapper.DigitalCertificateMapper;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for digital certificate issuance, retrieval, verification, and lifecycle transitions.
 */
@Service
@Transactional
public class DigitalCertificateService {

    private static final int UNIQUE_IDENTIFIER_ATTEMPTS = 10;

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final DigitalCertificateRepository certificateRepository;
    private final EffectiveVaccinationRecordService effectiveRecordService;
    private final Clock clock;

    public DigitalCertificateService(
            VaccinationRecordRepository vaccinationRecordRepository,
            DigitalCertificateRepository certificateRepository,
            EffectiveVaccinationRecordService effectiveRecordService,
            Clock clock
    ) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.certificateRepository = certificateRepository;
        this.effectiveRecordService = effectiveRecordService;
        this.clock = clock;
    }

    public DigitalCertificate issueCertificateForVaccinationRecord(Long vaccinationRecordId) {
        VaccinationRecord vaccinationRecord = vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));

        if (certificateRepository.existsByVaccinationRecordId(vaccinationRecordId)) {
            throw duplicateForRecord(vaccinationRecordId, null);
        }

        EffectiveVaccinationRecordSnapshot effectiveRecord = effectiveRecordService.resolve(vaccinationRecord);
        DigitalCertificate certificate = createCertificate(effectiveRecord, null);

        try {
            return certificateRepository.saveAndFlush(certificate);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateForRecord(vaccinationRecordId, ex);
        }
    }

    public DigitalCertificate revokeCertificate(
            Long certificateId,
            RevokeDigitalCertificateRequest request
    ) {
        LifecycleRequest lifecycleRequest = validateLifecycleRequest(
                request == null ? null : request.reason(),
                request == null ? null : request.revokedBy(),
                "Revoked-by"
        );
        DigitalCertificate certificate = certificateRepository.findByIdForUpdate(certificateId)
                .orElseThrow(() -> new DigitalCertificateNotFoundException(
                        "Digital certificate not found with ID: " + certificateId
                ));
        if (certificate.getStatus() == CertificateStatus.REVOKED) {
            throw new CertificateAlreadyRevokedException(
                    "Digital certificate is already revoked with ID: " + certificateId
            );
        }
        if (certificate.getStatus() == CertificateStatus.SUPERSEDED) {
            throw new CertificateLifecycleConflictException(
                    "A superseded digital certificate cannot be revoked: " + certificateId
            );
        }

        certificate.revoke(clock.instant(), lifecycleRequest.actorId(), lifecycleRequest.reason());
        return certificateRepository.saveAndFlush(certificate);
    }

    public CertificateReissueResponse reissueCertificate(
            Long vaccinationRecordId,
            ReissueDigitalCertificateRequest request
    ) {
        VaccinationRecord record = vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
        LifecycleRequest lifecycleRequest = validateLifecycleRequest(
                request == null ? null : request.reason(),
                request == null ? null : request.reissuedBy(),
                "Reissued-by"
        );

        List<DigitalCertificate> validCertificates =
                certificateRepository.findCurrentByVaccinationRecordIdAndStatusForUpdate(
                        record.getId(),
                        CertificateStatus.VALID
                );
        if (validCertificates.isEmpty()) {
            throw new NoValidDigitalCertificateException(
                    "No valid digital certificate is available for vaccination record: " + vaccinationRecordId
            );
        }
        if (validCertificates.size() > 1) {
            throw new CertificateLifecycleConflictException(
                    "Multiple valid digital certificates exist for vaccination record: " + vaccinationRecordId
            );
        }

        DigitalCertificate previousCertificate = validCertificates.get(0);
        EffectiveVaccinationRecordSnapshot effectiveRecord = effectiveRecordService.resolve(record);
        previousCertificate.supersede(
                clock.instant(),
                lifecycleRequest.actorId(),
                lifecycleRequest.reason()
        );
        certificateRepository.saveAndFlush(previousCertificate);

        DigitalCertificate replacement = createCertificate(effectiveRecord, previousCertificate.getId());
        try {
            replacement = certificateRepository.saveAndFlush(replacement);
            previousCertificate.markSupersededBy(replacement.getId());
            certificateRepository.saveAndFlush(previousCertificate);
        } catch (DataIntegrityViolationException ex) {
            throw new CertificateLifecycleConflictException(
                    "Unable to reissue the digital certificate because persisted lifecycle state changed"
            );
        }

        return new CertificateReissueResponse(
                DigitalCertificateMapper.toResponse(previousCertificate),
                DigitalCertificateMapper.toResponse(replacement)
        );
    }

    @Transactional(readOnly = true)
    public List<DigitalCertificateResponse> getCertificateHistory(Long vaccinationRecordId) {
        vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
        return certificateRepository
                .findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(vaccinationRecordId)
                .stream()
                .map(DigitalCertificateMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DigitalCertificate getCertificateById(Long certificateId) {
        return certificateRepository.findById(certificateId)
                .orElseThrow(() -> new DigitalCertificateNotFoundException(
                        "Digital certificate not found with ID: " + certificateId
                ));
    }

    @Transactional(readOnly = true)
    public DigitalCertificate getCertificateByReference(String certificateReference) {
        return certificateRepository.findByCertificateReference(certificateReference)
                .orElseThrow(() -> new DigitalCertificateNotFoundException(
                        "Digital certificate not found with reference: " + certificateReference
                ));
    }

    @Transactional(readOnly = true)
    public CertificateVerificationResponse verifyCertificate(String verificationToken) {
        if (verificationToken == null || verificationToken.isBlank()) {
            throw new InvalidCertificateVerificationTokenException("Certificate verification token is required");
        }

        String normalizedToken = verificationToken.trim();
        DigitalCertificate certificate = certificateRepository.findByVerificationToken(normalizedToken)
                .orElseThrow(() -> new CertificateVerificationNotFoundException(
                        "No digital certificate was found for the supplied verification token"
                ));
        return DigitalCertificateMapper.toVerificationResponse(certificate);
    }

    private String generateUniqueCertificateReference() {
        for (int attempt = 0; attempt < UNIQUE_IDENTIFIER_ATTEMPTS; attempt++) {
            String candidate = "CERT-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
            if (certificateRepository.findByCertificateReference(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique certificate reference");
    }

    private String generateUniqueVerificationToken() {
        for (int attempt = 0; attempt < UNIQUE_IDENTIFIER_ATTEMPTS; attempt++) {
            String candidate = UUID.randomUUID().toString().replace("-", "")
                    + UUID.randomUUID().toString().replace("-", "");
            if (certificateRepository.findByVerificationToken(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique certificate verification token");
    }

    private DigitalCertificate createCertificate(
            EffectiveVaccinationRecordSnapshot effectiveRecord,
            Long reissuedFromCertificateId
    ) {
        return new DigitalCertificate(
                effectiveRecord.vaccinationRecordId(),
                generateUniqueCertificateReference(),
                generateUniqueVerificationToken(),
                clock.instant(),
                reissuedFromCertificateId,
                effectiveRecord.vaccineBatchId(),
                effectiveRecord.doseNumber(),
                effectiveRecord.administrationTime()
        );
    }

    private LifecycleRequest validateLifecycleRequest(String reason, Long actorId, String actorLabel) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidCertificateLifecycleException("Certificate lifecycle reason is required");
        }
        String normalizedReason = reason.trim();
        if (normalizedReason.length() > 1000) {
            throw new InvalidCertificateLifecycleException(
                    "Certificate lifecycle reason must not exceed 1000 characters"
            );
        }
        if (actorId == null || actorId <= 0) {
            throw new InvalidCertificateLifecycleException(actorLabel + " ID must be positive");
        }
        return new LifecycleRequest(normalizedReason, actorId);
    }

    private record LifecycleRequest(String reason, Long actorId) {
    }

    private DuplicateDigitalCertificateException duplicateForRecord(Long vaccinationRecordId, Throwable cause) {
        String message = "A digital certificate already exists for vaccination record: " + vaccinationRecordId;
        return cause == null
                ? new DuplicateDigitalCertificateException(message)
                : new DuplicateDigitalCertificateException(message, cause);
    }
}
