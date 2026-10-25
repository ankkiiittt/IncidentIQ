package com.incidentiq.analytics.controller;

import com.incidentiq.analytics.dto.*;
import com.incidentiq.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "System reliability metrics, SLA performance, recurring causes, and AI effectiveness")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT', 'VIEWER')")
    @Operation(summary = "Get high-level reliability KPIs: MTTR, MTTD, SLA rate, and incident counts")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analyticsService.getOverview(days));
    }

    @GetMapping("/volume")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT', 'VIEWER')")
    @Operation(summary = "Get incident volume time series grouped by day or week and priority")
    public ResponseEntity<AnalyticsVolumeResponse> getVolume(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "day") String groupBy
    ) {
        return ResponseEntity.ok(analyticsService.getVolumeTrend(days, groupBy));
    }

    @GetMapping("/sla")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT', 'VIEWER')")
    @Operation(summary = "Get SLA compliance breakdown by priority and target time")
    public ResponseEntity<AnalyticsSlaResponse> getSla(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analyticsService.getSlaPerformance(days));
    }

    @GetMapping("/causes")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT', 'VIEWER')")
    @Operation(summary = "Get incident category distribution and recurring causes table")
    public ResponseEntity<AnalyticsCausesResponse> getCauses(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analyticsService.getCausesAndRecurrence(days));
    }

    @GetMapping("/ai-feedback")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENGINEER', 'SUPPORT', 'VIEWER')")
    @Operation(summary = "Get AI triage & RCA effectiveness: helpful rating, acceptance rate, and confidence")
    public ResponseEntity<AiFeedbackMetricsResponse> getAiFeedback(
            @RequestParam(defaultValue = "30") int days
    ) {
        return ResponseEntity.ok(analyticsService.getAiFeedbackMetrics(days));
    }
}
