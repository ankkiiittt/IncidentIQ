package com.incidentiq.knowledge.repository;

import com.incidentiq.knowledge.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    void deleteByDocumentId(UUID documentId);

    long countByDocumentId(UUID documentId);

    @Query(value = """
            SELECT
                c.id AS chunkId,
                c.document_id AS documentId,
                d.title AS documentTitle,
                c.source_type AS sourceType,
                c.access_level AS accessLevel,
                c.service_id AS serviceId,
                s.name AS serviceName,
                c.chunk_index AS chunkIndex,
                c.token_count AS tokenCount,
                c.content AS content,
                CAST(c.metadata AS text) AS metadata,
                (1 - (c.embedding <=> CAST(:queryVector AS vector))) AS similarity
            FROM knowledge_chunks c
            JOIN knowledge_documents d ON c.document_id = d.id
            LEFT JOIN services s ON c.service_id = s.id
            WHERE c.access_level IN (:allowedAccessLevels)
              AND (CAST(:serviceId AS uuid) IS NULL OR c.service_id = CAST(:serviceId AS uuid))
              AND (:sourceType IS NULL OR c.source_type = :sourceType)
              AND (1 - (c.embedding <=> CAST(:queryVector AS vector))) >= :minSimilarity
            ORDER BY (c.embedding <=> CAST(:queryVector AS vector)) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<ChunkSearchProjection> vectorSearch(
            @Param("queryVector") String queryVector,
            @Param("allowedAccessLevels") Collection<String> allowedAccessLevels,
            @Param("serviceId") UUID serviceId,
            @Param("sourceType") String sourceType,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT
                c.document_id AS documentId,
                d.incident_id AS incidentId,
                i.number AS incidentNumber,
                i.title AS title,
                i.severity AS severity,
                i.priority AS priority,
                i.status AS status,
                i.root_cause AS rootCause,
                i.resolution AS resolution,
                i.resolved_at AS resolvedAt,
                s.name AS serviceName,
                MAX(1 - (c.embedding <=> CAST(:queryVector AS vector))) AS maxSimilarity
            FROM knowledge_chunks c
            JOIN knowledge_documents d ON c.document_id = d.id
            JOIN incidents i ON d.incident_id = i.id
            LEFT JOIN services s ON i.service_id = s.id
            WHERE c.source_type = 'INCIDENT'
              AND c.access_level IN (:allowedAccessLevels)
              AND (CAST(:excludeIncidentId AS uuid) IS NULL OR d.incident_id IS NULL OR d.incident_id != CAST(:excludeIncidentId AS uuid))
              AND (1 - (c.embedding <=> CAST(:queryVector AS vector))) >= :minSimilarity
            GROUP BY c.document_id, d.incident_id, i.number, i.title, i.severity, i.priority, i.status, i.root_cause, i.resolution, i.resolved_at, s.name
            ORDER BY maxSimilarity DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<SimilarIncidentProjection> findSimilarIncidents(
            @Param("queryVector") String queryVector,
            @Param("allowedAccessLevels") Collection<String> allowedAccessLevels,
            @Param("excludeIncidentId") UUID excludeIncidentId,
            @Param("minSimilarity") double minSimilarity,
            @Param("limit") int limit
    );
}
