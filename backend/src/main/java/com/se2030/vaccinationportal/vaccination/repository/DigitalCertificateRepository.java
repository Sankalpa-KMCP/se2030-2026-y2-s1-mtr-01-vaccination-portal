package com.se2030.vaccinationportal.vaccination.repository;

import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import com.se2030.vaccinationportal.vaccination.model.CertificateStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for digital certificate identities.
 */
@Repository
public interface DigitalCertificateRepository extends JpaRepository<DigitalCertificate, Long> {

    boolean existsByVaccinationRecordId(Long vaccinationRecordId);

    boolean existsByVaccinationRecordIdAndStatus(Long vaccinationRecordId, CertificateStatus status);

    List<DigitalCertificate> findByVaccinationRecordIdOrderByIssuedAtAscIdAsc(Long vaccinationRecordId);

    Optional<DigitalCertificate> findFirstByVaccinationRecordIdAndStatusOrderByIssuedAtDescIdDesc(
            Long vaccinationRecordId,
            CertificateStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select certificate
            from DigitalCertificate certificate
            where certificate.id = :certificateId
            """)
    Optional<DigitalCertificate> findByIdForUpdate(@Param("certificateId") Long certificateId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select certificate
            from DigitalCertificate certificate
            where certificate.vaccinationRecordId = :vaccinationRecordId
              and certificate.status = :status
            order by certificate.issuedAt desc, certificate.id desc
            """)
    List<DigitalCertificate> findCurrentByVaccinationRecordIdAndStatusForUpdate(
            @Param("vaccinationRecordId") Long vaccinationRecordId,
            @Param("status") CertificateStatus status
    );

    Optional<DigitalCertificate> findByCertificateReference(String certificateReference);

    Optional<DigitalCertificate> findByVerificationToken(String verificationToken);

    @Query("""
            select certificate
            from DigitalCertificate certificate, VaccinationRecord record
            where certificate.vaccinationRecordId = record.id
              and record.citizenId = :citizenId
            order by certificate.issuedAt desc, certificate.id desc
            """)
    List<DigitalCertificate> findByCitizenIdOrderByIssuedAtDesc(@Param("citizenId") Long citizenId);
}
