package com.se2030.vaccinationportal.screening.validation;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Preserves the existing question-id-to-answer JSON object contract while rejecting
 * duplicate question keys before a map can silently overwrite them.
 */
public class ScreeningAnswerMapDeserializer extends ValueDeserializer<Map<Long, String>> {

    @Override
    public Map<Long, String> deserialize(JsonParser parser, DeserializationContext context) {
        if (!parser.hasToken(JsonToken.START_OBJECT)) {
            throw DatabindException.from(parser, "Screening answers must be a JSON object keyed by question ID");
        }

        Map<Long, String> answers = new LinkedHashMap<>();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (!parser.hasToken(JsonToken.PROPERTY_NAME)) {
                throw DatabindException.from(parser, "Expected a question ID in screening answers");
            }

            String rawQuestionId = parser.currentName();
            Long questionId;
            try {
                questionId = Long.valueOf(rawQuestionId);
            } catch (NumberFormatException ex) {
                throw DatabindException.from(parser, "Invalid question ID in screening answers: " + rawQuestionId, ex);
            }

            if (answers.containsKey(questionId)) {
                throw DatabindException.from(parser, "Duplicate answer submitted for question " + questionId);
            }

            JsonToken valueToken = parser.nextToken();
            if (valueToken == JsonToken.VALUE_NULL) {
                answers.put(questionId, null);
            } else if (valueToken == JsonToken.VALUE_STRING) {
                answers.put(questionId, parser.getString());
            } else {
                throw DatabindException.from(
                        parser,
                        "Answer for question " + questionId + " must be a string"
                );
            }
        }
        return answers;
    }
}
