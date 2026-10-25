package com.incidentiq.incident.repository;

import com.incidentiq.analytics.dto.DailyIncidentTrendProjection;
import com.incidentiq.analytics.dto.TopAffectedServiceProjection;
import com.incidentiq.incident.entity.Incident;
import com.incidentiq.incident.entity.IncidentPriority;
import com.incidentiq.incident.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByNumber(Long number);

    boolean existsByServiceIdAndStatusNotIn(UUID serviceId, Collection<IncidentStatus> statuses);

    long countByServiceId(UUID serviceId);

    long countByPriorityAndStatusIn(IncidentPriority priority, Collection<IncidentStatus> statuses);

    long countByStatusIn(Collection<IncidentStatus> statuses);

    long countByAssigneeIsNullAndStatusIn(Collection<IncidentStatus> statuses);

    long countBySlaBreachedTrueAndStatusIn(Collection<IncidentStatus> statuses);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.status IN :statuses AND i.slaBreached = false AND ((i.slaResolveDueAt IS NOT NULL AND i.slaResolveDueAt <= :threshold) OR (i.slaResponseDueAt IS NOT NULL AND i.slaResponseDueAt <= :threshold))")
    long countSlaAtRisk(@Param("statuses") Collection<IncidentStatus> statuses, @Param("threshold") Instant threshold);

    @Query("SELECT COUNT(DISTINCT i.service.id) FROM Incident i WHERE i.status IN :statuses")
    long countDistinctServiceByStatusIn(@Param("statuses") Collection<IncidentStatus> statuses);

    long countByResolvedAtGreaterThanEqual(Instant since);

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - COALESCE(started_at, created_at))) / 60.0), 0) FROM incidents WHERE resolved_at >= :since", nativeQuery = true)
    Double calculateMttrMinutes(@Param("since") Instant since);

    @Query(value = """
            WITH days AS (
              SELECT generate_series(CURRENT_DATE - INTERVAL '29 days', CURRENT_DATE, '1 day'::interval)::date AS day
            ),
            created AS (
              SELECT created_at::date AS day, count(*) AS count
              FROM incidents
              WHERE created_at >= CURRENT_DATE - INTERVAL '29 days'
              GROUP BY created_at::date
            ),
            resolved AS (
              SELECT resolved_at::date AS day, count(*) AS count
              FROM incidents
              WHERE resolved_at >= CURRENT_DATE - INTERVAL '29 days'
              GROUP BY resolved_at::date
            )
            SELECT
              to_char(days.day, 'YYYY-MM-DD') AS day,
              COALESCE(created.count, 0) AS createdCount,
              COALESCE(resolved.count, 0) AS resolvedCount
            FROM days
            LEFT JOIN created ON days.day = created.day
            LEFT JOIN resolved ON days.day = resolved.day
            ORDER BY days.day ASC
            """, nativeQuery = true)
    List<DailyIncidentTrendProjection> find30DayTrend();

    @Query(value = """
            SELECT
              s.id AS serviceId,
              s.name AS serviceName,
              s.criticality AS serviceTier,
              COUNT(CASE WHEN i.status IN ('OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS') THEN 1 END) AS activeIncidents,
              COUNT(i.id) AS totalIncidents
            FROM services s
            LEFT JOIN incidents i ON s.id = i.service_id
            WHERE s.active = true
            GROUP BY s.id, s.name, s.criticality
            ORDER BY activeIncidents DESC, totalIncidents DESC, s.name ASC
            LIMIT 5
            """, nativeQuery = true)
    List<TopAffectedServiceProjection> findTopAffectedServices();

    List<Incident> findTop5ByOrderByCreatedAtDesc();
}
