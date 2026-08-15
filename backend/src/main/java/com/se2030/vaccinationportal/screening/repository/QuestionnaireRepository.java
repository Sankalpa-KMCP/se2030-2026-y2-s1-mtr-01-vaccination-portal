package com.se2030.vaccinationportal.screening.repository;

import com.se2030.vaccinationportal.screening.model.Questionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
    Optional<Questionnaire> findByCode(String code);
    boolean existsByCode(String code);
}
