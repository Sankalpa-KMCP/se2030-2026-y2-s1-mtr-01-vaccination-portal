package com.se2030.vaccinationportal.campaign.controller;

import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSeriesCancellationRequest;
import com.se2030.vaccinationportal.campaign.dto.request.ConfirmSeriesChangeRequest;
import com.se2030.vaccinationportal.campaign.dto.request.CreateSessionRecurrenceRuleRequest;
import com.se2030.vaccinationportal.campaign.dto.request.GenerateOccurrencesRequest;
import com.se2030.vaccinationportal.campaign.dto.request.PreviewSeriesChangeRequest;
import com.se2030.vaccinationportal.campaign.dto.request.UpdateSessionRecurrenceRuleRequest;
import com.se2030.vaccinationportal.campaign.dto.response.RecurrenceGenerationResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesChangePreviewResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SeriesChangeResultResponse;
import com.se2030.vaccinationportal.campaign.dto.response.SessionRecurrenceRuleResponse;
import com.se2030.vaccinationportal.campaign.mapper.RecurrenceRuleDtoMapper;
import com.se2030.vaccinationportal.campaign.model.RecurrenceRuleDefinitionSnapshot;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRule;
import com.se2030.vaccinationportal.campaign.model.SessionRecurrenceRuleStatus;
import com.se2030.vaccinationportal.campaign.service.RecurrenceGenerationResult;
import com.se2030.vaccinationportal.campaign.service.SessionOccurrenceGenerationService;
import com.se2030.vaccinationportal.campaign.service.SessionRecurrenceRuleService;
import com.se2030.vaccinationportal.campaign.service.SessionSeriesChangeService;
import com.se2030.vaccinationportal.campaign.service.SeriesChangePreview;
import com.se2030.vaccinationportal.campaign.service.SeriesChangeResult;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing vaccination session recurrence rule management, occurrence generation,
 * and series-wide change control endpoints.
 */
@RestController
@RequestMapping("/api/v1/recurrence-rules")
public class RecurrenceRuleController {

    private final SessionRecurrenceRuleService recurrenceRuleService;
    private final SessionOccurrenceGenerationService occurrenceGenerationService;
    private final SessionSeriesChangeService seriesChangeService;

    public RecurrenceRuleController(
            SessionRecurrenceRuleService recurrenceRuleService,
            SessionOccurrenceGenerationService occurrenceGenerationService,
            SessionSeriesChangeService seriesChangeService
    ) {
        this.recurrenceRuleService = recurrenceRuleService;
        this.occurrenceGenerationService = occurrenceGenerationService;
        this.seriesChangeService = seriesChangeService;
    }

    @PostMapping
    public ResponseEntity<SessionRecurrenceRuleResponse> createRule(
            @Valid @RequestBody CreateSessionRecurrenceRuleRequest request
    ) {
        SessionRecurrenceRule created = recurrenceRuleService.createRule(
                request.campaignId(),
                request.centerId(),
                request.weekdays(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.timezone()
        );

        URI location = URI.create("/api/v1/recurrence-rules/" + created.getId());
        return ResponseEntity.created(location).body(RecurrenceRuleDtoMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionRecurrenceRuleResponse> getRuleById(@PathVariable Long id) {
        SessionRecurrenceRule rule = recurrenceRuleService.getRequiredRuleById(id);
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toResponse(rule));
    }

    @GetMapping
    public ResponseEntity<List<SessionRecurrenceRuleResponse>> getRules(
            @RequestParam(required = false) Long campaignId,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) SessionRecurrenceRuleStatus status
    ) {
        List<SessionRecurrenceRule> rules = recurrenceRuleService.getRules(campaignId, centerId, status);
        List<SessionRecurrenceRuleResponse> response = rules.stream()
                .map(RecurrenceRuleDtoMapper::toResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SessionRecurrenceRuleResponse> updateFutureRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSessionRecurrenceRuleRequest request
    ) {
        SessionRecurrenceRule updated = recurrenceRuleService.updateFutureRule(
                id,
                request.weekdays(),
                request.startDate(),
                request.endDate(),
                request.startTime(),
                request.endTime(),
                request.timezone()
        );
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toResponse(updated));
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<RecurrenceGenerationResponse> generateOccurrences(
            @PathVariable Long id,
            @RequestBody(required = false) GenerateOccurrencesRequest request
    ) {
        RecurrenceGenerationResult result;
        if (request == null || (request.requestedStartDate() == null && request.requestedEndDate() == null)) {
            result = occurrenceGenerationService.generateOccurrences(id);
        } else {
            result = occurrenceGenerationService.generateOccurrences(
                    id, request.requestedStartDate(), request.requestedEndDate());
        }
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toGenerationResponse(result));
    }

    @PostMapping("/{id}/change-preview")
    public ResponseEntity<SeriesChangePreviewResponse> previewSeriesChange(
            @PathVariable Long id,
            @Valid @RequestBody PreviewSeriesChangeRequest request
    ) {
        RecurrenceRuleDefinitionSnapshot snapshot = RecurrenceRuleDtoMapper.toDefinitionSnapshot(request);
        SeriesChangePreview preview = seriesChangeService.previewSeriesChange(id, snapshot);
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toPreviewResponse(preview));
    }

    @PostMapping("/{id}/change")
    public ResponseEntity<SeriesChangeResultResponse> confirmSeriesChange(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSeriesChangeRequest request
    ) {
        RecurrenceRuleDefinitionSnapshot snapshot = RecurrenceRuleDtoMapper.toDefinitionSnapshot(request);
        SeriesChangeResult result = seriesChangeService.confirmSeriesChange(
                id,
                snapshot,
                request.previewToken(),
                request.reason(),
                request.actorId()
        );
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toResultResponse(result));
    }

    @PostMapping("/{id}/cancellation-preview")
    public ResponseEntity<SeriesChangePreviewResponse> previewSeriesCancellation(@PathVariable Long id) {
        SeriesChangePreview preview = seriesChangeService.previewSeriesCancellation(id);
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toPreviewResponse(preview));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SeriesChangeResultResponse> confirmSeriesCancellation(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmSeriesCancellationRequest request
    ) {
        SeriesChangeResult result = seriesChangeService.confirmSeriesCancellation(
                id,
                request.previewToken(),
                request.reason(),
                request.actorId()
        );
        return ResponseEntity.ok(RecurrenceRuleDtoMapper.toResultResponse(result));
    }
}
