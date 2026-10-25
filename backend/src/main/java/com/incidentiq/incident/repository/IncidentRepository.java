package com.incidentiq.incident.repository;

import com.incidentiq.analytics.dto.CategoryCountProjection;
import com.incidentiq.analytics.dto.DailyIncidentTrendProjection;
import com.incidentiq.analytics.dto.PrioritySlaProjection;
import com.incidentiq.analytics.dto.RecurringCauseProjection;
import com.incidentiq.analytics.dto.TopAffectedServiceProjection;
import com.incidentiq.analytics.dto.VolumeTrendProjection;
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

    long countByServiceIdAndStatusIn(UUID serviceId, Collection<IncidentStatus> statuses);

    List<Incident> findByServiceIdAndStatusInOrderByCreatedAtDesc(UUID serviceId, Collection<IncidentStatus> statuses);

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

    long countByCreatedAtGreaterThanEqual(Instant since);

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 60.0), 0) FROM incidents WHERE resolved_at >= :since", nativeQuery = true)
    Double calculateResolvedMttrMinutes(@Param("since") Instant since);

    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (created_at - started_at)) / 60.0), 0) FROM incidents WHERE created_at >= :since AND started_at IS NOT NULL", nativeQuery = true)
    Double calculateMttdMinutes(@Param("since") Instant since);

    @Query("SELECT i.priority, COUNT(i) FROM Incident i WHERE i.createdAt >= :since GROUP BY i.priority")
    List<Object[]> countByPrioritySince(@Param("since") Instant since);

    @Query(value = """
            SELECT COUNT(*) FROM incidents
            WHERE created_at >= :since
              AND (
                (status IN ('RESOLVED', 'CLOSED') AND sla_breached = false)
                OR
                (status NOT IN ('RESOLVED', 'CLOSED') AND (sla_resolve_due_at IS NULL OR sla_resolve_due_at >= CURRENT_TIMESTAMP))
              )
            """, nativeQuery = true)
    long countSlaMetSince(@Param("since") Instant since);

    @Query(value = """
            SELECT COUNT(*) FROM incidents
            WHERE created_at >= :since
              AND (
                sla_breached = true
                OR
                (status NOT IN ('RESOLVED', 'CLOSED') AND sla_resolve_due_at IS NOT NULL AND sla_resolve_due_at < CURRENT_TIMESTAMP)
              )
            """, nativeQuery = true)
    long countSlaBreachedSince(@Param("since") Instant since);

    @Query(value = """
            SELECT
              priority AS priority,
              COUNT(CASE WHEN (status IN ('RESOLVED', 'CLOSED') AND sla_breached = false) OR (status NOT IN ('RESOLVED', 'CLOSED') AND (sla_resolve_due_at IS NULL OR sla_resolve_due_at >= CURRENT_TIMESTAMP)) THEN 1 END) AS metCount,
              COUNT(CASE WHEN sla_breached = true OR (status NOT IN ('RESOLVED', 'CLOSED') AND sla_resolve_due_at IS NOT NULL AND sla_resolve_due_at < CURRENT_TIMESTAMP) THEN 1 END) AS breachedCount
            FROM incidents
            WHERE created_at >= :since
            GROUP BY priority
            ORDER BY priority ASC
            """, nativeQuery = true)
    List<PrioritySlaProjection> findSlaBreakdownByPriority(@Param("since") Instant since);

    @Query(value = """
            WITH days AS (
              SELECT generate_series(date_trunc('day', CAST(:since AS timestamp with time zone)), date_trunc('day', CURRENT_TIMESTAMP), '1 day'::interval)::date AS day
            ),
            counts AS (
              SELECT
                created_at::date AS day,
                COUNT(CASE WHEN priority = 'P1' THEN 1 END) AS p1,
                COUNT(CASE WHEN priority = 'P2' THEN 1 END) AS p2,
                COUNT(CASE WHEN priority = 'P3' THEN 1 END) AS p3,
                COUNT(CASE WHEN priority = 'P4' THEN 1 END) AS p4,
                COUNT(*) AS total
              FROM incidents
              WHERE created_at >= :since
              GROUP BY created_at::date
            )
            SELECT
              to_char(days.day, 'YYYY-MM-DD') AS timestamp,
              COALESCE(counts.p1, 0) AS p1,
              COALESCE(counts.p2, 0) AS p2,
              COALESCE(counts.p3, 0) AS p3,
              COALESCE(counts.p4, 0) AS p4,
              COALESCE(counts.total, 0) AS total
            FROM days
            LEFT JOIN counts ON days.day = counts.day
            ORDER BY days.day ASC
            """, nativeQuery = true)
    List<VolumeTrendProjection> findVolumeTrendDaily(@Param("since") Instant since);

    @Query(value = """
            WITH weeks AS (
              SELECT generate_series(date_trunc('week', CAST(:since AS timestamp with time zone)), date_trunc('week', CURRENT_TIMESTAMP), '1 week'::interval)::date AS week_start
            ),
            counts AS (
              SELECT
                date_trunc('week', created_at)::date AS week_start,
                COUNT(CASE WHEN priority = 'P1' THEN 1 END) AS p1,
                COUNT(CASE WHEN priority = 'P2' THEN 1 END) AS p2,
                COUNT(CASE WHEN priority = 'P3' THEN 1 END) AS p3,
                COUNT(CASE WHEN priority = 'P4' THEN 1 END) AS p4,
                COUNT(*) AS total
              FROM incidents
              WHERE created_at >= :since
              GROUP BY date_trunc('week', created_at)::date
            )
            SELECT
              to_char(weeks.week_start, 'YYYY-MM-DD') AS timestamp,
              COALESCE(counts.p1, 0) AS p1,
              COALESCE(counts.p2, 0) AS p2,
              COALESCE(counts.p3, 0) AS p3,
              COALESCE(counts.p4, 0) AS p4,
              COALESCE(counts.total, 0) AS total
            FROM weeks
            LEFT JOIN counts ON weeks.week_start = counts.week_start
            ORDER BY weeks.week_start ASC
            """, nativeQuery = true)
    List<VolumeTrendProjection> findVolumeTrendWeekly(@Param("since") Instant since);

    @Query(value = """
            SELECT
              category AS category,
              COUNT(*) AS count
            FROM incidents
            WHERE created_at >= :since AND category IS NOT NULL
            GROUP BY category
            ORDER BY count DESC
            """, nativeQuery = true)
    List<CategoryCountProjection> findCategoryDistribution(@Param("since") Instant since);

    @Query(value = """
            SELECT
              s.id AS serviceId,
              s.name AS serviceName,
              i.category AS category,
              COUNT(i.id) AS incidentCount,
              MAX(i.number) AS lastIncidentNumber,
              MAX(i.created_at) AS lastOccurredAt
            FROM incidents i
            JOIN services s ON i.service_id = s.id
            WHERE i.created_at >= :since AND i.category IS NOT NULL
            GROUP BY s.id, s.name, i.category
            ORDER BY incidentCount DESC, lastOccurredAt DESC
            """, nativeQuery = true)
    List<RecurringCauseProjection> findCausesAndRecurrence(@Param("since") Instant since);
}
