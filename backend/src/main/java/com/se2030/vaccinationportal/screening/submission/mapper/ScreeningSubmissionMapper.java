package com.se2030.vaccinationportal.screening.submission.mapper;

import com.se2030.vaccinationportal.screening.submission.dto.response.ScreeningAnswerResponse;
import com.se2030.vaccinationportal.screening.submission.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.submission.model.ScreeningSubmission;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScreeningSubmissionMapper {

    public ScreeningSubmissionResponse toResponse(ScreeningSubmission submission) {
        return new ScreeningSubmissionResponse(
                submission.getId(),
                submission.getCitizenId(),
                submission.getCampaignId(),
                submission.getQuestionnaireVersion().getId(),
                submission.getConsentNoticeVersion().getId(),
                submission.getStatus(),
                submission.getPreliminaryResult(),
                submission.getConsentAt(),
                submission.getSubmittedAt(),
                submission.getAnswers().stream().map(this::toResponse).toList(),
                submission.getCreatedAt(),
                submission.getUpdatedAt(),
                submission.getCorrectionInstructions(),
                submission.getCorrectionRequestedAt()
        );
    }

    public List<ScreeningSubmissionResponse> toResponses(List<ScreeningSubmission> submissions) {
        return submissions.stream().map(this::toResponse).toList();
    }

    private ScreeningAnswerResponse toResponse(ScreeningAnswer answer) {
        return new ScreeningAnswerResponse(
                answer.getId(),
                answer.getQuestion().getId(),
                answer.getTypedAnswer(),
                answer.getCreatedAt()
        );
    }
}
