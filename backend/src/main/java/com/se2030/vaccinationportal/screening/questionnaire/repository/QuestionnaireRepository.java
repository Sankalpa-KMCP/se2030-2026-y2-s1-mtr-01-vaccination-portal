package com.se2030.vaccinationportal.screening.questionnaire.repository;

import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
    Optional<Questionnaire> findByCode(String code);
    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM Questionnaire q WHERE q.id = :id")
    Optional<Questionnaire> findByIdForUpdate(@Param("id") Long id);

    // Bulk deletion deliberately bypasses Questionnaire.versions cascade.
    // The FK and the NOT EXISTS condition remain the final guard against a concurrent insert.
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM Questionnaire q WHERE q.id = :id AND NOT EXISTS "
            + "(SELECT v.id FROM QuestionnaireVersion v WHERE v.questionnaire.id = :id)")
    int deleteIfWithoutVersions(@Param("id") Long id);
}
