package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.OperationalResource;
import com.se2030.vaccinationportal.campaign.model.OperationalResourceStatus;
import com.se2030.vaccinationportal.campaign.model.ResourceType;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for center operational resources.
 */
@Repository
public interface OperationalResourceRepository extends JpaRepository<OperationalResource, Long> {

    Optional<OperationalResource> findByCenterIdAndCode(Long centerId, String code);

    boolean existsByCenterIdAndCode(Long centerId, String code);

    List<OperationalResource> findByCenterIdOrderByCodeAsc(Long centerId);

    List<OperationalResource> findByCenterIdAndResourceTypeOrderByCodeAsc(Long centerId, ResourceType resourceType);

    List<OperationalResource> findByCenterIdAndResourceTypeAndStatusOrderByCodeAsc(
            Long centerId,
            ResourceType resourceType,
            OperationalResourceStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OperationalResource r WHERE r.id = :id")
    Optional<OperationalResource> findByIdForUpdate(@Param("id") Long id);
}
