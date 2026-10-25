package com.incidentiq.metrics.controller;

import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.metrics.dto.MetricBatchRequest;
import com.incidentiq.metrics.dto.MetricBatchResponse;
import com.incidentiq.metrics.service.MetricIngestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    private final MetricIngestionService metricIngestionService;
    private final String ingestApiKey;

    public MetricController(
            MetricIngestionService metricIngestionService,
            @Value("${incidentiq.ingest-api-key:test-ingest-key-12345}") String ingestApiKey
    ) {
        this.metricIngestionService = metricIngestionService;
        this.ingestApiKey = ingestApiKey;
    }

    @PostMapping("/batch")
    public ResponseEntity<MetricBatchResponse> ingestBatch(
            @Valid @RequestBody MetricBatchRequest request,
            @RequestHeader(value = "X-Ingest-Key", required = false) String providedKey
    ) {
        // Authenticate via either X-Ingest-Key or Spring Security (ADMIN / ENGINEER)
        boolean keyMatches = providedKey != null && !providedKey.isBlank() && providedKey.equals(ingestApiKey);
        if (!keyMatches) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean hasRole = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_ENGINEER"));

            if (!hasRole) {
                throw new AppException(ErrorCode.FORBIDDEN, "Valid X-Ingest-Key or ENGINEER/ADMIN role required for metrics ingestion");
            }
        }

        MetricBatchResponse response = metricIngestionService.ingestBatch(request);
        return ResponseEntity.ok(response);
    }
}
