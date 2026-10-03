package com.se2030.vaccinationportal.vaccination.service;

import com.se2030.vaccinationportal.shared.auth.repository.UserAccountRepository;
import com.se2030.vaccinationportal.vaccination.dto.CreateVaccinationCorrectionRequest;
import com.se2030.vaccinationportal.vaccination.dto.VaccinationCorrectionDetailResponse;
import com.se2030.vaccinationportal.vaccination.exception.InvalidVaccinationCorrectionException;
import com.se2030.vaccinationportal.vaccination.exception.UnsupportedVaccinationCorrectionFieldException;
import com.se2030.vaccinationportal.vaccination.exception.VaccinationRecordNotFoundException;
import com.se2030.vaccinationportal.vaccination.model.EffectiveVaccinationRecordSnapshot;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrection;
import com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionField;
import com.se2030.vaccinationportal.vaccination.model.VaccinationRecord;
import com.se2030.vaccinationportal.vaccination.repository.DigitalCertificateRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationCorrectionRepository;
import com.se2030.vaccinationportal.vaccination.repository.VaccinationRecordRepository;
import com.se2030.vaccinationportal.vaccination.validation.VaccinationCorrectionValueCodec;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for append-only corrections to finalized vaccination history. */
@Service
@Transactional
public class VaccinationCorrectionService {

    private final VaccinationRecordRepository vaccinationRecordRepository;
    private final VaccinationCorrectionRepository correctionRepository;
    private final EffectiveVaccinationRecordService effectiveRecordService;
    private final VaccinationCorrectionValueCodec valueCodec;
    private final Clock clock;
    private final DigitalCertificateRepository digitalCertificateRepository;
    private final UserAccountRepository userAccountRepository;

    public VaccinationCorrectionService(
            VaccinationRecordRepository vaccinationRecordRepository,
            VaccinationCorrectionRepository correctionRepository,
            EffectiveVaccinationRecordService effectiveRecordService,
            VaccinationCorrectionValueCodec valueCodec,
            Clock clock
    ) {
        this(vaccinationRecordRepository, correctionRepository, effectiveRecordService, valueCodec, clock, null, null);
    }

    @Autowired
    public VaccinationCorrectionService(
            VaccinationRecordRepository vaccinationRecordRepository,
            VaccinationCorrectionRepository correctionRepository,
            EffectiveVaccinationRecordService effectiveRecordService,
            VaccinationCorrectionValueCodec valueCodec,
            Clock clock,
            DigitalCertificateRepository digitalCertificateRepository,
            UserAccountRepository userAccountRepository
    ) {
        this.vaccinationRecordRepository = vaccinationRecordRepository;
        this.correctionRepository = correctionRepository;
        this.effectiveRecordService = effectiveRecordService;
        this.valueCodec = valueCodec;
        this.clock = clock != null ? clock : Clock.systemUTC();
        this.digitalCertificateRepository = digitalCertificateRepository;
        this.userAccountRepository = userAccountRepository;
    }

