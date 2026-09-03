package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.OperationalStaffIdentity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence repository for MF-02 operational staff identities and pessimistic concurrency locking.
 */
@Repository
public interface OperationalStaffIdentityRepository extends JpaRepository<OperationalStaffIdentity, Long> {

    Optional<OperationalStaffIdentity> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM OperationalStaffIdentity i WHERE i.userId = :userId")
    Optional<OperationalStaffIdentity> findByUserIdForUpdate(@Param("userId") Long userId);

    /**
     * Atomically bootstraps the operational staff identity anchor for a user if it does not yet exist.
     * Uses a database-level idempotent upsert to guarantee collision-free initialization under concurrent execution.
     */
    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query(
            value = "INSERT INTO operational_staff_identities (user_id, created_at) VALUES (:userId, CURRENT_TIMESTAMP) ON DUPLICATE KEY UPDATE user_id = user_id",
            nativeQuery = true
    )
    int bootstrapIdentity(@Param("userId") Long userId);
}
