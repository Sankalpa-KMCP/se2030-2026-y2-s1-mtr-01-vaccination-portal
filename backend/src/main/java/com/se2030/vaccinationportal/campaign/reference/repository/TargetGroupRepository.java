package com.se2030.vaccinationportal.campaign.reference.repository;

import com.se2030.vaccinationportal.campaign.reference.model.TargetGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the TargetGroup domain entity.
 */
@Repository
public interface TargetGroupRepository extends JpaRepository<TargetGroup, Long> {

    /**
     * Find a target group by its unique business code.
     *
     * @param code Unique target group code (e.g. "TG-ELDERLY-65")
     * @return Optional containing the TargetGroup if found
     */
    Optional<TargetGroup> findByCode(String code);

    /**
     * Check whether a target group with the specified code already exists.
     *
     * @param code Unique target group code
     * @return true if an entity with this code exists
     */
    boolean existsByCode(String code);

    /**
     * List all currently active target groups.
     *
     * @return List of active TargetGroup entities
     */
    List<TargetGroup> findByActiveTrue();
}
