package com.incidentiq.ai.service;

import com.incidentiq.ai.dto.AiModelOutput;
import com.incidentiq.incident.entity.IncidentCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDeterministicAiClientTest {

    private LocalDeterministicAiClient aiClient;

    @BeforeEach
    void setUp() {
        aiClient = new LocalDeterministicAiClient();
    }

    @Test
    @DisplayName("Generates grounded root causes and actions when evidence chunks are provided")
    void testAnalysisWithEvidence() {
        UUID chunk1 = UUID.randomUUID();
        UUID chunk2 = UUID.randomUUID();

        String prompt = """
                === INCIDENT ===
                Number: INC-0001
                Title: Checkout API failing with 500 errors
                Description: Users unable to purchase; connection timeout in HikariCP
                Severity: CRITICAL
                Priority: P1
                Category: APPLICATION
                Service: checkout-api

                === EVIDENCE ===
                [Chunk: %s] [Score: 0.72] [Title: Database Connection Pool Exhaustion Runbook]
                Snippet: HikariCP connection pool errors and thread starvation. Terminate long running queries.

                [Chunk: %s] [Score: 0.55] [Title: Redis Cache Latency Runbook]
                Snippet: Redis memory threshold exceeded causing key eviction and latency spikes.

                === DEPLOYMENTS ===
                [No deployments in last 24 hours]
                """.formatted(chunk1, chunk2);

        AiModelOutput output = aiClient.completeAnalysis("system", prompt);

        assertThat(output.evidenceSufficient()).isTrue();
        assertThat(output.category()).isEqualTo(IncidentCategory.DATABASE);
        assertThat(output.rootCauses()).isNotEmpty();
        assertThat(output.rootCauses().get(0).evidenceRefs()).contains(chunk1.toString());
        assertThat(output.rootCauses().get(0).confidence()).isGreaterThan(0.70);
        assertThat(output.actions()).isNotEmpty();
        assertThat(output.summary()).isNotBlank().hasSizeLessThan(600);
    }

    @Test
    @DisplayName("Returns INSUFFICIENT_EVIDENCE when evidence block has no matching chunks")
    void testAnalysisWithNoEvidence() {
        String prompt = """
                === INCIDENT ===
                Number: INC-0002
                Title: Unknown obscure issue
                Description: Something strange happened
                Severity: LOW
                Priority: P3
                Category: OTHER
                Service: unknown-service

                === EVIDENCE ===
                [No evidence chunks above threshold]

                === DEPLOYMENTS ===
                [No deployments in last 24 hours]
                """;

        AiModelOutput output = aiClient.completeAnalysis("system", prompt);

        assertThat(output.evidenceSufficient()).isFalse();
        assertThat(output.rootCauses()).isEmpty();
        assertThat(output.actions()).isEmpty();
        assertThat(output.summary()).contains("No relevant operational runbooks");
    }
}