    public VaccinationCorrection createCorrection(
            Long vaccinationRecordId,
            CreateVaccinationCorrectionRequest request,
            Long actorId
    ) {
        VaccinationRecord record = findRecord(vaccinationRecordId);
        validateRequest(request, actorId);

        String fieldName = request.fieldName() != null && !request.fieldName().trim().isEmpty()
                ? request.fieldName().trim()
                : VaccinationCorrectionField.AUTHORIZED_NOTES.getApiName();

        if ("vaccineBatchId".equalsIgnoreCase(fieldName) || "VACCINE_BATCH_ID".equalsIgnoreCase(fieldName)) {
            throw new UnsupportedVaccinationCorrectionFieldException(
                    "Vaccine batch corrections are not supported through the vaccination correction workflow because batch changes require inventory reconciliation."
            );
        }

        VaccinationCorrectionField field = VaccinationCorrectionField.fromApiName(fieldName);
        if (field == null || !field.isSupportedForCorrection()) {
            throw new UnsupportedVaccinationCorrectionFieldException(
                    "Unsupported vaccination correction field: " + request.fieldName()
            );
        }

        EffectiveVaccinationRecordSnapshot effectiveRecord = effectiveRecordService.resolve(record);
        String effectiveOldValue = valueCodec.valueFrom(effectiveRecord, field);
        String newValue = null;
        if (request.newValue() != null && !request.newValue().trim().isEmpty()) {
            newValue = valueCodec.normalizeRequestedValue(field, request.newValue());
            if (Objects.equals(effectiveOldValue, newValue)) {
                throw new InvalidVaccinationCorrectionException(
                        "Corrected value must differ from the current effective value"
                );
            }
        }

        VaccinationCorrection correction = new VaccinationCorrection(
                record.getId(),
                field,
                effectiveOldValue,
                newValue,
                request.reason(),
                actorId,
                clock.instant(),
                com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.PENDING
        );
        return correctionRepository.save(correction);
    }

