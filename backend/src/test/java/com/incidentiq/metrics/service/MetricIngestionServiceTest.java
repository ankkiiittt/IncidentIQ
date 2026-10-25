package com.incidentiq.metrics.service;

import com.incidentiq.catalog.entity.ServiceCriticality;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.entity.ServiceEnvironment;
import com.incidentiq.catalog.service.CatalogService;
import com.incidentiq.messaging.service.OutboxService;
import com.incidentiq.metrics.dto.MetricBatchRequest;
import com.incidentiq.metrics.dto.MetricBatchResponse;
import com.incidentiq.metrics.dto.MetricIngestItem;
import com.incidentiq.metrics.entity.*;
import com.incidentiq.metrics.repository.MetricRepository;
import com.incidentiq.metrics.repository.PredictionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricIngestionServiceTest {

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private PredictionRepository predictionRepository;

    @Mock
    private CatalogService catalogService;

    @Mock
    private StatisticalAnomalyDetector anomalyDetector;

    @Mock
    private OutboxService outboxService;

    private MetricIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new MetricIngestionService(
                metricRepository,
                predictionRepository,
                catalogService,
                anomalyDetector,
                outboxService
        );
    }

    @Test
    void ingestBatch_insertsMetricsAndTriggersAnomalyDetection() {
        UUID serviceId = UUID.randomUUID();
        Instant now = Instant.now();

        List<MetricIngestItem> items = List.of(
                new MetricIngestItem(serviceId, MetricType.CPU_PERCENT, 98.0, now),
                new MetricIngestItem(serviceId, MetricType.ERROR_RATE_PERCENT, 15.0, now)
        );

        when(metricRepository.insertIdempotent(eq(serviceId), eq(MetricType.CPU_PERCENT.name()), eq(98.0), eq(now)))
                .thenReturn(1);
        when(metricRepository.insertIdempotent(eq(serviceId), eq(MetricType.ERROR_RATE_PERCENT.name()), eq(15.0), eq(now)))
                .thenReturn(1);

        ServiceEntity service = new ServiceEntity();
        service.setName("checkout-api");
        service.setEnvironment(ServiceEnvironment.PROD);
        service.setCriticality(ServiceCriticality.CRITICAL);
        when(catalogService.findServiceEntity(serviceId)).thenReturn(service);

        StatisticalAnomalyDetector.DetectionResult detection = new StatisticalAnomalyDetector.DetectionResult(
                BigDecimal.valueOf(0.92),
                PredictionRiskLevel.HIGH,
                Map.of("CPU_PERCENT", Map.of("score", 0.95))
        );
        when(anomalyDetector.evaluate(serviceId)).thenReturn(detection);
        when(predictionRepository.findActiveSince(eq(serviceId), any())).thenReturn(List.of());

        Prediction savedPrediction = new Prediction(
                service,
                BigDecimal.valueOf(0.92),
                PredictionRiskLevel.HIGH,
                null,
                30,
                PredictionSource.STATISTICAL,
                "v1",
                Map.of()
        );
        when(predictionRepository.save(any(Prediction.class))).thenReturn(savedPrediction);

        MetricBatchResponse response = ingestionService.ingestBatch(new MetricBatchRequest(items));

        assertThat(response.totalSubmitted()).isEqualTo(2);
        assertThat(response.inserted()).isEqualTo(2);
        assertThat(response.duplicatesSkipped()).isEqualTo(0);
        assertThat(response.anomaliesDetected()).isEqualTo(1);

        verify(predictionRepository, times(1)).save(any(Prediction.class));
        verify(outboxService, times(1)).publish(eq("PREDICTION"), any(), eq("failure.prediction.created"), any());
    }

    @Test
    void ingestBatch_duplicateHandling_countsDuplicatesSkipped() {
        UUID serviceId = UUID.randomUUID();
        Instant now = Instant.now();

        List<MetricIngestItem> items = List.of(
                new MetricIngestItem(serviceId, MetricType.CPU_PERCENT, 50.0, now)
        );

        // 0 rows returned indicates duplicate
        when(metricRepository.insertIdempotent(eq(serviceId), eq(MetricType.CPU_PERCENT.name()), eq(50.0), eq(now)))
                .thenReturn(0);

        MetricBatchResponse response = ingestionService.ingestBatch(new MetricBatchRequest(items));

        assertThat(response.totalSubmitted()).isEqualTo(1);
        assertThat(response.inserted()).isEqualTo(0);
        assertThat(response.duplicatesSkipped()).isEqualTo(1);
        assertThat(response.anomaliesDetected()).isEqualTo(0);

        verify(anomalyDetector, never()).evaluate(any());
    }
}
