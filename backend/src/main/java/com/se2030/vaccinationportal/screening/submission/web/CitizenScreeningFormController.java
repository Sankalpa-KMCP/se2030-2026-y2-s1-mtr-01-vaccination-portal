package com.se2030.vaccinationportal.screening.submission.web;

import com.se2030.vaccinationportal.screening.submission.dto.response.ScreeningFormResponse;
import com.se2030.vaccinationportal.screening.submission.service.ScreeningFormService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/screening/campaigns")
public class CitizenScreeningFormController {

    private final ScreeningFormService screeningFormService;

    public CitizenScreeningFormController(ScreeningFormService screeningFormService) {
        this.screeningFormService = screeningFormService;
    }

    @GetMapping("/{campaignId}/form")
    @RequireRoles({"CITIZEN", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningFormResponse> getScreeningForm(
            @PathVariable("campaignId") Long campaignId
    ) {
        return ResponseEntity.ok(screeningFormService.getFormForCampaign(campaignId));
    }
}
