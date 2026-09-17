package com.se2030.vaccinationportal.screening.validation;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/**
 * Rejects floating-point numbers (such as 1.5) and non-numeric strings
 * during request binding instead of silently truncating them to integers.
 */
public class StrictIntegerDeserializer extends ValueDeserializer<Integer> {

    @Override
    public Integer deserialize(JsonParser parser, DeserializationContext context) {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }
        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return parser.getIntValue();
        }
        if (parser.currentToken() == JsonToken.VALUE_STRING) {
            String text = parser.getString().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(text);
            } catch (NumberFormatException ex) {
                throw DatabindException.from(parser, "Value must be a whole integer: " + text, ex);
            }
        }
        throw DatabindException.from(parser, "Value must be a whole integer, got: " + parser.currentToken());
    }
}
