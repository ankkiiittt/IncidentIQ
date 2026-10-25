package com.incidentiq.knowledge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalDeterministicEmbeddingServiceTest {

    private final LocalDeterministicEmbeddingService embeddingService = new LocalDeterministicEmbeddingService();

    @Test
    @DisplayName("Generates 384-dimensional unit normalized vectors")
    void testVectorDimensionsAndNormalization() {
        float[] vector = embeddingService.embed("Database connection pool timeout on checkout service");

        assertEquals(384, vector.length);

        float sumSquares = 0.0f;
        for (float v : vector) {
            sumSquares += v * v;
        }

        assertEquals(1.0f, sumSquares, 0.001f, "Vector must be L2 normalized to unit length");
    }

    @Test
    @DisplayName("Deterministic outputs for identical inputs")
    void testDeterminism() {
        String text = "Downstream TLS certificate expired";
        float[] v1 = embeddingService.embed(text);
        float[] v2 = embeddingService.embed(text);

        assertArrayEquals(v1, v2, 0.00001f);
    }

    @Test
    @DisplayName("Semantically overlapping texts have higher cosine similarity than unrelated texts")
    void testSemanticSimilarity() {
        float[] dbIncident = embeddingService.embed("PostgreSQL database connection pool exhausted timeout");
        float[] dbRunbook = embeddingService.embed("Database connection pool failover HikariCP timeout");
        float[] uiDoc = embeddingService.embed("Frontend button color CSS styling typography");

        double similarityDb = cosineSimilarity(dbIncident, dbRunbook);
        double similarityUi = cosineSimilarity(dbIncident, uiDoc);

        assertTrue(similarityDb > similarityUi,
                "DB incident should be more similar to DB runbook (" + similarityDb + ") than UI doc (" + similarityUi + ")");
        assertTrue(similarityDb > 0.40, "Expected significant similarity for related texts: " + similarityDb);
    }

    @Test
    @DisplayName("Vector to string formatting produces valid pgvector syntax")
    void testVectorStringFormatting() {
        float[] v = new float[] {0.1f, -0.5f, 0.9f};
        String str = embeddingService.toVectorString(v);

        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
        assertTrue(str.contains("0.1"));
        assertTrue(str.contains("-0.5"));
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
        }
        return dot;
    }
}
