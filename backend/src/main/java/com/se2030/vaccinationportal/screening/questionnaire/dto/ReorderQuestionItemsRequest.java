package com.se2030.vaccinationportal.screening.questionnaire.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderQuestionItemsRequest(
        @NotEmpty(message = "Item IDs are required")
        List<Long> itemIds
) {}
