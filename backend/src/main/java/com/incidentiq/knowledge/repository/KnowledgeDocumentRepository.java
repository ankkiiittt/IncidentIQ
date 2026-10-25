package com.incidentiq.knowledge.repository;

import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.entity.DocumentStatus;
import com.incidentiq.knowledge.entity.KnowledgeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByContentHash(String contentHash);

    boolean existsByContentHash(String contentHash);

    Optional<KnowledgeDocument> findByIncidentId(UUID incidentId);

    @Query("SELECT d FROM KnowledgeDocument d WHERE d.accessLevel IN :accessLevels ORDER BY d.createdAt DESC")
    Page<KnowledgeDocument> findByAccessLevelIn(@Param("accessLevels") Collection<AccessLevel> accessLevels, Pageable pageable);

    @Query("SELECT d FROM KnowledgeDocument d WHERE d.accessLevel IN :accessLevels AND (:sourceType IS NULL OR d.sourceType = :sourceType) AND (:serviceId IS NULL OR d.service.id = :serviceId) ORDER BY d.createdAt DESC")
    Page<KnowledgeDocument> findWithFilters(
            @Param("accessLevels") Collection<AccessLevel> accessLevels,
            @Param("sourceType") DocumentSourceType sourceType,
            @Param("serviceId") UUID serviceId,
            Pageable pageable
    );

    long countByStatus(DocumentStatus status);
}
