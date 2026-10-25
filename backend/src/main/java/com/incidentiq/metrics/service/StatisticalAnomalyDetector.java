package com.incidentiq.metrics.service;

import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.metrics.dto.MetricBaselineProjection;
import com.incidentiq.metrics.entity.*;
import com.incidentiq.metrics.repository.MetricRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class StatisticalAnomalyDetector {

    private static final Logger log = LoggerFactory.getLogger(StatisticalAnomalyDetector.class);
    public static final String MODEL_VERSION = "statistical-zscore-v1.0";

    private final MetricRepository metricRepository;

    public StatisticalAnomalyDetector(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public record DetectionResult(
            BigDecimal anomalyScore,
            PredictionRiskLevel riskLevel,
            Map<String, Object> contributingMetrics
    ) {}

    public DetectionResult evaluate(UUID serviceId) {
        Instant since = Instant.now().minus(Duration.ofHours(24));
        List<Metric> latestMetrics = metricRepository.findLatestByServiceId(serviceId);
        if (latestMetrics.isEmpty()) {
            return new DetectionResult(BigDecimal.ZERO, PredictionRiskLevel.LOW, Map.of());
        }

        List<MetricBaselineProjection> baselines = metricRepository.calculateBaselines(serviceId, since);
        Map<String, MetricBaselineProjection> baselineMap = new HashMap<>();
        for (MetricBaselineProjection b : baselines) {
            baselineMap.put(b.getMetricName(), b);
        }

        Map<String, Object> contributing = new LinkedHashMap<>();
        double maxScore = 0.0;
        double secondarySum = 0.0;

        for (Metric metric : latestMetrics) {
            MetricType type = metric.getMetricName();
            double val = metric.getValue();
            MetricBaselineProjection base = baselineMap.get(type.name());

            double mean = (base != null && base.getAvgValue() != null) ? base.getAvgValue() : val;
            double stddev = (base != null && base.getStddevValue() != null) ? base.getStddevValue() : 0.0;
            double zScore = (stddev > 0.001) ? (val - mean) / stddev : 0.0;

            double score = scoreMetric(type, val, mean, stddev, zScore);
            score = Math.min(1.0, Math.max(0.0, score));

            String status = (score >= 0.80) ? "CRITICAL" : (score >= 0.50 ? "ELEVATED" : "NORMAL");

            Map<String, Object> metricInfo = new LinkedHashMap<>();
            metricInfo.put("score", round(score, 3));
            metricInfo.put("value", round(val, 2));
            metricInfo.put("mean", round(mean, 2));
            metricInfo.put("zScore", round(zScore, 2));
            metricInfo.put("status", status);

            contributing.put(type.name(), metricInfo);

            if (score > maxScore) {
                secondarySum += maxScore;
                maxScore = score;
            } else if (score >= 0.40) {
                secondarySum += score;
            }
        }

        // Composite Anomaly Score: max + damping * secondary anomalies
        double composite = Math.min(0.999, maxScore + (0.08 * secondarySum));
        composite = round(composite, 3);
        BigDecimal scoreBd = BigDecimal.valueOf(composite).setScale(3, RoundingMode.HALF_UP);

        PredictionRiskLevel riskLevel;
        if (composite >= 0.80) {
            riskLevel = PredictionRiskLevel.HIGH;
        } else if (composite >= 0.50) {
            riskLevel = PredictionRiskLevel.MEDIUM;
        } else {
            riskLevel = PredictionRiskLevel.LOW;
        }

        return new DetectionResult(scoreBd, riskLevel, contributing);
    }

    public double scoreMetric(MetricType type, double val, double mean, double stddev, double zScore) {
        return switch (type) {
            case CPU_PERCENT -> {
                if (val >= 95.0) yield 0.95;
                if (val >= 85.0) yield 0.80 + 0.15 * ((val - 85.0) / 10.0);
                if (val >= 70.0) yield 0.50 + 0.30 * ((val - 70.0) / 15.0);
                if (zScore >= 2.5) yield Math.min(0.90, 0.50 + 0.15 * (zScore - 2.5));
                yield Math.max(0.0, val / 150.0);
            }
            case MEMORY_PERCENT -> {
                if (val >= 92.0) yield 0.95;
                if (val >= 85.0) yield 0.80 + 0.15 * ((val - 85.0) / 7.0);
                if (val >= 75.0) yield 0.50 + 0.30 * ((val - 75.0) / 10.0);
                if (zScore >= 2.5) yield Math.min(0.90, 0.50 + 0.15 * (zScore - 2.5));
                yield Math.max(0.0, val / 160.0);
            }
            case DISK_PERCENT -> {
                if (val >= 92.0) yield 0.95;
                if (val >= 85.0) yield 0.80 + 0.15 * ((val - 85.0) / 7.0);
                if (val >= 80.0) yield 0.50 + 0.30 * ((val - 80.0) / 5.0);
                yield Math.max(0.0, val / 180.0);
            }
            case ERROR_RATE_PERCENT -> {
                if (val >= 10.0) yield 0.98;
                if (val >= 5.0) yield 0.85 + 0.13 * ((val - 5.0) / 5.0);
                if (val >= 2.0) yield 0.60 + 0.25 * ((val - 2.0) / 3.0);
                if (val >= 0.5) yield 0.30 + 0.30 * ((val - 0.5) / 1.5);
                if (zScore >= 3.0) yield Math.min(0.95, 0.60 + 0.12 * (zScore - 3.0));
                yield 0.0;
            }
            case LATENCY_P95_MS -> {
                if (val >= 1000.0) yield 0.95;
                if (val >= 500.0) yield 0.80 + 0.15 * ((val - 500.0) / 500.0);
                if (val >= 250.0) yield 0.50 + 0.30 * ((val - 250.0) / 250.0);
                if (zScore >= 3.0) yield Math.min(0.95, 0.60 + 0.12 * (zScore - 3.0));
                yield 0.0;
            }
            case THROUGHPUT_RPS -> {
                if (zScore >= 3.5) yield Math.min(0.85, 0.50 + 0.10 * (zScore - 3.5));
                if (zScore <= -3.0) yield 0.70;
                yield 0.0;
            }
        };
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
