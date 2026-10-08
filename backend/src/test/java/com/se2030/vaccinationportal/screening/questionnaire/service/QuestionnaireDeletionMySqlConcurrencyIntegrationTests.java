package com.se2030.vaccinationportal.screening.questionnaire.service;

import com.se2030.vaccinationportal.screening.questionnaire.model.Questionnaire;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersion;
import com.se2030.vaccinationportal.screening.questionnaire.model.QuestionnaireVersionStatus;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireRepository;
import com.se2030.vaccinationportal.screening.questionnaire.repository.QuestionnaireVersionRepository;
import com.se2030.vaccinationportal.testcontainers.AbstractMySqlTestContainersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionnaireDeletionMySqlConcurrencyIntegrationTests extends AbstractMySqlTestContainersIntegrationTest {
    @Autowired private QuestionnaireRepository questionnaireRepository;
    @Autowired private QuestionnaireVersionRepository versionRepository;
    @Autowired private ScreeningConfigurationService configurationService;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    @Timeout(40)
    void versionInsertRacingWithDeletionCannotEraseVersionData() throws Exception {
        assertConnectedToRealMySql();
        TransactionTemplate transactions = new TransactionTemplate(transactionManager);
        Long id = transactions.execute(ignored -> questionnaireRepository
                .saveAndFlush(new Questionnaire("DELETE-RACE", "Race fixture", null)).getId());
        CountDownLatch parentRead = new CountDownLatch(1);
        CountDownLatch permitInsert = new CountDownLatch(1);
        CountDownLatch insertAttempted = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> insertion = executor.submit(() -> transactions.execute(ignored -> {
                Questionnaire parent = questionnaireRepository.findById(id).orElseThrow();
                parentRead.countDown();
                try {
                    if (!permitInsert.await(10, TimeUnit.SECONDS)) {
                        throw new AssertionError("Insert was not released");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                insertAttempted.countDown();
                versionRepository.saveAndFlush(new QuestionnaireVersion(parent, 1, QuestionnaireVersionStatus.DRAFT));
                return null;
            }));

            assertThat(parentRead.await(10, TimeUnit.SECONDS)).isTrue();
            transactions.executeWithoutResult(ignored -> {
                questionnaireRepository.findByIdForUpdate(id).orElseThrow();
                permitInsert.countDown();
                try {
                    assertThat(insertAttempted.await(10, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
                configurationService.deleteQuestionnaire(id);
            });

            assertThatThrownBy(() -> insertion.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);
            assertThat(questionnaireRepository.existsById(id)).isFalse();
            assertThat(versionRepository.existsByQuestionnaireId(id)).isFalse();
        } finally {
            permitInsert.countDown();
        }
    }
}
