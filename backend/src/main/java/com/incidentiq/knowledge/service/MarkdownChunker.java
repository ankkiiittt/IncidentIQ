package com.incidentiq.knowledge.service;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MarkdownChunker {

    private static final int TARGET_CHUNK_TOKENS = 500;
    private static final int OVERLAP_TOKENS = 50;
    private static final int CHARS_PER_TOKEN = 4;

    private static final int TARGET_CHUNK_CHARS = TARGET_CHUNK_TOKENS * CHARS_PER_TOKEN; // ~2000 chars
    private static final int OVERLAP_CHARS = OVERLAP_TOKENS * CHARS_PER_TOKEN; // ~200 chars

    public record ChunkResult(
            int chunkIndex,
            int tokenCount,
            String content,
            String embeddingText,
            Map<String, Object> metadata
    ) {}

    public List<ChunkResult> chunkDocument(String documentTitle, String markdownContent) {
        if (markdownContent == null || markdownContent.isBlank()) {
            return List.of();
        }

        List<ChunkResult> results = new ArrayList<>();
        String[] lines = markdownContent.split("\\r?\\n");

        List<String> headingStack = new ArrayList<>();
        StringBuilder currentSection = new StringBuilder();
        String currentHeadingPath = documentTitle;

        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.startsWith("#")) {
                // If we have content accumulated, process it
                if (!currentSection.isEmpty()) {
                    splitSectionIntoChunks(documentTitle, currentHeadingPath, currentSection.toString(), results);
                    currentSection.setLength(0);
                }

                // Parse heading level
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                String headingText = trimmed.substring(level).trim();

                // Update heading stack
                while (headingStack.size() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(headingText);

                currentHeadingPath = documentTitle + " > " + String.join(" > ", headingStack);
            } else {
                currentSection.append(rawLine).append("\n");
            }
        }

        // Process final accumulated section
        if (!currentSection.isEmpty()) {
            splitSectionIntoChunks(documentTitle, currentHeadingPath, currentSection.toString(), results);
        }

        // If no chunks were created (e.g. document had only whitespace or headings without text),
        // fallback to document title as single chunk
        if (results.isEmpty() && !markdownContent.isBlank()) {
            splitSectionIntoChunks(documentTitle, documentTitle, markdownContent, results);
        }

        // Renumber chunk indices sequentially
        List<ChunkResult> reindexed = new ArrayList<>(results.size());
        for (int i = 0; i < results.size(); i++) {
            ChunkResult c = results.get(i);
            reindexed.add(new ChunkResult(i, c.tokenCount(), c.content(), c.embeddingText(), c.metadata()));
        }

        return reindexed;
    }

    private void splitSectionIntoChunks(String documentTitle,
                                        String headingPath,
                                        String text,
                                        List<ChunkResult> accumulator) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        if (trimmed.length() <= TARGET_CHUNK_CHARS) {
            int tokenCount = Math.max(1, trimmed.length() / CHARS_PER_TOKEN);
            String embeddingText = documentTitle + "\n" + headingPath + "\n" + trimmed;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("headingPath", headingPath);
            metadata.put("documentTitle", documentTitle);

            accumulator.add(new ChunkResult(accumulator.size(), tokenCount, trimmed, embeddingText, metadata));
            return;
        }

        // Chunk with sliding window
        int start = 0;
        while (start < trimmed.length()) {
            int end = Math.min(start + TARGET_CHUNK_CHARS, trimmed.length());

            // If not at the end of the text, try to break at a newline or space
            if (end < trimmed.length()) {
                int lastBreak = trimmed.lastIndexOf("\n", end);
                if (lastBreak > start + (TARGET_CHUNK_CHARS / 2)) {
                    end = lastBreak;
                } else {
                    int lastSpace = trimmed.lastIndexOf(" ", end);
                    if (lastSpace > start + (TARGET_CHUNK_CHARS / 2)) {
                        end = lastSpace;
                    }
                }
            }

            String chunkContent = trimmed.substring(start, end).trim();
            if (!chunkContent.isEmpty()) {
                int tokenCount = Math.max(1, chunkContent.length() / CHARS_PER_TOKEN);
                String embeddingText = documentTitle + "\n" + headingPath + "\n" + chunkContent;
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("headingPath", headingPath);
                metadata.put("documentTitle", documentTitle);

                accumulator.add(new ChunkResult(accumulator.size(), tokenCount, chunkContent, embeddingText, metadata));
            }

            if (end >= trimmed.length()) {
                break;
            }

            // Slide forward by window minus overlap
            start = Math.max(end - OVERLAP_CHARS, start + 1);
        }
    }
}
