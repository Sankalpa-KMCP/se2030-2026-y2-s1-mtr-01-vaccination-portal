package com.se2030.vaccinationportal.screening.controller;

import com.se2030.vaccinationportal.screening.dto.response.ScreeningHistoryDetailsResponse;
import com.se2030.vaccinationportal.screening.service.ScreeningHistoryService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningHistoryController {
    private final ScreeningHistoryService historyService;

    public ScreeningHistoryController(ScreeningHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/submissions/{submissionId}/history-details")
    @RequireRoles({"CITIZEN", "HEALTHCARE_OFFICER", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningHistoryDetailsResponse> getHistoryDetails(
            @PathVariable("submissionId") Long submissionId
    ) {
        return ResponseEntity.ok(historyService.getHistoryDetails(submissionId));
    }
}
