package com.se2030.vaccinationportal.campaign.center.repository;

import com.se2030.vaccinationportal.campaign.center.model.CenterStatus;
import com.se2030.vaccinationportal.campaign.center.model.VaccinationCenter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link VaccinationCenter} entity.
 */
@Repository
public interface VaccinationCenterRepository extends JpaRepository<VaccinationCenter, Long> {

    /**
     * Finds a vaccination center by ID and acquires a pessimistic write lock (SELECT ... FOR UPDATE).
     * Serializes concurrent operating hours and schedule modifications per WBVP-SE2030 concurrency rules.
     *
     * @param id Unique center identifier
     * @return Optional containing the locked VaccinationCenter if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM VaccinationCenter c WHERE c.id = :id")
    Optional<VaccinationCenter> findByIdForUpdate(@Param("id") Long id);

    /**
     * Finds a vaccination center by its unique business code.
     *
     * @param code Normalized center code (e.g. "CTR-NCMC")
     * @return Optional containing the center if found
     */
    Optional<VaccinationCenter> findByCode(String code);

    /**
     * Checks if a vaccination center with the given code already exists.
     *
     * @param code Normalized center code
     * @return true if center exists with this code
     */
    boolean existsByCode(String code);

    /**
     * Finds all vaccination centers matching a specific operational status.
     *
     * @param centerStatus {@link CenterStatus} enum (ACTIVE or INACTIVE)
     * @return List of matching centers
     */
    List<VaccinationCenter> findByCenterStatus(CenterStatus centerStatus);

    /**
     * Finds all vaccination centers matching a specific operational status ordered by code ascending.
     *
     * @param centerStatus {@link CenterStatus} enum (ACTIVE or INACTIVE)
     * @return List of matching centers ordered by code ascending
     */
    List<VaccinationCenter> findByCenterStatusOrderByCodeAsc(CenterStatus centerStatus);

    /**
     * Finds all vaccination centers ordered alphabetically by code.
     *
     * @return List of all centers ordered by code ascending
     */
    List<VaccinationCenter> findAllByOrderByCodeAsc();
}
