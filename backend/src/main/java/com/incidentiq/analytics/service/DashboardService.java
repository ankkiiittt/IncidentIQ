package com.incidentiq.analytics.service;

import com.incidentiq.analytics.dto.*;
import com.incidentiq.catalog.repository.ServiceRepository;
import com.incidentiq.incident.dto.IncidentSummaryResponse;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentStatus;
import com.incidentiq.incident.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final Set<IncidentStatus> ACTIVE_STATUSES = Set.of(
            IncidentStatus.OPEN,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS
    );

    private final IncidentRepository incidentRepository;
    private final ServiceRepository serviceRepository;

    public DashboardService(IncidentRepository incidentRepository, ServiceRepository serviceRepository) {
        this.incidentRepository = incidentRepository;
        this.serviceRepository = serviceRepository;
    }

    public DashboardResponse getDashboardData() {
        Instant now = Instant.now();

        // 1. KPIs
        long openP1 = incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P1, ACTIVE_STATUSES);
        long openP2 = incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P2, ACTIVE_STATUSES);
        long openP3 = incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P3, ACTIVE_STATUSES);
        long openP4 = incidentRepository.countByPriorityAndStatusIn(IncidentPriority.P4, ACTIVE_STATUSES);
        long totalOpen = incidentRepository.countByStatusIn(ACTIVE_STATUSES);

        long unassigned = incidentRepository.countByAssigneeIsNullAndStatusIn(ACTIVE_STATUSES);
        long slaBreached = incidentRepository.countBySlaBreachedTrueAndStatusIn(ACTIVE_STATUSES);
        long slaAtRisk = incidentRepository.countSlaAtRisk(ACTIVE_STATUSES, now.plus(Duration.ofMinutes(60)));

        long resolvedLast7Days = incidentRepository.countByResolvedAtGreaterThanEqual(now.minus(Duration.ofDays(7)));
        Double mttrMinutesRaw = incidentRepository.calculateMttrMinutes(now.minus(Duration.ofDays(30)));
        Double mttrMinutes = (mttrMinutesRaw != null && mttrMinutesRaw > 0) ? Math.round(mttrMinutesRaw * 10.0) / 10.0 : 0.0;

        DashboardKpiResponse kpis = new DashboardKpiResponse(
                openP1,
                openP2,
                openP3,
                openP4,
                totalOpen,
                unassigned,
                slaBreached,
                slaAtRisk,
                resolvedLast7Days,
                mttrMinutes
        );

        // 2. 30-Day Trend
        List<DailyTrendPoint> trend30Days = incidentRepository.find30DayTrend().stream()
                .map(p -> new DailyTrendPoint(p.getDay(), p.getCreatedCount(), p.getResolvedCount()))
                .toList();

        // 3. Top Affected Services
        List<TopAffectedServiceDto> topAffectedServices = incidentRepository.findTopAffectedServices().stream()
                .map(p -> new TopAffectedServiceDto(
                        p.getServiceId(),
                        p.getServiceName(),
                        p.getServiceTier(),
                        p.getActiveIncidents(),
                        p.getTotalIncidents()
                ))
                .toList();

        // 4. Recent Incidents
        List<IncidentSummaryResponse> recentIncidents = incidentRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(IncidentSummaryResponse::fromEntity)
                .toList();

        // 5. Service Health Summary
        long totalServices = serviceRepository.countByActiveTrue();
        long degradedServices = incidentRepository.countDistinctServiceByStatusIn(ACTIVE_STATUSES);
        long healthyServices = Math.max(0, totalServices - degradedServices);

        ServiceHealthSummaryDto serviceHealth = new ServiceHealthSummaryDto(
                totalServices,
                healthyServices,
                degradedServices
        );

        return new DashboardResponse(
                kpis,
                trend30Days,
                topAffectedServices,
                recentIncidents,
                serviceHealth
        );
    }
}
