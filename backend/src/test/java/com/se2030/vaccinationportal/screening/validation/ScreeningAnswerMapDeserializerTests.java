package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.dto.ScreeningSubmissionRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScreeningAnswerMapDeserializerTests {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void preservesExistingAnswerObjectPayload() {
        String json = """
                {
                  "citizenId": 1,
                  "campaignId": 2,
                  "questionnaireVersionId": 3,
                  "consentNoticeVersionId": 4,
                  "answers": {"7": "true", "8": "notes"}
                }
                """;

        ScreeningSubmissionRequest request = jsonMapper.readValue(json, ScreeningSubmissionRequest.class);

        assertThat(request.answers()).containsEntry(7L, "true").containsEntry(8L, "notes");
    }

    @Test
    void rejectsDuplicateQuestionAnswerBeforeMapOverwrite() {
        String json = """
                {
                  "citizenId": 1,
                  "campaignId": 2,
                  "questionnaireVersionId": 3,
                  "consentNoticeVersionId": 4,
                  "answers": {"7": "true", "7": "false"}
                }
                """;

        assertThatThrownBy(() -> jsonMapper.readValue(json, ScreeningSubmissionRequest.class))
                .hasMessageContaining("Duplicate answer submitted for question 7");
    }

    @Test
    void rejectsEquivalentNumericQuestionKeys() {
        String json = """
                {
                  "citizenId": 1,
                  "campaignId": 2,
                  "questionnaireVersionId": 3,
                  "consentNoticeVersionId": 4,
                  "answers": {"7": "true", "007": "false"}
                }
                """;

        assertThatThrownBy(() -> jsonMapper.readValue(json, ScreeningSubmissionRequest.class))
                .hasMessageContaining("Duplicate answer submitted for question 7");
    }
}
