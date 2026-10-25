package com.incidentiq.metrics.entity;

import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.incident.entity.Incident;
import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "predictions",
        indexes = {
                @Index(name = "idx_predictions_service_created", columnList = "service_id, created_at DESC"),
                @Index(name = "idx_predictions_status", columnList = "status")
        }
)
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(name = "anomaly_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal anomalyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 6)
    private PredictionRiskLevel riskLevel;

    @Column(name = "failure_probability", precision = 4, scale = 3)
    private BigDecimal failureProbability;

    @Column(name = "horizon_minutes", nullable = false)
    private Integer horizonMinutes = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 12)
    private PredictionSource source = PredictionSource.STATISTICAL;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contributing_metrics", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> contributingMetrics = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private PredictionStatus status = PredictionStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Prediction() {}

    public Prediction(ServiceEntity service,
                      BigDecimal anomalyScore,
                      PredictionRiskLevel riskLevel,
                      BigDecimal failureProbability,
                      Integer horizonMinutes,
                      PredictionSource source,
                      String modelVersion,
                      Map<String, Object> contributingMetrics) {
        this.service = service;
        this.anomalyScore = anomalyScore;
        this.riskLevel = riskLevel;
        this.failureProbability = failureProbability;
        this.horizonMinutes = horizonMinutes != null ? horizonMinutes : 30;
        this.source = source != null ? source : PredictionSource.STATISTICAL;
        this.modelVersion = modelVersion;
        this.contributingMetrics = contributingMetrics != null ? contributingMetrics : new HashMap<>();
        this.status = PredictionStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public BigDecimal getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(BigDecimal anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public PredictionRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(PredictionRiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getFailureProbability() {
        return failureProbability;
    }

    public void setFailureProbability(BigDecimal failureProbability) {
        this.failureProbability = failureProbability;
    }

    public Integer getHorizonMinutes() {
        return horizonMinutes;
    }

    public void setHorizonMinutes(Integer horizonMinutes) {
        this.horizonMinutes = horizonMinutes;
    }

    public PredictionSource getSource() {
        return source;
    }

    public void setSource(PredictionSource source) {
        this.source = source;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Map<String, Object> getContributingMetrics() {
        return contributingMetrics;
    }

    public void setContributingMetrics(Map<String, Object> contributingMetrics) {
        this.contributingMetrics = contributingMetrics;
    }

    public PredictionStatus getStatus() {
        return status;
    }

    public void setStatus(PredictionStatus status) {
        this.status = status;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
