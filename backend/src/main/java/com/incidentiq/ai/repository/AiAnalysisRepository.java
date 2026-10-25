package com.incidentiq.ai.repository;

import com.incidentiq.ai.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    List<AiAnalysis> findByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    Optional<AiAnalysis> findFirstByIncidentIdOrderByCreatedAtDesc(UUID incidentId);

    long countByIncidentId(UUID incidentId);
}
