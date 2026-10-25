package com.incidentiq.metrics.service;

import com.incidentiq.catalog.entity.DeploymentEntity;
import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.repository.DeploymentRepository;
import com.incidentiq.catalog.repository.ServiceRepository;
import com.incidentiq.common.error.AppException;
import com.incidentiq.common.error.ErrorCode;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentStatus;
import com.incidentiq.incident.repository.IncidentRepository;
import com.incidentiq.metrics.dto.*;
import com.incidentiq.metrics.entity.*;
import com.incidentiq.metrics.repository.MetricRepository;
import com.incidentiq.metrics.repository.PredictionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ServiceHealthService {

    private static final Set<IncidentStatus> ACTIVE_INCIDENT_STATUSES = Set.of(
            IncidentStatus.OPEN,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS
    );

    private final ServiceRepository serviceRepository;
    private final MetricRepository metricRepository;
    private final PredictionRepository predictionRepository;
    private final DeploymentRepository deploymentRepository;
    private final IncidentRepository incidentRepository;

    public ServiceHealthService(
            ServiceRepository serviceRepository,
            MetricRepository metricRepository,
            PredictionRepository predictionRepository,
            DeploymentRepository deploymentRepository,
            IncidentRepository incidentRepository
    ) {
        this.serviceRepository = serviceRepository;
        this.metricRepository = metricRepository;
        this.predictionRepository = predictionRepository;
        this.deploymentRepository = deploymentRepository;
        this.incidentRepository = incidentRepository;
    }

    public List<ServiceHealthSummary> getAllServicesHealth() {
        List<ServiceEntity> services = serviceRepository.findAll();
        List<ServiceHealthSummary> summaries = new ArrayList<>();

        for (ServiceEntity service : services) {
            if (!service.isActive()) {
                continue;
            }
            List<Metric> latestMetricsList = metricRepository.findLatestByServiceId(service.getId());
            Map<String, Double> metricMap = new LinkedHashMap<>();
            Instant lastRecorded = null;

            for (Metric m : latestMetricsList) {
                metricMap.put(m.getMetricName().name(), m.getValue());
                if (lastRecorded == null || m.getRecordedAt().isAfter(lastRecorded)) {
                    lastRecorded = m.getRecordedAt();
                }
            }

            long openIncidents = incidentRepository.countByServiceIdAndStatusIn(service.getId(), ACTIVE_INCIDENT_STATUSES);
            List<Prediction> activePredictions = predictionRepository.findByServiceIdAndStatusOrderByCreatedAtDesc(
                    service.getId(),
                    PredictionStatus.ACTIVE
            );

            PredictionRiskLevel highestRisk = PredictionRiskLevel.LOW;
            for (Prediction p : activePredictions) {
                if (p.getRiskLevel() == PredictionRiskLevel.HIGH) {
                    highestRisk = PredictionRiskLevel.HIGH;
                    break;
                } else if (p.getRiskLevel() == PredictionRiskLevel.MEDIUM && highestRisk == PredictionRiskLevel.LOW) {
                    highestRisk = PredictionRiskLevel.MEDIUM;
                }
            }

            String healthStatus = calculateHealthStatus(openIncidents, highestRisk);

            summaries.add(new ServiceHealthSummary(
                    service.getId(),
                    service.getName(),
                    service.getEnvironment().name(),
                    service.getOwnerTeam(),
                    service.getCriticality().name(),
                    healthStatus,
                    highestRisk,
                    openIncidents,
                    activePredictions.size(),
                    metricMap,
                    lastRecorded
            ));
        }

        return summaries;
    }

    public ServiceHealthDetailResponse getServiceHealth(UUID serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Service not found: " + serviceId));

        Instant since = Instant.now().minus(Duration.ofHours(24));
        List<Metric> metrics24h = metricRepository.findByServiceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(serviceId, since);

        Map<MetricType, List<Metric>> grouped = new EnumMap<>(MetricType.class);
        for (MetricType t : MetricType.values()) {
            grouped.put(t, new ArrayList<>());
        }
        for (Metric m : metrics24h) {
            grouped.get(m.getMetricName()).add(m);
        }

        List<MetricSeries> seriesList = new ArrayList<>();
        for (MetricType type : MetricType.values()) {
            List<Metric> list = grouped.get(type);
            List<MetricDataPoint> points = list.stream()
                    .map(m -> new MetricDataPoint(m.getRecordedAt(), m.getValue()))
                    .toList();

            Double current = list.isEmpty() ? null : list.get(list.size() - 1).getValue();
            DoubleSummaryStatistics stats = list.stream().mapToDouble(Metric::getValue).summaryStatistics();
            Double min = list.isEmpty() ? null : round(stats.getMin(), 2);
            Double max = list.isEmpty() ? null : round(stats.getMax(), 2);
            Double avg = list.isEmpty() ? null : round(stats.getAverage(), 2);

            String displayName = formatMetricDisplayName(type);
            String unit = getMetricUnit(type);

            seriesList.add(new MetricSeries(type, displayName, unit, current, min, max, avg, points));
        }

        List<Prediction> activePredictions = predictionRepository.findByServiceIdAndStatusOrderByCreatedAtDesc(
                serviceId,
                PredictionStatus.ACTIVE
        );
        List<PredictionResponse> predictionResponses = activePredictions.stream()
                .map(PredictionResponse::fromEntity)
                .toList();

        PredictionRiskLevel highestRisk = activePredictions.stream()
                .map(Prediction::getRiskLevel)
                .filter(r -> r == PredictionRiskLevel.HIGH)
                .findFirst()
                .orElse(activePredictions.stream()
                        .map(Prediction::getRiskLevel)
                        .filter(r -> r == PredictionRiskLevel.MEDIUM)
                        .findFirst()
                        .orElse(PredictionRiskLevel.LOW));

        List<Incident> openIncidents = incidentRepository.findByServiceIdAndStatusInOrderByCreatedAtDesc(
                serviceId,
                ACTIVE_INCIDENT_STATUSES
        );
        List<ServiceIncidentItem> incidentItems = openIncidents.stream()
                .map(i -> new ServiceIncidentItem(
                        i.getId(),
                        i.getNumber(),
                        i.getTitle(),
                        i.getStatus().name(),
                        i.getPriority().name(),
                        i.getSeverity().name(),
                        i.getCreatedAt()
                ))
                .toList();

        List<DeploymentEntity> deployments = deploymentRepository.findByServiceIdOrderByDeployedAtDesc(
                serviceId,
                PageRequest.of(0, 10)
        ).getContent();
        List<ServiceDeploymentItem> deploymentItems = deployments.stream()
                .map(d -> new ServiceDeploymentItem(
                        d.getId(),
                        d.getVersion(),
                        d.getDeployedAt(),
                        d.getDeployedBy(),
                        d.getNotes()
                ))
                .toList();

        boolean hasP1P2 = openIncidents.stream().anyMatch(i -> i.getPriority() == IncidentPriority.P1 || i.getPriority() == IncidentPriority.P2);
        String healthStatus;
        if (hasP1P2 || highestRisk == PredictionRiskLevel.HIGH) {
            healthStatus = "CRITICAL";
        } else if (!openIncidents.isEmpty() || highestRisk == PredictionRiskLevel.MEDIUM) {
            healthStatus = "DEGRADED";
        } else {
            healthStatus = "HEALTHY";
        }

        return new ServiceHealthDetailResponse(
                service.getId(),
                service.getName(),
                service.getEnvironment().name(),
                service.getOwnerTeam(),
                service.getCriticality().name(),
                healthStatus,
                highestRisk,
                seriesList,
                predictionResponses,
                deploymentItems,
                incidentItems
        );
    }

    private String calculateHealthStatus(long openIncidents, PredictionRiskLevel highestRisk) {
        if (highestRisk == PredictionRiskLevel.HIGH || openIncidents >= 2) {
            return "CRITICAL";
        } else if (highestRisk == PredictionRiskLevel.MEDIUM || openIncidents > 0) {
            return "DEGRADED";
        } else {
            return "HEALTHY";
        }
    }

    private String formatMetricDisplayName(MetricType type) {
        return switch (type) {
            case CPU_PERCENT -> "CPU Utilization";
            case MEMORY_PERCENT -> "Memory Usage";
            case DISK_PERCENT -> "Disk Space";
            case LATENCY_P95_MS -> "Latency P95";
            case THROUGHPUT_RPS -> "Throughput";
            case ERROR_RATE_PERCENT -> "Error Rate";
        };
    }

    private String getMetricUnit(MetricType type) {
        return switch (type) {
            case CPU_PERCENT, MEMORY_PERCENT, DISK_PERCENT, ERROR_RATE_PERCENT -> "%";
            case LATENCY_P95_MS -> "ms";
            case THROUGHPUT_RPS -> "req/s";
        };
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
