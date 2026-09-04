package com.se2030.vaccinationportal.screening.service;

import com.se2030.vaccinationportal.screening.exception.ResourceNotFoundException;
import com.se2030.vaccinationportal.screening.mapper.ScreeningHistoryDetailsMapper;
import com.se2030.vaccinationportal.screening.model.*;
import com.se2030.vaccinationportal.screening.repository.AnswerOptionRepository;
import com.se2030.vaccinationportal.screening.repository.QuestionnaireItemRepository;
import com.se2030.vaccinationportal.screening.repository.ScreeningSubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScreeningHistoryServiceTests {
    @Mock private ScreeningSubmissionRepository submissions;
    @Mock private QuestionnaireItemRepository items;
    @Mock private AnswerOptionRepository options;
    @Mock private CitizenScreeningAccessService access;
    private ScreeningHistoryService service;

    @BeforeEach
    void setUp() {
        service = new ScreeningHistoryService(submissions, items, options, access, new ScreeningHistoryDetailsMapper());
    }

    @Test
    void usesOnlyStoredVersionAndRemainsReadableAfterRetirement() {
        QuestionnaireVersion version = new QuestionnaireVersion(null, 2, QuestionnaireVersionStatus.PUBLISHED);
        version.setId(70L);
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setId(7L);
        submission.setCitizenId(6L);
        submission.setCampaignId(1L);
        submission.setQuestionnaireVersion(version);
        ScreeningQuestion question = new ScreeningQuestion("OLD", "Original question?", QuestionType.SINGLE_CHOICE, true, true);
        question.setId(10L);
        submission.addAnswer(new ScreeningAnswer(submission, question, "old-value"));
        when(submissions.findById(7L)).thenReturn(Optional.of(submission));
        when(items.findByQuestionnaireVersionIdOrderByDisplayOrderAsc(70L))
                .thenReturn(List.of(new QuestionnaireItem(version, question, 4, null)));
        when(options.findByQuestionIdOrderByDisplayOrderAsc(10L))
                .thenReturn(List.of(new AnswerOption(question, "OLD", "Original label", "old-value", 1)));

        var publishedDetails = service.getHistoryDetails(7L);
        // A campaign may subsequently use any other version: the only lookup key remains stored version 70.
        version.setStatus(QuestionnaireVersionStatus.RETIRED);
        var retiredDetails = service.getHistoryDetails(7L);

        assertThat(retiredDetails).isEqualTo(publishedDetails);
        assertThat(retiredDetails.questionnaireVersionId()).isEqualTo(70L);
        assertThat(retiredDetails.questionnaireVersionNumber()).isEqualTo(2);
        assertThat(retiredDetails.answers().get(0).questionText()).isEqualTo("Original question?");
        assertThat(retiredDetails.answers().get(0).displayAnswer()).isEqualTo("Original label");
        verify(items, times(2)).findByQuestionnaireVersionIdOrderByDisplayOrderAsc(70L);
        verifyNoMoreInteractions(items);
        verify(submissions, never()).save(any());
        verify(options, never()).save(any());
    }

    @Test
    void ownershipFailurePreventsReadingQuestionnaireAndOptions() {
        ScreeningSubmission submission = new ScreeningSubmission();
        submission.setCitizenId(6L);
        when(submissions.findById(7L)).thenReturn(Optional.of(submission));
        doThrow(new AccessDeniedException("Not the owner")).when(access).requireCitizenIdAccess(
                6L, "ROLE_HEALTHCARE_OFFICER", "ROLE_ADMIN", "ROLE_SYSTEM_ADMINISTRATOR");
        assertThatThrownBy(() -> service.getHistoryDetails(7L)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(items, options);
    }

    @Test
    void nonexistentSubmissionStopsBeforeHydration() {
        when(submissions.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getHistoryDetails(7L)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(items, options, access);
    }

    @Test
    void nullIdIsRejectedBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.getHistoryDetails(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(submissions, items, options, access);
    }
}
