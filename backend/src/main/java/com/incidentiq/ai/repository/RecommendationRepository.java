package com.incidentiq.ai.repository;

import com.incidentiq.ai.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {

    List<Recommendation> findByAnalysisIdOrderByPositionAsc(UUID analysisId);
}
