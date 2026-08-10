package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CenterClosure;
import com.se2030.vaccinationportal.campaign.model.CenterClosureStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for vaccination-center closure history.
 */
@Repository
public interface CenterClosureRepository extends JpaRepository<CenterClosure, Long> {

    @Query("SELECT c.center.id FROM CenterClosure c WHERE c.id = :id")
    Optional<Long> findCenterIdById(@Param("id") Long id);

    List<CenterClosure> findByCenterIdOrderByStartAtAscEndAtAscIdAsc(Long centerId);

    /**
     * Tests half-open interval overlap. A null exclude ID includes every closure.
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM CenterClosure c " +
            "WHERE c.center.id = :centerId " +
            "AND c.status = :status " +
            "AND (:excludeId IS NULL OR c.id <> :excludeId) " +
            "AND c.startAt < :endAt " +
            "AND :startAt < c.endAt")
    boolean existsOverlappingClosure(
            @Param("centerId") Long centerId,
            @Param("status") CenterClosureStatus status,
            @Param("excludeId") Long excludeId,
            @Param("startAt") Instant startAt,
            @Param("endAt") Instant endAt
    );
}
