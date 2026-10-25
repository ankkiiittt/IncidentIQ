package com.incidentiq.metrics.repository;

import com.incidentiq.metrics.entity.Prediction;
import com.incidentiq.metrics.entity.PredictionRiskLevel;
import com.incidentiq.metrics.entity.PredictionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, UUID> {

    List<Prediction> findByStatusOrderByCreatedAtDesc(PredictionStatus status);

    List<Prediction> findByStatusAndRiskLevelOrderByCreatedAtDesc(
            PredictionStatus status,
            PredictionRiskLevel riskLevel
    );

    List<Prediction> findByServiceIdAndStatusOrderByCreatedAtDesc(
            UUID serviceId,
            PredictionStatus status
    );

    List<Prediction> findByServiceIdOrderByCreatedAtDesc(UUID serviceId);

    Optional<Prediction> findTopByServiceIdAndStatusOrderByCreatedAtDesc(
            UUID serviceId,
            PredictionStatus status
    );

    long countByStatus(PredictionStatus status);

    long countByStatusAndRiskLevel(PredictionStatus status, PredictionRiskLevel riskLevel);

    @Query("""
            SELECT p FROM Prediction p
            JOIN FETCH p.service
            WHERE p.status = :status
            ORDER BY p.createdAt DESC
            """)
    List<Prediction> findAllWithServiceByStatus(@Param("status") PredictionStatus status);

    @Query("""
            SELECT p FROM Prediction p
            JOIN FETCH p.service
            WHERE p.id = :id
            """)
    Optional<Prediction> findByIdWithService(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Prediction p
            WHERE p.service.id = :serviceId
              AND p.status = 'ACTIVE'
              AND p.createdAt >= :since
            ORDER BY p.createdAt DESC
            """)
    List<Prediction> findActiveSince(
            @Param("serviceId") UUID serviceId,
            @Param("since") Instant since
    );
}
