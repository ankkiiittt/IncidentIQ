package com.incidentiq.knowledge.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownChunkerTest {

    private final MarkdownChunker chunker = new MarkdownChunker();

    @Test
    @DisplayName("Heading-aware chunking parses headings into metadata and generates chunks")
    void testHeadingAwareChunking() {
        String markdown = """
                # Database Runbook

                ## Prerequisites
                Verify access to primary database node and cluster monitoring.

                ## Failover Procedures
                ### Step 1: Check Repl Lag
                Inspect replication lag on secondary read replicas.

                ### Step 2: Promote Secondary
                Execute promotion command on the standby node with zero lag.
                """;

        List<MarkdownChunker.ChunkResult> chunks = chunker.chunkDocument("Database Runbook", markdown);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 3);

        MarkdownChunker.ChunkResult first = chunks.get(0);
        assertEquals(0, first.chunkIndex());
        assertTrue(first.content().contains("Verify access"));
        assertTrue(first.embeddingText().startsWith("Database Runbook"));
        assertTrue(first.metadata().get("headingPath").toString().contains("Prerequisites"));

        // Verify later chunk contains nested heading hierarchy
        boolean foundStep2 = chunks.stream()
                .anyMatch(c -> c.metadata().get("headingPath").toString().contains("Failover Procedures > Step 2: Promote Secondary"));
        assertTrue(foundStep2, "Expected nested heading path in chunk metadata");
    }

    @Test
    @DisplayName("Sliding window splits long sections exceeding target token size")
    void testLongSectionSplitting() {
        StringBuilder longContent = new StringBuilder("# Large Document\n\n## Big Section\n");
        for (int i = 0; i < 600; i++) {
            longContent.append("word").append(i).append(" and some technical instructions ");
        }

        List<MarkdownChunker.ChunkResult> chunks = chunker.chunkDocument("Large Document", longContent.toString());

        assertTrue(chunks.size() > 1, "Expected large section to be split into multiple chunks");
        for (MarkdownChunker.ChunkResult c : chunks) {
            assertTrue(c.tokenCount() > 0);
            assertNotNull(c.embeddingText());
        }
    }

    @Test
    @DisplayName("Empty or blank text produces empty chunk list")
    void testBlankContent() {
        List<MarkdownChunker.ChunkResult> chunks = chunker.chunkDocument("Title", "   \n  ");
        assertTrue(chunks.isEmpty());
    }
}
