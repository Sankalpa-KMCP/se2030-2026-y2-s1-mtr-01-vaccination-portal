package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.OperationalStaffRoster;
import com.se2030.vaccinationportal.campaign.model.OperationalStaffStatus;
import com.se2030.vaccinationportal.campaign.model.StaffRole;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for center operational staff roster and concurrency locking.
 */
@Repository
public interface OperationalStaffRosterRepository extends JpaRepository<OperationalStaffRoster, Long> {

    Optional<OperationalStaffRoster> findByCenterIdAndUserId(Long centerId, Long userId);

    boolean existsByCenterIdAndUserId(Long centerId, Long userId);

    List<OperationalStaffRoster> findByCenterIdOrderByUserIdAsc(Long centerId);

    List<OperationalStaffRoster> findByCenterIdAndRoleOrderByUserIdAsc(Long centerId, StaffRole role);

    List<OperationalStaffRoster> findByCenterIdAndStatusOrderByUserIdAsc(Long centerId, OperationalStaffStatus status);

    List<OperationalStaffRoster> findByCenterIdAndRoleAndStatusOrderByUserIdAsc(
            Long centerId,
            StaffRole role,
            OperationalStaffStatus status
    );

    List<OperationalStaffRoster> findByUserIdOrderByCenterIdAsc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OperationalStaffRoster r WHERE r.id = :id")
    Optional<OperationalStaffRoster> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OperationalStaffRoster r WHERE r.center.id = :centerId AND r.userId = :userId")
    Optional<OperationalStaffRoster> findByCenterIdAndUserIdForUpdate(@Param("centerId") Long centerId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OperationalStaffRoster r WHERE r.userId = :userId ORDER BY r.id ASC")
    List<OperationalStaffRoster> findByUserIdForUpdateOrderByIdAsc(@Param("userId") Long userId);
}
