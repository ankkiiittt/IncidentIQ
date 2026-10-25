package com.incidentiq.ai.service;

import com.incidentiq.ai.dto.AiModelOutput;
import com.incidentiq.ai.dto.RootCauseHypothesis;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalDeterministicAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(LocalDeterministicAiClient.class);
    private static final String MODEL_NAME = "incidentiq-rca-deterministic-v1";

    private static final Pattern EVIDENCE_CHUNK_PATTERN = Pattern.compile(
            "\\[Chunk: ([0-9a-fA-F-]+)\\]\\s*\\[Score: ([0-9.]+)\\]\\s*\\[Title: ([^\\]]+)\\]\\s*Snippet: ([^\\n]+)",
            Pattern.MULTILINE
    );

    @Override
    public String getModelName() {
        return MODEL_NAME;
    }

    @Override
    public AiModelOutput completeAnalysis(String systemPrompt, String userPrompt) {
        log.debug("Executing local deterministic AI analysis pipeline");

        List<RetrievedChunk> parsedChunks = extractEvidenceChunks(userPrompt);

        if (parsedChunks.isEmpty() || userPrompt.contains("[No evidence chunks above threshold]")) {
            return new AiModelOutput(
                    extractIncidentCategory(userPrompt, IncidentCategory.OTHER),
                    extractIncidentPriority(userPrompt, IncidentPriority.P3),
                    "No relevant operational runbooks or postmortems exceeded the retrieval threshold. Direct manual diagnosis recommended.",
                    false,
                    List.of(),
                    List.of()
            );
        }

        RetrievedChunk topChunk = parsedChunks.get(0);
        IncidentCategory inferredCategory = inferCategory(userPrompt, topChunk);
        IncidentPriority suggestedPriority = inferPriority(userPrompt);

        List<RootCauseHypothesis> rootCauses = new ArrayList<>();
        List<AiModelOutput.ActionRecommendation> actions = new ArrayList<>();

        String lowerPrompt = userPrompt.toLowerCase();
        String lowerTitle = topChunk.title().toLowerCase();
        String lowerSnippet = topChunk.snippet().toLowerCase();

        if (lowerPrompt.contains("connection") || lowerPrompt.contains("pool") || lowerTitle.contains("connection pool") || lowerSnippet.contains("hikaricp")) {
            rootCauses.add(new RootCauseHypothesis(
                    "HikariCP connection pool exhaustion causing thread starvation and cascading HTTP 500 timeouts",
                    Math.min(0.92, Math.max(0.70, topChunk.similarity() + 0.15)),
                    List.of(topChunk.chunkId())
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Inspect and terminate long-running transactions holding table/row locks via pg_stat_activity",
                    "Frees blocked connections immediately without requiring a full cluster restart"
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Temporarily scale connection pool maxLifetime and maximumPoolSize in application config",
                    "Provides immediate buffer for peak query traffic while root queries are optimized"
            ));
        } else if (lowerPrompt.contains("tls") || lowerPrompt.contains("cert") || lowerPrompt.contains("handshake") || lowerTitle.contains("tls") || lowerSnippet.contains("handshake")) {
            rootCauses.add(new RootCauseHypothesis(
                    "SSL/TLS certificate expiration or truststore handshake failure preventing downstream service communication",
                    Math.min(0.94, Math.max(0.75, topChunk.similarity() + 0.12)),
                    List.of(topChunk.chunkId())
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Verify downstream endpoint certificate validity date and issuer truststore chain",
                    "Pinpoints expired intermediate or root certificates causing handshake rejection"
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Rotate or update the active TLS secret in Kubernetes/Secrets Manager and trigger rolling reload",
                    "Restores encrypted connectivity to external gateway"
            ));
        } else if (lowerPrompt.contains("redis") || lowerPrompt.contains("oom") || lowerPrompt.contains("cache") || lowerTitle.contains("redis")) {
            rootCauses.add(new RootCauseHypothesis(
                    "Redis memory ceiling reached under heavy key load causing aggressive eviction and p99 latency degradation",
                    Math.min(0.89, Math.max(0.70, topChunk.similarity() + 0.10)),
                    List.of(topChunk.chunkId())
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Execute Redis MEMORY USAGE and INFO STATS to isolate oversized keys and fragmented memory",
                    "Identifies unevicted hash sets or missing TTL policies"
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Purge stale cache keys and verify volatile-lru eviction policy configuration",
                    "Relieves instantaneous memory pressure on the caching layer"
            ));
        } else {
            rootCauses.add(new RootCauseHypothesis(
                    "Operational degradation matching historical patterns documented in: " + topChunk.title(),
                    Math.min(0.80, Math.max(0.60, topChunk.similarity())),
                    List.of(topChunk.chunkId())
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Follow the diagnostic verification steps outlined in the cited operational runbook",
                    "Ensures systematic incident remediation based on proven team procedures"
            ));
            actions.add(new AiModelOutput.ActionRecommendation(
                    "Check recent deployments and error rate metrics on the affected service",
                    "Correlates failure timeline with configuration changes"
            ));
        }

        if (parsedChunks.size() > 1) {
            RetrievedChunk secondChunk = parsedChunks.get(1);
            rootCauses.add(new RootCauseHypothesis(
                    "Secondary contributing factor identified in: " + secondChunk.title(),
                    Math.min(0.75, Math.max(0.50, secondChunk.similarity())),
                    List.of(secondChunk.chunkId())
            ));
        }

        String summary = String.format(
                "Incident exhibits operational failure patterns consistent with %s. Vector similarity retrieval matched evidence with %.0f%% confidence against runbook '%s'. Immediate diagnostic and remediation steps recommended.",
                inferredCategory.name().toLowerCase(),
                topChunk.similarity() * 100.0,
                topChunk.title()
        );
        if (summary.length() > 590) {
            summary = summary.substring(0, 587) + "...";
        }

        return new AiModelOutput(
                inferredCategory,
                suggestedPriority,
                summary,
                true,
                rootCauses,
                actions
        );
    }

    private List<RetrievedChunk> extractEvidenceChunks(String prompt) {
        List<RetrievedChunk> chunks = new ArrayList<>();
        Matcher matcher = EVIDENCE_CHUNK_PATTERN.matcher(prompt);
        while (matcher.find()) {
            String chunkId = matcher.group(1);
            double score = Double.parseDouble(matcher.group(2));
            String title = matcher.group(3).trim();
            String snippet = matcher.group(4).trim();
            chunks.add(new RetrievedChunk(chunkId, score, title, snippet));
        }
        return chunks;
    }

    private IncidentCategory inferCategory(String prompt, RetrievedChunk topChunk) {
        String combined = (prompt + " " + topChunk.title() + " " + topChunk.snippet()).toLowerCase();
        if (combined.contains("database") || combined.contains("postgres") || combined.contains("hikaricp") || combined.contains("sql") || combined.contains("query")) {
            return IncidentCategory.DATABASE;
        }
        if (combined.contains("tls") || combined.contains("cert") || combined.contains("security") || combined.contains("unauthorized")) {
            return IncidentCategory.SECURITY;
        }
        if (combined.contains("network") || combined.contains("dns") || combined.contains("connection refused") || combined.contains("packet")) {
            return IncidentCategory.NETWORK;
        }
        if (combined.contains("redis") || combined.contains("cpu") || combined.contains("memory") || combined.contains("oom") || combined.contains("disk")) {
            return IncidentCategory.INFRASTRUCTURE;
        }
        if (combined.contains("deploy") || combined.contains("release") || combined.contains("rollback")) {
            return IncidentCategory.DEPLOYMENT;
        }
        return IncidentCategory.APPLICATION;
    }

    private IncidentPriority inferPriority(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("p1") || lower.contains("critical") || lower.contains("outage") || lower.contains("100%") || lower.contains("global")) {
            return IncidentPriority.P1;
        }
        if (lower.contains("high") || lower.contains("latency") || lower.contains("500") || lower.contains("p2")) {
            return IncidentPriority.P2;
        }
        return IncidentPriority.P3;
    }

    private IncidentCategory extractIncidentCategory(String prompt, IncidentCategory fallback) {
        Pattern p = Pattern.compile("Category:\\s*([A-Za-z_]+)");
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            try {
                return IncidentCategory.valueOf(m.group(1).trim());
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private IncidentPriority extractIncidentPriority(String prompt, IncidentPriority fallback) {
        Pattern p = Pattern.compile("Priority:\\s*([A-Za-z0-9]+)");
        Matcher m = p.matcher(prompt);
        if (m.find()) {
            try {
                return IncidentPriority.valueOf(m.group(1).trim());
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private record RetrievedChunk(String chunkId, double similarity, String title, String snippet) {}
}
