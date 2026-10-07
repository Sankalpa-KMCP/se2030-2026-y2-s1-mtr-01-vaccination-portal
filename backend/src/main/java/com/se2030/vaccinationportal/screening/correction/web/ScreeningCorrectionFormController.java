package com.se2030.vaccinationportal.screening.correction.web;

import com.se2030.vaccinationportal.screening.correction.dto.response.ScreeningCorrectionFormResponse;
import com.se2030.vaccinationportal.screening.correction.service.ScreeningCorrectionFormService;
import com.se2030.vaccinationportal.screening.validation.RequireRoles;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/screening")
public class ScreeningCorrectionFormController {

    private final ScreeningCorrectionFormService correctionFormService;

    public ScreeningCorrectionFormController(ScreeningCorrectionFormService correctionFormService) {
        this.correctionFormService = correctionFormService;
    }

    @GetMapping("/submissions/{submissionId}/correction-form")
    @RequireRoles({"CITIZEN", "ADMIN", "SYSTEM_ADMINISTRATOR"})
    public ResponseEntity<ScreeningCorrectionFormResponse> getCorrectionForm(
            @PathVariable("submissionId") Long submissionId
    ) {
        return ResponseEntity.ok(correctionFormService.getCorrectionForm(submissionId));
    }
}
