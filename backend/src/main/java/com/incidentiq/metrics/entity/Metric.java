package com.incidentiq.metrics.entity;

import com.incidentiq.catalog.entity.ServiceEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "metrics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_metrics_service_metric_time", columnNames = {"service_id", "metric_name", "recorded_at"})
        },
        indexes = {
                @Index(name = "idx_metrics_service_metric_recorded", columnList = "service_id, metric_name, recorded_at DESC")
        }
)
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_name", nullable = false, length = 25)
    private MetricType metricName;

    @Column(nullable = false)
    private Double value;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected Metric() {}

    public Metric(ServiceEntity service, MetricType metricName, Double value, Instant recordedAt) {
        this.service = service;
        this.metricName = metricName;
        this.value = value;
        this.recordedAt = recordedAt;
    }

    public Long getId() {
        return id;
    }

    public ServiceEntity getService() {
        return service;
    }

    public void setService(ServiceEntity service) {
        this.service = service;
    }

    public MetricType getMetricName() {
        return metricName;
    }

    public void setMetricName(MetricType metricName) {
        this.metricName = metricName;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
