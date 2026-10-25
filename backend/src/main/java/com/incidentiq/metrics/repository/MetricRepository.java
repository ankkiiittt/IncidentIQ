package com.incidentiq.metrics.repository;

import com.incidentiq.metrics.dto.MetricBaselineProjection;
import com.incidentiq.metrics.entity.Metric;
import com.incidentiq.metrics.entity.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MetricRepository extends JpaRepository<Metric, Long> {

    List<Metric> findByServiceIdAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(UUID serviceId, Instant since);

    List<Metric> findByServiceIdAndMetricNameAndRecordedAtGreaterThanEqualOrderByRecordedAtAsc(
            UUID serviceId,
            MetricType metricName,
            Instant since
    );

    @Query(value = """
            SELECT DISTINCT ON (metric_name) *
            FROM metrics
            WHERE service_id = :serviceId
            ORDER BY metric_name, recorded_at DESC
            """, nativeQuery = true)
    List<Metric> findLatestByServiceId(@Param("serviceId") UUID serviceId);

    @Query(value = """
            SELECT
                metric_name AS metricName,
                COUNT(*) AS sampleCount,
                AVG(value) AS avgValue,
                COALESCE(STDDEV_SAMP(value), 0.0) AS stddevValue,
                MAX(value) AS maxValue,
                MIN(value) AS minValue
            FROM metrics
            WHERE service_id = :serviceId AND recorded_at >= :since
            GROUP BY metric_name
            """, nativeQuery = true)
    List<MetricBaselineProjection> calculateBaselines(
            @Param("serviceId") UUID serviceId,
            @Param("since") Instant since
    );

    @Modifying
    @Query(value = """
            INSERT INTO metrics (service_id, metric_name, value, recorded_at)
            VALUES (:serviceId, :metricName, :val, :recordedAt)
            ON CONFLICT (service_id, metric_name, recorded_at) DO NOTHING
            """, nativeQuery = true)
    int insertIdempotent(
            @Param("serviceId") UUID serviceId,
            @Param("metricName") String metricName,
            @Param("val") Double val,
            @Param("recordedAt") Instant recordedAt
    );
}
