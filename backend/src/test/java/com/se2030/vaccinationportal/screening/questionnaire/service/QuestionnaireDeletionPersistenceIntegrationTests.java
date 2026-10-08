package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.ConsentNoticeVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionType;
import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.model.ScreeningQuestion;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionnaireDeletionPersistenceIntegrationTests {
    @Autowired private EntityManager entityManager;
    @Autowired private ScreeningConfigurationService service;

    @Test
    void deletesOnlyTheEmptyParent() {
        Questionnaire empty = persist(new Questionnaire("DELETE-EMPTY", "Empty", null));
        Long id = empty.getId();
        entityManager.flush();
        entityManager.clear();

        service.deleteQuestionnaire(id);
        entityManager.clear();

        assertThat(entityManager.find(Questionnaire.class, id)).isNull();
    }

    @ParameterizedTest
    @EnumSource(QuestionnaireVersionStatus.class)
    void anyPersistedVersionPreventsParentDeletion(QuestionnaireVersionStatus status) {
        Questionnaire parent = persist(new Questionnaire("DELETE-" + status, "Protected", null));
        QuestionnaireVersion version = persist(new QuestionnaireVersion(parent, 1, status));
        Long parentId = parent.getId();
        Long versionId = version.getId();
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> service.deleteQuestionnaire(parentId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("contains versions");
        entityManager.clear();

        assertThat(entityManager.find(Questionnaire.class, parentId)).isNotNull();
        assertThat(entityManager.find(QuestionnaireVersion.class, versionId)).isNotNull();
    }

    @Test
    void historicalSubmissionAndAnswerRemainIntactWhenDeleteIsRejected() {
        Questionnaire parent = persist(new Questionnaire("DELETE-HISTORY", "Historical", null));
        QuestionnaireVersion version = persist(new QuestionnaireVersion(parent, 1, QuestionnaireVersionStatus.PUBLISHED));
        ScreeningQuestion question = persist(new ScreeningQuestion("DELETE-Q", "Historical answer?", QuestionType.TEXT, true, true));
        ConsentNoticeVersion consent = persist(new ConsentNoticeVersion("DELETE-CONSENT", 1,
                "Consent", "Consent text", "hash", Instant.parse("2026-01-01T00:00:00Z")));
        ScreeningSubmission submission = new ScreeningSubmission(42L, 99L, version, consent, Instant.now());
        submission.addAnswer(new ScreeningAnswer(submission, question, "Yes"));
        persist(submission);
        Long parentId = parent.getId();
        Long versionId = version.getId();
        Long submissionId = submission.getId();
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> service.deleteQuestionnaire(parentId))
                .isInstanceOf(IllegalStateException.class);
        entityManager.clear();

        assertThat(entityManager.find(Questionnaire.class, parentId)).isNotNull();
        assertThat(entityManager.find(QuestionnaireVersion.class, versionId)).isNotNull();
        ScreeningSubmission preserved = entityManager.find(ScreeningSubmission.class, submissionId);
        assertThat(preserved).isNotNull();
        assertThat(preserved.getAnswers()).hasSize(1);
    }

    private <T> T persist(T entity) {
        entityManager.persist(entity);
        return entity;
    }
}
