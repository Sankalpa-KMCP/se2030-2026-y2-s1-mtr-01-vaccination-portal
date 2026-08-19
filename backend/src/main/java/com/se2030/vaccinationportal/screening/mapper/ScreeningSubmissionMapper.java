package com.se2030.vaccinationportal.screening.mapper;

import com.se2030.vaccinationportal.screening.dto.response.ScreeningAnswerResponse;
import com.se2030.vaccinationportal.screening.dto.response.ScreeningSubmissionResponse;
import com.se2030.vaccinationportal.screening.model.ScreeningAnswer;
import com.se2030.vaccinationportal.screening.model.ScreeningSubmission;
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
                submission.getUpdatedAt()
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
