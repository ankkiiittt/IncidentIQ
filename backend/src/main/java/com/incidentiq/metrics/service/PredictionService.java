package com.incidentiq.metrics.service;

import com.incidentiq.audit.service.AuditService;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.dto.CreateIncidentRequest;
import com.incidentiq.incident.dto.IncidentDetailResponse;
import com.incidentiq.incident.entity.*;
import com.incidentiq.incident.repository.IncidentEventRepository;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.incident.service.IncidentService;
import com.incidentiq.metrics.dto.CreateIncidentFromPredictionRequest;
import com.incidentiq.metrics.dto.PredictionResponse;
import com.incidentiq.metrics.entity.Prediction;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.entity.PredictionStatus;
import com.incidentiq.metrics.repository.PredictionRepository;
import com.incidentiq.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class PredictionService {

    private final PredictionRepository predictionRepository;
    private final IncidentService incidentService;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final AuditService auditService;

    public PredictionService(
            PredictionRepository predictionRepository,
            IncidentService incidentService,
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            AuditService auditService
    ) {
        this.predictionRepository = predictionRepository;
        this.incidentService = incidentService;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PredictionResponse> getActivePredictions(UUID serviceId, PredictionRiskLevel riskLevel) {
        List<Prediction> predictions;
        if (serviceId != null) {
            predictions = predictionRepository.findByServiceIdAndStatusOrderByCreatedAtDesc(
                    serviceId,
                    PredictionStatus.ACTIVE
            );
        } else if (riskLevel != null) {
            predictions = predictionRepository.findByStatusAndRiskLevelOrderByCreatedAtDesc(
                    PredictionStatus.ACTIVE,
                    riskLevel
            );
        } else {
            predictions = predictionRepository.findAllWithServiceByStatus(PredictionStatus.ACTIVE);
        }
        return predictions.stream().map(PredictionResponse::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public PredictionResponse getPrediction(UUID predictionId) {
        Prediction p = predictionRepository.findByIdWithService(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Prediction not found: " + predictionId));
        return PredictionResponse.fromEntity(p);
    }

    public PredictionResponse dismissPrediction(UUID predictionId, User actor, String ipAddress) {
        Prediction prediction = predictionRepository.findByIdWithService(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Prediction not found: " + predictionId));

        if (prediction.getStatus() != PredictionStatus.ACTIVE) {
            throw new AppException(ErrorCode.CONFLICT, "Prediction is not active: " + prediction.getStatus());
        }

        prediction.setStatus(PredictionStatus.DISMISSED);
        Prediction saved = predictionRepository.save(prediction);

        auditService.record(
                actor != null ? actor.getId() : null,
                "PREDICTION_DISMISSED",
                "PREDICTION",
                saved.getId().toString(),
                Map.of(
                        "serviceId", saved.getService().getId().toString(),
                        "serviceName", saved.getService().getName(),
                        "riskLevel", saved.getRiskLevel().name()
                ),
                ipAddress
        );

        return PredictionResponse.fromEntity(saved);
    }

    public IncidentDetailResponse createIncidentFromPrediction(
            UUID predictionId,
            CreateIncidentFromPredictionRequest request,
            User actor,
            String ipAddress
    ) {
        Prediction prediction = predictionRepository.findByIdWithService(predictionId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Prediction not found: " + predictionId));

        if (prediction.getStatus() != PredictionStatus.ACTIVE) {
            throw new AppException(ErrorCode.CONFLICT, "Prediction is not active: " + prediction.getStatus());
        }

        ServiceEntity service = prediction.getService();

        // Determine title
        String title = (request != null && request.title() != null && !request.title().isBlank())
                ? request.title()
                : String.format("Predicted Failure Risk on %s (%s Anomaly, score: %s)",
                service.getName(), prediction.getRiskLevel(), prediction.getAnomalyScore());

        // Determine description
        String description;
        if (request != null && request.description() != null && !request.description().isBlank()) {
            description = request.description();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("Proactive incident drafted by Predictive Risk Engine.\n\n");
            sb.append("• Service: ").append(service.getName()).append(" (").append(service.getEnvironment()).append(")\n");
            sb.append("• Risk Level: ").append(prediction.getRiskLevel()).append("\n");
            sb.append("• Anomaly Score: ").append(prediction.getAnomalyScore()).append(" (scale 0 to 1)\n");
            sb.append("• Horizon: ").append(prediction.getHorizonMinutes()).append(" minutes\n");
            sb.append("• Model: ").append(prediction.getModelVersion()).append(" (").append(prediction.getSource()).append(")\n\n");
            sb.append("Contributing Telemetry Metrics:\n");
            if (prediction.getContributingMetrics() != null) {
                prediction.getContributingMetrics().forEach((k, v) ->
                        sb.append("  - ").append(k).append(": ").append(v).append("\n"));
            }
            description = sb.toString();
        }

        // Determine priority and severity
        IncidentPriority priority;
        if (request != null && request.priority() != null) {
            priority = request.priority();
        } else {
            priority = (prediction.getRiskLevel() == PredictionRiskLevel.HIGH) ? IncidentPriority.P2 : IncidentPriority.P3;
        }

        IncidentSeverity severity;
        if (request != null && request.severity() != null) {
            severity = request.severity();
        } else {
            severity = (prediction.getRiskLevel() == PredictionRiskLevel.HIGH) ? IncidentSeverity.HIGH : IncidentSeverity.MEDIUM;
        }

        IncidentCategory category = deduceCategory(prediction.getContributingMetrics());

        CreateIncidentRequest createReq = new CreateIncidentRequest(
                title,
                description,
                severity,
                priority,
                category,
                service.getId(),
                List.of("predictive-alert", "telemetry-anomaly", service.getName().toLowerCase()),
                null,
                null
        );

        IncidentDetailResponse createdIncident = incidentService.createIncident(createReq, actor, ipAddress);

        // Resolve incident entity
        Incident incidentEntity = incidentRepository.findById(createdIncident.id())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Created incident not found: " + createdIncident.id()));

        // Link prediction
        prediction.setIncident(incidentEntity);
        prediction.setStatus(PredictionStatus.RESOLVED);
        predictionRepository.save(prediction);

        // Record PREDICTION_LINKED event in incident timeline
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("predictionId", prediction.getId().toString());
        eventPayload.put("anomalyScore", prediction.getAnomalyScore());
        eventPayload.put("riskLevel", prediction.getRiskLevel().name());
        eventPayload.put("modelVersion", prediction.getModelVersion());

        incidentEventRepository.save(new IncidentEvent(
                incidentEntity,
                IncidentEventType.PREDICTION_LINKED,
                actor,
                eventPayload
        ));

        // Audit log
        auditService.record(
                actor != null ? actor.getId() : null,
                "INCIDENT_CREATED_FROM_PREDICTION",
                "PREDICTION",
                prediction.getId().toString(),
                Map.of(
                        "incidentId", createdIncident.id().toString(),
                        "incidentNumber", createdIncident.formattedNumber(),
                        "serviceId", service.getId().toString(),
                        "riskLevel", prediction.getRiskLevel().name()
                ),
                ipAddress
        );

        return createdIncident;
    }

    private IncidentCategory deduceCategory(Map<String, Object> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return IncidentCategory.INFRASTRUCTURE;
        }
        if (metrics.containsKey("DATABASE_PERCENT") || metrics.containsKey("DATABASE")) {
            return IncidentCategory.DATABASE;
        }
        if (metrics.containsKey("ERROR_RATE_PERCENT") || metrics.containsKey("LATENCY_P95_MS")) {
            return IncidentCategory.APPLICATION;
        }
        return IncidentCategory.INFRASTRUCTURE;
    }
}
