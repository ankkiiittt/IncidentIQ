package com.incidentiq.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppMetricsTest {

    private MeterRegistry meterRegistry;
    private AppMetrics appMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        appMetrics = new AppMetrics(meterRegistry);
    }

    @Test
    @DisplayName("Should increment incident created metric with tags")
    void shouldIncrementIncidentCreated() {
        appMetrics.incrementIncidentCreated("CRITICAL", "P1", "INFRASTRUCTURE");

        double count = meterRegistry.get(AppMetrics.METRIC_INCIDENTS_CREATED)
                .tag("severity", "CRITICAL")
                .tag("priority", "P1")
                .tag("category", "INFRASTRUCTURE")
                .counter()
                .count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should increment incident resolved metric and record MTTR")
    void shouldIncrementIncidentResolvedAndRecordMttr() {
        appMetrics.incrementIncidentResolved("APPLICATION", 300.0);

        double resolvedCount = meterRegistry.get(AppMetrics.METRIC_INCIDENTS_RESOLVED)
                .tag("category", "APPLICATION")
                .counter()
                .count();

        assertThat(resolvedCount).isEqualTo(1.0);

        double meanMttr = meterRegistry.get(AppMetrics.METRIC_INCIDENTS_MTTR)
                .summary()
                .mean();

        assertThat(meanMttr).isEqualTo(300.0);
    }

    @Test
    @DisplayName("Should increment metrics ingested counter")
    void shouldIncrementMetricsIngested() {
        appMetrics.incrementMetricsIngested(50);
        appMetrics.incrementMetricsIngested(25);

        assertThat(appMetrics.getMetricsIngested().count()).isEqualTo(75.0);
    }

    @Test
    @DisplayName("Should increment prediction created metric with risk level tag")
    void shouldIncrementPredictionCreated() {
        appMetrics.incrementPredictionCreated("HIGH");

        double count = meterRegistry.get(AppMetrics.METRIC_PREDICTIONS_CREATED)
                .tag("riskLevel", "HIGH")
                .counter()
                .count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should record AI diagnostic latency, tokens, and cache hits/misses")
    void shouldRecordAiMetrics() {
        appMetrics.recordAiDiagnosticsDuration(150);
        appMetrics.recordAiTokens(120, 45);
        appMetrics.incrementAiCacheHit();
        appMetrics.incrementAiCacheMiss();

        assertThat(appMetrics.getAiDiagnosticsDuration().count()).isEqualTo(1);
        assertThat(appMetrics.getAiCacheHits().count()).isEqualTo(1.0);
        assertThat(appMetrics.getAiCacheMisses().count()).isEqualTo(1.0);

        double inputTokens = meterRegistry.get(AppMetrics.METRIC_AI_TOKENS)
                .tag("type", "input")
                .counter()
                .count();
        double outputTokens = meterRegistry.get(AppMetrics.METRIC_AI_TOKENS)
                .tag("type", "output")
                .counter()
                .count();

        assertThat(inputTokens).isEqualTo(120.0);
        assertThat(outputTokens).isEqualTo(45.0);
    }
}
