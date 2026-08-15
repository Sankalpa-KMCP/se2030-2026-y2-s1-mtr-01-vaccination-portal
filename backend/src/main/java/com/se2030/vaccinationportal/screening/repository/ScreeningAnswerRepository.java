package com.se2030.vaccinationportal.screening.repository;

import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreeningAnswerRepository extends JpaRepository<ScreeningAnswer, Long> {
    List<ScreeningAnswer> findBySubmissionId(Long submissionId);
    Optional<ScreeningAnswer> findBySubmissionIdAndQuestionId(Long submissionId, Long questionId);
}
