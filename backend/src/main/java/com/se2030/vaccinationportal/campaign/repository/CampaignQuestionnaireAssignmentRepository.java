package com.se2030.vaccinationportal.campaign.repository;

import com.se2030.vaccinationportal.campaign.model.CampaignQuestionnaireAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link CampaignQuestionnaireAssignment} entity.
 * Provides query methods for effective-dated questionnaire version lookups, history, and overlap prevention.
 */
@Repository
public interface CampaignQuestionnaireAssignmentRepository extends JpaRepository<CampaignQuestionnaireAssignment, Long> {

    /**
     * Finds all questionnaire assignments for a campaign ordered by activeFrom descending, then ID descending.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @return List of questionnaire assignments
     */
    List<CampaignQuestionnaireAssignment> findByCampaignIdOrderByActiveFromDescIdDesc(Long campaignId);

    /**
     * Finds the single effective questionnaire assignment for a campaign on a specific evaluation date.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @param date Evaluation date
     * @return Optional containing the effective assignment if found
     */
    @Query("SELECT a FROM CampaignQuestionnaireAssignment a " +
           "WHERE a.campaign.id = :campaignId " +
           "AND a.activeFrom <= :date " +
           "AND (a.activeTo IS NULL OR a.activeTo > :date)")
    Optional<CampaignQuestionnaireAssignment> findEffectiveAssignmentAt(
            @Param("campaignId") Long campaignId,
            @Param("date") LocalDate date
    );

    /**
     * Finds all effective questionnaire assignments for a campaign on a specific evaluation date.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @param date Evaluation date
     * @return List of effective assignments found
     */
    @Query("SELECT a FROM CampaignQuestionnaireAssignment a " +
           "WHERE a.campaign.id = :campaignId " +
           "AND a.activeFrom <= :date " +
           "AND (a.activeTo IS NULL OR a.activeTo > :date)")
    List<CampaignQuestionnaireAssignment> findEffectiveAssignmentsAtList(
            @Param("campaignId") Long campaignId,
            @Param("date") LocalDate date
    );

    /**
     * Finds the open-ended questionnaire assignment (activeTo IS NULL) for a campaign, if one exists.
     * Note: Open-ended does not automatically mean currently effective if activeFrom is in the future.
     *
     * @param campaignId ID of the VaccinationCampaign
     * @return Optional containing the open-ended assignment if found
     */
    @Query("SELECT a FROM CampaignQuestionnaireAssignment a " +
           "WHERE a.campaign.id = :campaignId " +
           "AND a.activeTo IS NULL")
    Optional<CampaignQuestionnaireAssignment> findOpenEndedAssignment(@Param("campaignId") Long campaignId);

    /**
     * Finds assignments whose half-open effective periods overlap [activeFrom, activeTo).
     *
     * @param campaignId ID of the VaccinationCampaign
     * @param activeFrom Start date of the candidate interval
     * @param activeTo End date of the candidate interval (null for open-ended)
     * @param excludeId Optional assignment ID to exclude (e.g. when updating/closing an existing version)
     * @return List of overlapping assignments
     */
    @Query("SELECT a FROM CampaignQuestionnaireAssignment a " +
           "WHERE a.campaign.id = :campaignId " +
           "AND (:excludeId IS NULL OR a.id != :excludeId) " +
           "AND (:activeTo IS NULL OR a.activeFrom < :activeTo) " +
           "AND (a.activeTo IS NULL OR a.activeTo > :activeFrom)")
    List<CampaignQuestionnaireAssignment> findOverlappingAssignments(
            @Param("campaignId") Long campaignId,
            @Param("activeFrom") LocalDate activeFrom,
            @Param("activeTo") LocalDate activeTo,
            @Param("excludeId") Long excludeId
    );

    /**
     * Finds all assignments across campaigns that link to a specific QuestionnaireVersion ID.
     *
     * @param questionnaireVersionId ID of the QuestionnaireVersion in MF-01
     * @return List of assignments referencing this questionnaire version
     */
    List<CampaignQuestionnaireAssignment> findByQuestionnaireVersionId(Long questionnaireVersionId);

}
