package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.VaccineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the VaccineType master entity.
 */
@Repository
public interface VaccineTypeRepository extends JpaRepository<VaccineType, Long> {

    /**
     * Find a vaccine type by its unique business code.
     *
     * @param code Unique vaccine code (e.g. "VAX-MRNA-01")
     * @return Optional containing the VaccineType if found
     */
    Optional<VaccineType> findByCode(String code);

    /**
     * Check whether a vaccine type with the specified code already exists.
     *
     * @param code Unique vaccine code
     * @return true if an entity with this code exists
     */
    boolean existsByCode(String code);

    /**
     * List all currently active vaccine types.
     *
     * @return List of active VaccineType entities
     */
    List<VaccineType> findByActiveTrue();
}
