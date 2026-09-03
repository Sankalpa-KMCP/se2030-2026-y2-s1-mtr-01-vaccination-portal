package com.se2030.vaccinationportal.vaccination.repository;

import com.se2030.vaccinationportal.vaccination.model.DigitalCertificate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for digital certificate identities.
 */
@Repository
public interface DigitalCertificateRepository extends JpaRepository<DigitalCertificate, Long> {

    Optional<DigitalCertificate> findByCertificateReference(String certificateReference);

    Optional<DigitalCertificate> findByVerificationToken(String verificationToken);
}
