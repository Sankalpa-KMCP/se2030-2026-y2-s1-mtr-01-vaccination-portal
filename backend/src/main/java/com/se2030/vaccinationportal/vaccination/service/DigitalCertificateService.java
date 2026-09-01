package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.vaccination.exception.DigitalCertificateNotFoundException;
import com.se2030.vaccinationportal.vaccination.exception.DuplicateDigitalCertificateException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import java.time.Clock;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for independent digital certificate issuance and retrieval.
 */
@Service
@Transactional
public class DigitalCertificateService {

    private static final int UNIQUE_IDENTIFIER_ATTEMPTS = 10;

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final DigitalCertificateRepository certificateRepository;
    private final Clock clock;

    public DigitalCertificateService(
            VaccinationRecordRepository vaccinationRecordRepository,
            DigitalCertificateRepository certificateRepository,
            Clock clock
    ) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.certificateRepository = certificateRepository;
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

        DigitalCertificate certificate = new DigitalCertificate(
                vaccinationRecord.getId(),
                generateUniqueCertificateReference(),
                generateUniqueVerificationToken(),
                clock.instant()
        );

        try {
            return certificateRepository.saveAndFlush(certificate);
        } catch (DataIntegrityViolationException ex) {
            throw duplicateForRecord(vaccinationRecordId, ex);
        }
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

    private DuplicateDigitalCertificateException duplicateForRecord(Long vaccinationRecordId, Throwable cause) {
        String message = "A digital certificate already exists for vaccination record: " + vaccinationRecordId;
        return cause == null
                ? new DuplicateDigitalCertificateException(message)
                : new DuplicateDigitalCertificateException(message, cause);
    }
}
