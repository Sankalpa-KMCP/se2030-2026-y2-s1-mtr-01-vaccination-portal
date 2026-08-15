package com.se2030.vaccinationportal.screening.repository;

import com.se2030.vaccinationportal.screening.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.model.QuestionnaireVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionnaireVersionRepository extends JpaRepository<QuestionnaireVersion, Long> {
    List<QuestionnaireVersion> findByQuestionnaireIdOrderByVersionNumberDesc(Long questionnaireId);
    Optional<QuestionnaireVersion> findByQuestionnaireIdAndVersionNumber(Long questionnaireId, Integer versionNumber);
    Optional<QuestionnaireVersion> findFirstByQuestionnaireIdOrderByVersionNumberDesc(Long questionnaireId);

    @Query("SELECT qv FROM QuestionnaireVersion qv WHERE qv.id = :id AND qv.status = :status")
    Optional<QuestionnaireVersion> findByIdAndStatus(@Param("id") Long id, @Param("status") QuestionnaireVersionStatus status);

    @Query("SELECT COUNT(s) > 0 FROM ScreeningSubmission s WHERE s.questionnaireVersion.id = :versionId")
    boolean hasSubmissions(@Param("versionId") Long versionId);
}