    public VaccinationCorrection createCorrection(
            Long vaccinationRecordId,
            CreateVaccinationCorrectionRequest request
    ) {
        Long actorId = request != null && request.correctedBy() != null ? request.correctedBy() : null;
        return createCorrection(vaccinationRecordId, request, actorId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveCorrection(Long vaccinationRecordId) {
        return correctionRepository.existsActiveByVaccinationRecordId(vaccinationRecordId);
    }

    @Transactional(readOnly = true)
    public VaccinationCorrection getCorrectionById(Long vaccinationRecordId, Long correctionId) {
        findRecord(vaccinationRecordId);
        return correctionRepository.findByIdAndVaccinationRecordId(correctionId, vaccinationRecordId)
                .orElseThrow(() -> new InvalidVaccinationCorrectionException(
                        "Vaccination correction not found with ID: " + correctionId + " for record: " + vaccinationRecordId
                ));
    }

    public VaccinationCorrection approveCorrection(
            Long vaccinationRecordId,
            Long correctionId,
            Long reviewerId,
            String reviewReason
    ) {
        if (reviewerId == null || reviewerId <= 0) {
            throw new InvalidVaccinationCorrectionException("Reviewer ID must be positive");
        }
        VaccinationCorrection correction = getCorrectionById(vaccinationRecordId, correctionId);
        if (Objects.equals(correction.getCorrectedBy(), reviewerId)) {
            throw new InvalidVaccinationCorrectionException("Submitter cannot approve their own correction");
        }
        if (correction.getStatus() != com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.PENDING) {
            throw new InvalidVaccinationCorrectionException(
                    "Only PENDING corrections can be approved; current status is: " + correction.getStatus()
            );
        }
        correction.approve(reviewerId, clock.instant(), reviewReason);
        return correctionRepository.save(correction);
    }

    public VaccinationCorrection rejectCorrection(
            Long vaccinationRecordId,
            Long correctionId,
            Long reviewerId,
            String rejectionReason
    ) {
        if (reviewerId == null || reviewerId <= 0) {
            throw new InvalidVaccinationCorrectionException("Reviewer ID must be positive");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new InvalidVaccinationCorrectionException("Rejection reason is required and cannot be blank");
        }
        if (rejectionReason.trim().length() > 1000) {
            throw new InvalidVaccinationCorrectionException("Rejection reason must not exceed 1000 characters");
        }
        VaccinationCorrection correction = getCorrectionById(vaccinationRecordId, correctionId);
        if (correction.getStatus() != com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.PENDING
                && correction.getStatus() != com.se2030.vaccinationportal.vaccination.model.VaccinationCorrectionStatus.APPROVED) {
            throw new InvalidVaccinationCorrectionException(
                    "Only PENDING or APPROVED corrections can be rejected; current status is: " + correction.getStatus()
            );
        }
        correction.reject(reviewerId, clock.instant(), rejectionReason);
        return correctionRepository.save(correction);
    }

    @Transactional(readOnly = true)
    public List<VaccinationCorrection> getCorrectionHistory(Long vaccinationRecordId) {
        VaccinationRecord record = findRecord(vaccinationRecordId);
        return correctionRepository.findByVaccinationRecordIdOrderByCorrectedAtAscIdAsc(record.getId());
    }

    @Transactional(readOnly = true)
    public List<VaccinationCorrectionDetailResponse> getAdminCorrectionRequests() {
        return correctionRepository.findAllByOrderByCorrectedAtDescIdDesc()
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VaccinationCorrectionDetailResponse> getVaccinatorCorrectionRequests(Long vaccinatorId) {
        if (vaccinatorId == null) {
            return List.of();
        }
        return correctionRepository.findByVaccinatorIdOrderByCorrectedAtDescIdDesc(vaccinatorId)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VaccinationCorrectionDetailResponse> getCitizenCorrectionRequests(Long citizenId) {
        if (citizenId == null) {
            return List.of();
        }
        return correctionRepository.findByCitizenIdOrderByCorrectedAtDescIdDesc(citizenId)
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    public VaccinationCorrectionDetailResponse toDetailResponse(VaccinationCorrection correction) {
        VaccinationRecord record = vaccinationRecordRepository.findById(correction.getVaccinationRecordId()).orElse(null);
        Long citizenId = record != null ? record.getCitizenId() : null;
        Long vaccinatorId = record != null ? record.getVaccinatorId() : null;

        String citizenName = resolveCitizenName(citizenId);
        String vaccinatorName = resolveUserName(vaccinatorId);

        String certRef = null;
        String certStatus = null;
        Long certId = null;

        if (digitalCertificateRepository != null && record != null) {
            var certs = digitalCertificateRepository.findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(record.getId());
            if (!certs.isEmpty()) {
                var latestCert = certs.get(certs.size() - 1);
                certRef = latestCert.getCertificateReference();
                certStatus = latestCert.getStatus() != null ? latestCert.getStatus().name() : null;
                certId = latestCert.getId();
            }
        }

        return new VaccinationCorrectionDetailResponse(
                correction.getId(),
                correction.getVaccinationRecordId(),
                citizenId,
                citizenName,
                vaccinatorId,
                vaccinatorName,
                certRef,
                certStatus,
                certId,
                correction.getFieldName() != null ? correction.getFieldName().getApiName() : null,
                correction.getOldValue(),
                correction.getNewValue(),
                correction.getReason(),
                correction.getCorrectedBy(),
                correction.getCorrectedAt(),
                correction.getStatus(),
                correction.getReviewedBy(),
                correction.getReviewedAt(),
                correction.getReviewReason()
        );
    }

    private String resolveCitizenName(Long citizenId) {
        if (citizenId == null || userAccountRepository == null) {
            return citizenId != null ? "Citizen #" + citizenId : null;
        }
        return userAccountRepository.findById(citizenId)
                .map(user -> user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : "Citizen #" + citizenId)
                .orElse("Citizen #" + citizenId);
    }

    private String resolveUserName(Long userId) {
        if (userId == null || userAccountRepository == null) {
            return userId != null ? "User #" + userId : null;
        }
        return userAccountRepository.findById(userId)
                .map(user -> user.getFullName() != null && !user.getFullName().isBlank() ? user.getFullName() : user.getEmail())
                .orElse("User #" + userId);
    }

    private VaccinationRecord findRecord(Long vaccinationRecordId) {
        return vaccinationRecordRepository.findById(vaccinationRecordId)
                .orElseThrow(() -> new VaccinationRecordNotFoundException(
                        "Vaccination record not found with ID: " + vaccinationRecordId
                ));
    }

    private void validateRequest(CreateVaccinationCorrectionRequest request, Long actorId) {
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
        if (actorId == null || actorId <= 0) {
            throw new InvalidVaccinationCorrectionException("Corrected-by ID must be positive");
        }
    }
}
