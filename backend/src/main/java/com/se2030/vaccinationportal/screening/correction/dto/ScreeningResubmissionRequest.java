package com.se2030.vaccinationportal.screening.correction.dto;

import com.se2030.vaccinationportal.screening.validation.ScreeningAnswerMapDeserializer;
import jakarta.validation.constraints.NotEmpty;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.util.Map;

public record ScreeningResubmissionRequest(
        @NotEmpty(message = "Screening answers are required")
        @JsonDeserialize(using = ScreeningAnswerMapDeserializer.class)
        Map<Long, String> answers
) {}
