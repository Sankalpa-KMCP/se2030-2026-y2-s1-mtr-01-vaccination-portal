package com.se2030.vaccinationportal.shared.auth.repository;

import com.se2030.vaccinationportal.shared.auth.model.EmailOtpChallenge;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Auth-internal persistence for the current email OTP verification challenge.
 * Exactly one challenge row exists per account (unique user_id), and verify/resend
 * serialize on a pessimistic row lock so concurrent attempts cannot interleave.
 */
@Repository
public interface EmailOtpChallengeRepository extends JpaRepository<EmailOtpChallenge, Long> {

    Optional<EmailOtpChallenge> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from EmailOtpChallenge c where c.user.id = :userId")
    Optional<EmailOtpChallenge> findByUserIdForUpdate(@Param("userId") Long userId);
}
