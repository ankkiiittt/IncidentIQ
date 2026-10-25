package com.incidentiq.metrics.service;

import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.messaging.service.OutboxService;
import com.incidentiq.metrics.dto.MetricBatchRequest;
import com.incidentiq.metrics.dto.MetricBatchResponse;
import com.incidentiq.metrics.dto.MetricIngestItem;
import com.incidentiq.metrics.dto.PredictionResponse;
import com.incidentiq.metrics.entity.Prediction;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.entity.PredictionSource;
import com.incidentiq.metrics.repository.MetricRepository;
import com.incidentiq.metrics.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional
public class MetricIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MetricIngestionService.class);

    private final MetricRepository metricRepository;
    private final PredictionRepository predictionRepository;
    private final CatalogService catalogService;
    private final StatisticalAnomalyDetector anomalyDetector;
    private final OutboxService outboxService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.incidentiq.notification.service.NotificationBroadcaster notificationBroadcaster;

    public MetricIngestionService(
            MetricRepository metricRepository,
            PredictionRepository predictionRepository,
            CatalogService catalogService,
            StatisticalAnomalyDetector anomalyDetector,
            OutboxService outboxService
    ) {
        this.metricRepository = metricRepository;
        this.predictionRepository = predictionRepository;
        this.catalogService = catalogService;
        this.anomalyDetector = anomalyDetector;
        this.outboxService = outboxService;
    }

    public MetricBatchResponse ingestBatch(MetricBatchRequest request) {
        if (request == null || request.metrics() == null || request.metrics().isEmpty()) {
            return new MetricBatchResponse(0, 0, 0, 0);
        }

        int total = request.metrics().size();
        int insertedCount = 0;
        int duplicates = 0;
        Set<UUID> affectedServices = new HashSet<>();

        for (MetricIngestItem item : request.metrics()) {
            Instant recordedAt = item.recordedAt() != null ? item.recordedAt() : Instant.now();
            try {
                int rows = metricRepository.insertIdempotent(
                        item.serviceId(),
                        item.metricName().name(),
                        item.value(),
                        recordedAt
                );
                if (rows > 0) {
                    insertedCount++;
                    affectedServices.add(item.serviceId());
                } else {
                    duplicates++;
                }
            } catch (Exception e) {
                log.warn("Metric insert conflict or error for service {}: {}", item.serviceId(), e.getMessage());
                duplicates++;
            }
        }

        int anomaliesDetected = 0;
        for (UUID serviceId : affectedServices) {
            try {
                StatisticalAnomalyDetector.DetectionResult result = anomalyDetector.evaluate(serviceId);
                if (result.riskLevel() == PredictionRiskLevel.HIGH || result.riskLevel() == PredictionRiskLevel.MEDIUM) {
                    List<Prediction> recentActive = predictionRepository.findActiveSince(
                            serviceId,
                            Instant.now().minus(Duration.ofMinutes(30))
                    );

                    boolean shouldCreate = recentActive.isEmpty();
                    if (!shouldCreate && result.riskLevel() == PredictionRiskLevel.HIGH) {
                        // If recent active was only MEDIUM, upgrade with a new HIGH prediction
                        shouldCreate = recentActive.stream().noneMatch(p -> p.getRiskLevel() == PredictionRiskLevel.HIGH);
                    }

                    if (shouldCreate) {
                        ServiceEntity service = catalogService.findServiceEntity(serviceId);
                        Prediction prediction = new Prediction(
                                service,
                                result.anomalyScore(),
                                result.riskLevel(),
                                null, // failureProbability null for statistical source
                                30,
                                PredictionSource.STATISTICAL,
                                StatisticalAnomalyDetector.MODEL_VERSION,
                                result.contributingMetrics()
                        );

                        Prediction saved = predictionRepository.save(prediction);
                        anomaliesDetected++;

                        if (notificationBroadcaster != null) {
                            notificationBroadcaster.broadcastPrediction(PredictionResponse.fromEntity(saved));
                        }

                        // Outbox event for failure prediction
                        if (outboxService != null) {
                            UUID predictionId = saved.getId() != null ? saved.getId() : UUID.randomUUID();
                            Map<String, Object> payload = new HashMap<>();
                            payload.put("predictionId", predictionId.toString());
                            payload.put("serviceId", serviceId.toString());
                            payload.put("riskLevel", saved.getRiskLevel().name());
                            payload.put("anomalyScore", saved.getAnomalyScore());
                            outboxService.publish("PREDICTION", predictionId, "failure.prediction.created", payload);
                        }

                        log.warn("Predictive Anomaly Detected: service={}, risk={}, score={}",
                                service.getName(), saved.getRiskLevel(), saved.getAnomalyScore());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to run anomaly detector for service {}: {}", serviceId, e.getMessage(), e);
            }
        }

        return new MetricBatchResponse(total, insertedCount, duplicates, anomaliesDetected);
    }
}
