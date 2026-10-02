package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.vaccination.dto.CertificateVerificationResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateReissueResponse;
import com.se2030.vaccinationportal.vaccination.dto.CitizenCertificateResponse;
import com.se2030.vaccinationportal.vaccination.dto.CitizenVaccinationRecordResponse;
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
import com.se2030.vaccinationportal.shared.auth.security.UserPrincipal;
import java.time.Clock;
import com.se2030.vaccinationportal.campaign.model.VaccinationCampaign;
import com.se2030.vaccinationportal.campaign.model.VaccinationCenter;
import com.se2030.vaccinationportal.campaign.model.VaccineType;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCampaignRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccinationCenterRepository;
import com.se2030.vaccinationportal.campaign.repository.VaccineTypeRepository;
import com.se2030.vaccinationportal.inventory.model.BatchStatus;
import com.se2030.vaccinationportal.inventory.model.CenterInventory;
import com.se2030.vaccinationportal.inventory.model.VaccineBatch;
import com.se2030.vaccinationportal.inventory.repository.CenterInventoryRepository;
import com.se2030.vaccinationportal.inventory.repository.VaccineBatchRepository;
import com.se2030.vaccinationportal.shared.auth.model.UserAccount;
import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationPort;
import com.se2030.vaccinationportal.vaccination.dependency.AppointmentValidationSnapshot;
import com.se2030.vaccinationportal.vaccination.dto.AvailableVaccineBatchResponse;
import com.se2030.vaccinationportal.vaccination.dto.CertificateDisplayDetails;
import com.se2030.vaccinationportal.vaccination.dto.VaccinatorVaccinationRecordResponse;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationAppointmentNotFoundException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
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
    private final CertificatePdfService certificatePdfService;

    @Autowired(required = false)
    private VaccineBatchRepository vaccineBatchRepository;
    @Autowired(required = false)
    private CenterInventoryRepository centerInventoryRepository;
    @Autowired(required = false)
    private VaccineTypeRepository vaccineTypeRepository;
    @Autowired(required = false)
    private VaccinationCenterRepository vaccinationCenterRepository;
    @Autowired(required = false)
    private VaccinationCampaignRepository vaccinationCampaignRepository;
    @Autowired(required = false)
    private UserAccountRepository userAccountRepository;
    @Autowired(required = false)
    private AppointmentValidationPort appointmentValidationPort;

    public DigitalCertificateService(
            VaccinationRecordRepository vaccinationRecordRepository,
            DigitalCertificateRepository certificateRepository,
            EffectiveVaccinationRecordService effectiveRecordService,
            Clock clock
    ) {
        this(vaccinationRecordRepository, certificateRepository, effectiveRecordService, clock, new CertificatePdfService());
    }

    @Autowired
    public DigitalCertificateService(
            VaccinationRecordRepository vaccinationRecordRepository,
            DigitalCertificateRepository certificateRepository,
            EffectiveVaccinationRecordService effectiveRecordService,
            Clock clock,
            CertificatePdfService certificatePdfService
    ) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.certificateRepository = certificateRepository;
        this.effectiveRecordService = effectiveRecordService;
        this.clock = clock;
        this.certificatePdfService = certificatePdfService != null ? certificatePdfService : new CertificatePdfService();
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

    @Transactional(readOnly = true)
    public boolean hasCurrentValidCertificate(Long vaccinationRecordId) {
        return certificateRepository.existsByVaccinationRecordIdAndStatus(
                vaccinationRecordId,
                CertificateStatus.VALID
        );
    }

    public DigitalCertificate revokeCertificate(
            Long certificateId,
            String reason,
            Long actorId
    ) {
        LifecycleRequest lifecycleRequest = validateLifecycleRequest(
                reason,
                actorId,
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

    public DigitalCertificate revokeCertificate(
            Long certificateId,
            RevokeDigitalCertificateRequest request
    ) {
        return revokeCertificate(
                certificateId,
                request == null ? null : request.reason(),
                request == null ? null : request.revokedBy()
        );
    }

    public CertificateReissueResponse reissueCertificate(
            Long vaccinationRecordId,
            String reason,
            Long actorId
    ) {
        VaccinationRecord record = vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
        LifecycleRequest lifecycleRequest = validateLifecycleRequest(
                reason,
                actorId,
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

    public CertificateReissueResponse reissueCertificate(
            Long vaccinationRecordId,
            ReissueDigitalCertificateRequest request
    ) {
        return reissueCertificate(
                vaccinationRecordId,
                request == null ? null : request.reason(),
                request == null ? null : request.reissuedBy()
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

    @Transactional(readOnly = true)
    public void assertCertificateAccess(DigitalCertificate certificate, UserPrincipal principal) {
        if (principal == null || isStaff(principal)) {
            return;
        }
        VaccinationRecord record = vaccinationRecordRepository.findById(certificate.getVaccinationRecordId())
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + certificate.getVaccinationRecordId()
                ));
        if (isVaccinator(principal)) {
            if (!record.getVaccinatorId().equals(principal.getUserId())) {
                throw new AccessDeniedException("Vaccinators may only access digital certificates for vaccinations they administered");
            }
            return;
        }
        if (!record.getCitizenId().equals(principal.getUserId())) {
            throw new AccessDeniedException("Citizens may only access their own digital certificates");
        }
    }

    @Transactional(readOnly = true)
    public void assertRecordAccess(Long vaccinationRecordId, UserPrincipal principal) {
        if (principal == null || isStaff(principal)) {
            return;
        }
        VaccinationRecord record = vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
        if (isVaccinator(principal)) {
            if (!record.getVaccinatorId().equals(principal.getUserId())) {
                throw new AccessDeniedException("Vaccinators may only access records for vaccinations they administered");
            }
            return;
        }
        if (!record.getCitizenId().equals(principal.getUserId())) {
            throw new AccessDeniedException("Citizens may only access certificate history for their own vaccination records");
        }
    }

    @Transactional(readOnly = true)
    public List<VaccinatorVaccinationRecordResponse> getVaccinatorVaccinationHistory(Long vaccinatorId) {
        if (vaccinatorId == null || vaccinatorId <= 0) {
            throw new IllegalArgumentException("Vaccinator ID must be positive");
        }
        List<VaccinationRecord> records = vaccinationRecordRepository.findByVaccinatorIdOrderByAdministrationTimeDesc(vaccinatorId);
        return records.stream().map(this::toVaccinatorRecordResponse).toList();
    }

    @Transactional(readOnly = true)
    public VaccinatorVaccinationRecordResponse getVaccinationRecordDetails(Long recordId) {
        VaccinationRecord record = vaccinationRecordRepository.findById(recordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + recordId
                ));
        return toVaccinatorRecordResponse(record);
    }

    private VaccinatorVaccinationRecordResponse toVaccinatorRecordResponse(VaccinationRecord record) {
        var validCert = certificateRepository.findFirstByVaccinationRecordIdAndStatusOrderByIssuedAtDescIdDesc(
                record.getId(), CertificateStatus.VALID
        );
        var fallbackCert = certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId())
                .stream()
                .reduce((first, second) -> second);
        var certOpt = validCert.isPresent() ? validCert : fallbackCert;

        Long certId = certOpt.map(DigitalCertificate::getId).orElse(null);
        String certRef = certOpt.map(DigitalCertificate::getCertificateReference).orElse(null);
        CertificateStatus status = certOpt.map(DigitalCertificate::getStatus).orElse(null);

        String citizenName = resolveCitizenName(record.getCitizenId());
        String campaignName = resolveCampaignName(record.getCampaignId());
        String centerName = resolveCenterName(record.getCenterId());
        String batchNumber = resolveBatchNumber(record.getVaccineBatchId());
        String vaccineName = resolveVaccineName(record.getVaccineBatchId());

        return new VaccinatorVaccinationRecordResponse(
                record.getId(),
                record.getAppointmentId(),
                record.getCitizenId(),
                citizenName,
                record.getCampaignId(),
                campaignName,
                record.getCenterId(),
                centerName,
                record.getVaccineBatchId(),
                batchNumber,
                vaccineName,
                record.getDoseNumber(),
                record.getAdministrationTime(),
                record.getNextDoseDate(),
                record.getAuthorizedNotes(),
                record.getFinalizedAt(),
                certId,
                certRef,
                status
        );
    }

    @Transactional(readOnly = true)
    public List<AvailableVaccineBatchResponse> getAvailableBatchesForAppointment(Long appointmentId) {
        if (appointmentId == null || appointmentId <= 0) {
            throw new IllegalArgumentException("Appointment ID must be positive");
        }
        if (appointmentValidationPort == null) {
            return List.of();
        }
        AppointmentValidationSnapshot appointment = appointmentValidationPort.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new VaccinationAppointmentNotFoundException(
                        "Vaccination appointment not found with ID: " + appointmentId
                ));

        Long centerId = appointment.centerId();
        Long expectedVaccineTypeId = appointment.campaignVaccineTypeId();
        if (centerId == null || centerInventoryRepository == null || vaccineBatchRepository == null) {
            return List.of();
        }

        List<CenterInventory> inventories = centerInventoryRepository.findByCenterId(centerId);
        LocalDate today = LocalDate.now(clock != null ? clock : Clock.systemUTC());

        List<AvailableVaccineBatchResponse> results = new ArrayList<>();
        for (CenterInventory inv : inventories) {
            if (inv.getQuantity() <= 0) {
                continue;
            }
            VaccineBatch batch = vaccineBatchRepository.findById(inv.getBatchId()).orElse(null);
            if (batch == null || batch.getBatchStatus() != BatchStatus.ACTIVE) {
                continue;
            }
            if (batch.getExpiryDate() != null && batch.getExpiryDate().isBefore(today)) {
                continue;
            }
            if (expectedVaccineTypeId != null && !expectedVaccineTypeId.equals(batch.getVaccineTypeId())) {
                continue;
            }
            String vaccineName = "Standard Vaccine";
            if (vaccineTypeRepository != null && batch.getVaccineTypeId() != null) {
                vaccineName = vaccineTypeRepository.findById(batch.getVaccineTypeId())
                        .map(VaccineType::getName)
                        .orElse("Standard Vaccine");
            }
            results.add(new AvailableVaccineBatchResponse(
                    batch.getId(),
                    batch.getBatchNumber(),
                    vaccineName,
                    batch.getExpiryDate(),
                    inv.getQuantity()
            ));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<CitizenVaccinationRecordResponse> getCitizenVaccinationHistory(Long citizenId) {
        if (citizenId == null) {
            return List.of();
        }
        List<VaccinationRecord> records = vaccinationRecordRepository.findByCitizenIdOrderByAdministrationTimeDesc(citizenId);
        return records.stream().map(record -> {
            var validCert = certificateRepository.findFirstByVaccinationRecordIdAndStatusOrderByIssuedAtDescIdDesc(
                    record.getId(), CertificateStatus.VALID
            );
            var fallbackCert = certificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId())
                    .stream()
                    .reduce((first, second) -> second);
            var certOpt = validCert.isPresent() ? validCert : fallbackCert;

            Long certId = certOpt.map(DigitalCertificate::getId).orElse(null);
            String certRef = certOpt.map(DigitalCertificate::getCertificateReference).orElse(null);
            CertificateStatus status = certOpt.map(DigitalCertificate::getStatus).orElse(null);

            return new CitizenVaccinationRecordResponse(
                    record.getId(),
                    record.getAppointmentId(),
                    record.getCitizenId(),
                    record.getCampaignId(),
                    record.getCenterId(),
                    record.getVaccineBatchId(),
                    record.getDoseNumber(),
                    record.getAdministrationTime(),
                    record.getNextDoseDate(),
                    record.getAuthorizedNotes(),
                    record.getFinalizedAt(),
                    certId,
                    certRef,
                    status
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<CitizenCertificateResponse> getCitizenCertificates(Long citizenId) {
        if (citizenId == null) {
            return List.of();
        }
        List<DigitalCertificate> certificates = certificateRepository.findByCitizenIdOrderByIssuedAtDesc(citizenId);
        return certificates.stream().map(cert -> new CitizenCertificateResponse(
                cert.getId(),
                cert.getVaccinationRecordId(),
                cert.getCertificateReference(),
                cert.getIssuedAt(),
                cert.getStatus(),
                cert.getIssuedDoseNumber(),
                cert.getIssuedAdministrationTime(),
                cert.getIssuedVaccineBatchId(),
                cert.getVerificationToken(),
                cert.getRevokedAt(),
                cert.getRevocationReason(),
                cert.getReissuedFromCertificateId(),
                cert.getSupersededByCertificateId()
        )).toList();
    }

    @Transactional(readOnly = true)
    public byte[] generateCertificatePdf(Long certificateId, UserPrincipal principal) {
        DigitalCertificate certificate = getCertificateById(certificateId);
        assertCertificateAccess(certificate, principal);
        VaccinationRecord record = vaccinationRecordRepository.findById(certificate.getVaccinationRecordId())
                .orElse(null);
        CertificateDisplayDetails details = resolveDisplayDetails(certificate, record);
        return certificatePdfService.generateCertificatePdf(certificate, record, details);
    }

    private CertificateDisplayDetails resolveDisplayDetails(DigitalCertificate certificate, VaccinationRecord record) {
        if (record == null) {
            return new CertificateDisplayDetails(null, null, null, null);
        }
        String citizenName = resolveCitizenName(record.getCitizenId());
        String vaccineName = resolveVaccineName(record.getVaccineBatchId());
        String batchNumber = resolveBatchNumber(record.getVaccineBatchId());
        String centerName = resolveCenterName(record.getCenterId());
        return new CertificateDisplayDetails(citizenName, vaccineName, batchNumber, centerName);
    }

    private String resolveCitizenName(Long citizenId) {
        if (citizenId == null || userAccountRepository == null) {
            return "Citizen #" + citizenId;
        }
        return userAccountRepository.findById(citizenId)
                .map(user -> user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "Citizen #" + citizenId)
                .orElse("Citizen #" + citizenId);
    }

    private String resolveCenterName(Long centerId) {
        if (centerId == null || vaccinationCenterRepository == null) {
            return "Center #" + centerId;
        }
        return vaccinationCenterRepository.findById(centerId)
                .map(VaccinationCenter::getName)
                .orElse("Center #" + centerId);
    }

    private String resolveCampaignName(Long campaignId) {
        if (campaignId == null || vaccinationCampaignRepository == null) {
            return null;
        }
        return vaccinationCampaignRepository.findById(campaignId)
                .map(VaccinationCampaign::getName)
                .orElse(null);
    }

    private String resolveBatchNumber(Long batchId) {
        if (batchId == null || vaccineBatchRepository == null) {
            return "Batch #" + batchId;
        }
        return vaccineBatchRepository.findById(batchId)
                .map(VaccineBatch::getBatchNumber)
                .orElse("Batch #" + batchId);
    }

    private String resolveVaccineName(Long batchId) {
        if (batchId == null || vaccineBatchRepository == null || vaccineTypeRepository == null) {
            return "Authorized Vaccine";
        }
        return vaccineBatchRepository.findById(batchId)
                .flatMap(b -> vaccineTypeRepository.findById(b.getVaccineTypeId()))
                .map(VaccineType::getName)
                .orElse("Authorized Vaccine");
    }

    private boolean isVaccinator(UserPrincipal principal) {
        return principal != null && (principal.hasRole("ROLE_VACCINATOR") || principal.hasRole("VACCINATOR"));
    }

    private boolean isStaff(UserPrincipal principal) {
        return principal.hasRole("ROLE_ADMIN")
                || principal.hasRole("ADMIN");
    }

    @Autowired(required = false)
    public void setVaccineBatchRepository(VaccineBatchRepository vaccineBatchRepository) {
        this.vaccineBatchRepository = vaccineBatchRepository;
    }

    @Autowired(required = false)
    public void setCenterInventoryRepository(CenterInventoryRepository centerInventoryRepository) {
        this.centerInventoryRepository = centerInventoryRepository;
    }

    @Autowired(required = false)
    public void setVaccineTypeRepository(VaccineTypeRepository vaccineTypeRepository) {
        this.vaccineTypeRepository = vaccineTypeRepository;
    }

    @Autowired(required = false)
    public void setVaccinationCenterRepository(VaccinationCenterRepository vaccinationCenterRepository) {
        this.vaccinationCenterRepository = vaccinationCenterRepository;
    }

    @Autowired(required = false)
    public void setVaccinationCampaignRepository(VaccinationCampaignRepository vaccinationCampaignRepository) {
        this.vaccinationCampaignRepository = vaccinationCampaignRepository;
    }

    @Autowired(required = false)
    public void setUserAccountRepository(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Autowired(required = false)
    public void setAppointmentValidationPort(AppointmentValidationPort appointmentValidationPort) {
        this.appointmentValidationPort = appointmentValidationPort;
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
