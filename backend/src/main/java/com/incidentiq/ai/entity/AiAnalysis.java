package com.incidentiq.ai.entity;

import com.incidentiq.ai.dto.EvidenceItem;
import com.incidentiq.ai.dto.RootCauseHypothesis;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentCategory;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "ai_analyses",
        indexes = {
                @Index(name = "idx_ai_analyses_incident_created", columnList = "incident_id, created_at DESC"),
                @Index(name = "idx_ai_analyses_status", columnList = "status")
        }
)
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", length = 6, nullable = false)
    private AnalysisTrigger trigger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 22, nullable = false)
    private AnalysisStatus status;

    @Column(name = "model", length = 100, nullable = false)
    private String model;

    @Column(name = "embedding_model", length = 100, nullable = false)
    private String embeddingModel;

    @Column(name = "prompt_version", length = 50, nullable = false)
    private String promptVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 20)
    private IncidentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_priority", length = 2)
    private IncidentPriority suggestedPriority;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "confidence", precision = 3, scale = 2)
    private BigDecimal confidence;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "root_causes", columnDefinition = "jsonb", nullable = false)
    private List<RootCauseHypothesis> rootCauses = new ArrayList<>();

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", columnDefinition = "jsonb", nullable = false)
    private List<EvidenceItem> evidence = new ArrayList<>();

    @Column(name = "evidence_sufficient", nullable = false)
    private boolean evidenceSufficient = false;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "correlation_id", length = 64, nullable = false)
    private String correlationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<Recommendation> recommendations = new ArrayList<>();

    protected AiAnalysis() {}

    public AiAnalysis(Incident incident,
                      AnalysisTrigger trigger,
                      User requestedBy,
                      String model,
                      String embeddingModel,
                      String promptVersion,
                      String correlationId) {
        this.incident = incident;
        this.trigger = trigger;
        this.requestedBy = requestedBy;
        this.status = AnalysisStatus.PENDING;
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.promptVersion = promptVersion;
        this.correlationId = correlationId != null ? correlationId : "unknown";
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public AnalysisTrigger getTrigger() {
        return trigger;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public String getModel() {
        return model;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public IncidentCategory getCategory() {
        return category;
    }

    public void setCategory(IncidentCategory category) {
        this.category = category;
    }

    public IncidentPriority getSuggestedPriority() {
        return suggestedPriority;
    }

    public void setSuggestedPriority(IncidentPriority suggestedPriority) {
        this.suggestedPriority = suggestedPriority;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public List<RootCauseHypothesis> getRootCauses() {
        return rootCauses;
    }

    public void setRootCauses(List<RootCauseHypothesis> rootCauses) {
        this.rootCauses = rootCauses != null ? rootCauses : new ArrayList<>();
    }

    public List<EvidenceItem> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<EvidenceItem> evidence) {
        this.evidence = evidence != null ? evidence : new ArrayList<>();
    }

    public boolean isEvidenceSufficient() {
        return evidenceSufficient;
    }

    public void setEvidenceSufficient(boolean evidenceSufficient) {
        this.evidenceSufficient = evidenceSufficient;
    }

    public Integer getInputTokens() {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
        this.outputTokens = outputTokens;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations.clear();
        if (recommendations != null) {
            this.recommendations.addAll(recommendations);
        }
    }
}
