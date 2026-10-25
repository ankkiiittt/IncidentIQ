package com.incidentiq.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Centralized Micrometer domain metrics registry for IncidentIQ.
 * Exposes real-time operational and business telemetry to Prometheus Actuator.
 */
@Component
public class AppMetrics {

    public static final String METRIC_INCIDENTS_CREATED = "incidentiq.incidents.created.total";
    public static final String METRIC_INCIDENTS_RESOLVED = "incidentiq.incidents.resolved.total";
    public static final String METRIC_INCIDENTS_MTTR = "incidentiq.incidents.mttr.seconds";
    public static final String METRIC_METRICS_INGESTED = "incidentiq.metrics.ingested.total";
    public static final String METRIC_PREDICTIONS_CREATED = "incidentiq.predictions.created.total";
    public static final String METRIC_AI_DURATION = "incidentiq.ai.diagnostics.duration";
    public static final String METRIC_AI_TOKENS = "incidentiq.ai.diagnostics.tokens";
    public static final String METRIC_AI_CACHE_HITS = "incidentiq.ai.diagnostics.cache.hits";
    public static final String METRIC_AI_CACHE_MISSES = "incidentiq.ai.diagnostics.cache.misses";

    private final MeterRegistry meterRegistry;
    private final Counter aiCacheHits;
    private final Counter aiCacheMisses;
    private final Counter metricsIngested;
    private final Timer aiDiagnosticsDuration;

    public AppMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.aiCacheHits = Counter.builder(METRIC_AI_CACHE_HITS)
                .description("Total number of AI diagnostic cache hits")
                .register(meterRegistry);
        this.aiCacheMisses = Counter.builder(METRIC_AI_CACHE_MISSES)
                .description("Total number of AI diagnostic cache misses")
                .register(meterRegistry);
        this.metricsIngested = Counter.builder(METRIC_METRICS_INGESTED)
                .description("Total count of raw service metrics ingested")
                .register(meterRegistry);
        this.aiDiagnosticsDuration = Timer.builder(METRIC_AI_DURATION)
                .description("Time taken to run AI diagnostic analysis")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

    public void incrementIncidentCreated(String severity, String priority, String category) {
        Counter.builder(METRIC_INCIDENTS_CREATED)
                .description("Total incidents created")
                .tag("severity", severity != null ? severity : "UNKNOWN")
                .tag("priority", priority != null ? priority : "UNKNOWN")
                .tag("category", category != null ? category : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    public void incrementIncidentResolved(String category, double mttrSeconds) {
        Counter.builder(METRIC_INCIDENTS_RESOLVED)
                .description("Total incidents resolved")
                .tag("category", category != null ? category : "UNKNOWN")
                .register(meterRegistry)
                .increment();

        if (mttrSeconds > 0) {
            DistributionSummary.builder(METRIC_INCIDENTS_MTTR)
                    .description("Mean time to resolution in seconds")
                    .publishPercentiles(0.5, 0.95)
                    .register(meterRegistry)
                    .record(mttrSeconds);
        }
    }

    public void incrementMetricsIngested(long count) {
        if (count > 0) {
            metricsIngested.increment(count);
        }
    }

    public void incrementPredictionCreated(String riskLevel) {
        Counter.builder(METRIC_PREDICTIONS_CREATED)
                .description("Total predictive incident risk alerts created")
                .tag("riskLevel", riskLevel != null ? riskLevel : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }

    public void recordAiDiagnosticsDuration(long durationMs) {
        aiDiagnosticsDuration.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public void recordAiTokens(long inputTokens, long outputTokens) {
        if (inputTokens > 0) {
            Counter.builder(METRIC_AI_TOKENS)
                    .tag("type", "input")
                    .register(meterRegistry)
                    .increment(inputTokens);
        }
        if (outputTokens > 0) {
            Counter.builder(METRIC_AI_TOKENS)
                    .tag("type", "output")
                    .register(meterRegistry)
                    .increment(outputTokens);
        }
    }

    public void incrementAiCacheHit() {
        aiCacheHits.increment();
    }

    public void incrementAiCacheMiss() {
        aiCacheMisses.increment();
    }

    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }

    public Counter getAiCacheHits() {
        return aiCacheHits;
    }

    public Counter getAiCacheMisses() {
        return aiCacheMisses;
    }

    public Counter getMetricsIngested() {
        return metricsIngested;
    }

    public Timer getAiDiagnosticsDuration() {
        return aiDiagnosticsDuration;
    }
}
