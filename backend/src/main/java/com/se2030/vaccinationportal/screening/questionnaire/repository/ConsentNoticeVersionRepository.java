package com.se2030.vaccinationportal.screening.questionnaire.repository;

import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsentNoticeVersionRepository extends JpaRepository<ConsentNoticeVersion, Long> {
    Optional<ConsentNoticeVersion> findByNoticeCodeAndVersionNumber(String noticeCode, Integer versionNumber);
    Optional<ConsentNoticeVersion> findFirstByNoticeCodeAndRetiredAtIsNullOrderByVersionNumberDesc(String noticeCode);
}
