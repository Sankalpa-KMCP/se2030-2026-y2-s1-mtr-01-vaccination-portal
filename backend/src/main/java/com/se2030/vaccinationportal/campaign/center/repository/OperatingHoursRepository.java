package com.se2030.vaccinationportal.campaign.center.repository;

import com.se2030.vaccinationportal.campaign.center.model.OperatingHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link OperatingHours} entity.
 * Provides persistence queries for center schedule lookups, weekday siblings, and effective hours.
 */
@Repository
public interface OperatingHoursRepository extends JpaRepository<OperatingHours, Long> {

    /**
     * Finds the parent center ID associated with an operating hours record.
     * Used for safe lock ordering prior to loading managed target state.
     *
     * @param id ID of the OperatingHours
     * @return Optional containing the parent center ID if found
     */
    @Query("SELECT oh.center.id FROM OperatingHours oh WHERE oh.id = :id")
    Optional<Long> findCenterIdById(@Param("id") Long id);

    /**
     * Finds all operating hours records associated with a vaccination center.
     *
     * @param centerId ID of the VaccinationCenter
     * @return List of operating hours for the center
     */
    List<OperatingHours> findByCenterId(Long centerId);

    /**
     * Finds all operating hours records for a specific center and day of the week.
     *
     * @param centerId  ID of the VaccinationCenter
     * @param dayOfWeek Day of the week
     * @return List of matching operating hours records
     */
    List<OperatingHours> findByCenterIdAndDayOfWeek(Long centerId, DayOfWeek dayOfWeek);

    /**
     * Finds all operating hours for a specific center and weekday that are effective on a given date.
     * Evaluates inclusive effective range where {@code effectiveFrom <= date} and
     * ({@code effectiveTo IS NULL} or {@code effectiveTo >= date}).
     * Results are ordered by opening time ascending to support multiple split shifts.
     *
     * @param centerId  ID of the VaccinationCenter
     * @param dayOfWeek Day of the week
     * @param date      Evaluation date
     * @return List of effective operating hours ordered by opening time ascending
     */
    @Query("SELECT oh FROM OperatingHours oh " +
           "WHERE oh.center.id = :centerId " +
           "AND oh.dayOfWeek = :dayOfWeek " +
           "AND oh.effectiveFrom <= :date " +
           "AND (oh.effectiveTo IS NULL OR oh.effectiveTo >= :date) " +
           "ORDER BY oh.opening ASC")
    List<OperatingHours> findEffectiveOperatingHours(
            @Param("centerId") Long centerId,
            @Param("dayOfWeek") DayOfWeek dayOfWeek,
            @Param("date") LocalDate date
    );
}
