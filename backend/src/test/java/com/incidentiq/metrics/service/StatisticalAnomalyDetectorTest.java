package com.incidentiq.metrics.service;

import com.incidentiq.metrics.dto.MetricBaselineProjection;
import com.incidentiq.metrics.entity.Metric;
import com.incidentiq.metrics.entity.MetricType;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.repository.MetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticalAnomalyDetectorTest {

    @Mock
    private MetricRepository metricRepository;

    private StatisticalAnomalyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new StatisticalAnomalyDetector(metricRepository);
    }

    @Test
    void scoreMetric_cpuSaturation_returnsHighAnomalyScore() {
        double normal = detector.scoreMetric(MetricType.CPU_PERCENT, 45.0, 40.0, 5.0, 1.0);
        assertThat(normal).isLessThan(0.40);

        double high = detector.scoreMetric(MetricType.CPU_PERCENT, 96.0, 40.0, 5.0, 11.2);
        assertThat(high).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void scoreMetric_memoryOomRisk_returnsCriticalScore() {
        double critical = detector.scoreMetric(MetricType.MEMORY_PERCENT, 95.0, 60.0, 10.0, 3.5);
        assertThat(critical).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void scoreMetric_errorRateSpike_returnsCriticalScore() {
        double normal = detector.scoreMetric(MetricType.ERROR_RATE_PERCENT, 0.1, 0.05, 0.02, 2.5);
        assertThat(normal).isLessThan(0.30);

        double spike = detector.scoreMetric(MetricType.ERROR_RATE_PERCENT, 12.0, 0.1, 0.5, 23.8);
        assertThat(spike).isGreaterThanOrEqualTo(0.95);
    }

    @Test
    void scoreMetric_latencySpike_returnsHighAnomalyScore() {
        double spike = detector.scoreMetric(MetricType.LATENCY_P95_MS, 1500.0, 120.0, 30.0, 46.0);
        assertThat(spike).isGreaterThanOrEqualTo(0.90);
    }

    @Test
    void evaluate_anomalousMetrics_producesHighRiskResult() {
        UUID serviceId = UUID.randomUUID();

        Metric cpu = new Metric(null, MetricType.CPU_PERCENT, 97.5, Instant.now());
        Metric err = new Metric(null, MetricType.ERROR_RATE_PERCENT, 15.2, Instant.now());
        when(metricRepository.findLatestByServiceId(serviceId)).thenReturn(List.of(cpu, err));

        MetricBaselineProjection cpuBase = createBaseline(MetricType.CPU_PERCENT.name(), 50.0, 10.0);
        MetricBaselineProjection errBase = createBaseline(MetricType.ERROR_RATE_PERCENT.name(), 0.2, 0.1);
        when(metricRepository.calculateBaselines(eq(serviceId), any())).thenReturn(List.of(cpuBase, errBase));

        StatisticalAnomalyDetector.DetectionResult result = detector.evaluate(serviceId);

        assertThat(result.riskLevel()).isEqualTo(PredictionRiskLevel.HIGH);
        assertThat(result.anomalyScore().doubleValue()).isGreaterThanOrEqualTo(0.80);
        assertThat(result.contributingMetrics()).containsKey("CPU_PERCENT");
        assertThat(result.contributingMetrics()).containsKey("ERROR_RATE_PERCENT");
    }

    private MetricBaselineProjection createBaseline(String metricName, double avg, double stddev) {
        return new MetricBaselineProjection() {
            @Override public String getMetricName() { return metricName; }
            @Override public Long getSampleCount() { return 100L; }
            @Override public Double getAvgValue() { return avg; }
            @Override public Double getStddevValue() { return stddev; }
            @Override public Double getMaxValue() { return avg + 2 * stddev; }
            @Override public Double getMinValue() { return Math.max(0, avg - 2 * stddev); }
        };
    }
}
