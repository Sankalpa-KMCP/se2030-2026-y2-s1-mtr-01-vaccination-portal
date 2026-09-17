package com.se2030.vaccinationportal.screening.validation;

import com.se2030.vaccinationportal.screening.dto.AddQuestionItemRequest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StrictIntegerDeserializerTests {

    private final JsonMapper jsonMapper = new JsonMapper();

    @Test
    void deserializesValidIntegerDisplayOrder() throws Exception {
        String json = """
                {
                  "questionId": 10,
                  "displayOrder": 2,
                  "requiredOverride": true
                }
                """;

        AddQuestionItemRequest request = jsonMapper.readValue(json, AddQuestionItemRequest.class);

        assertThat(request.questionId()).isEqualTo(10L);
        assertThat(request.displayOrder()).isEqualTo(2);
        assertThat(request.requiredOverride()).isTrue();
    }

    @Test
    void deserializesValidNumericStringDisplayOrder() throws Exception {
        String json = """
                {
                  "questionId": 10,
                  "displayOrder": "3"
                }
                """;

        AddQuestionItemRequest request = jsonMapper.readValue(json, AddQuestionItemRequest.class);

        assertThat(request.displayOrder()).isEqualTo(3);
    }

    @Test
    void deserializesNullDisplayOrderAsNull() throws Exception {
        String json = """
                {
                  "questionId": 10,
                  "displayOrder": null
                }
                """;

        AddQuestionItemRequest request = jsonMapper.readValue(json, AddQuestionItemRequest.class);

        assertThat(request.displayOrder()).isNull();
    }

    @Test
    void rejectsFloatingPointDisplayOrder() {
        String json = """
                {
                  "questionId": 10,
                  "displayOrder": 1.5
                }
                """;

        assertThatThrownBy(() -> jsonMapper.readValue(json, AddQuestionItemRequest.class))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("Value must be a whole integer");
    }

    @Test
    void rejectsNonNumericStringDisplayOrder() {
        String json = """
                {
                  "questionId": 10,
                  "displayOrder": "abc"
                }
                """;

        assertThatThrownBy(() -> jsonMapper.readValue(json, AddQuestionItemRequest.class))
                .isInstanceOf(DatabindException.class)
                .hasMessageContaining("Value must be a whole integer");
    }
}
