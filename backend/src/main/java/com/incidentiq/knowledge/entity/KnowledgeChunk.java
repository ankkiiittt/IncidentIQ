package com.incidentiq.knowledge.entity;

import com.incidentiq.catalog.entity.ServiceEntity;
import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "knowledge_chunks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_doc_chunk_index", columnNames = {"document_id", "chunk_index"})
        }
)
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "token_count", nullable = false)
    private int tokenCount;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "embedding", columnDefinition = "vector(384)", nullable = false)
    @org.hibernate.annotations.ColumnTransformer(
            write = "?::vector",
            read = "embedding::text"
    )
    @Convert(converter = PGvectorConverter.class)
    private float[] embedding;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 12)
    private DocumentSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", nullable = false, length = 12)
    private AccessLevel accessLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected KnowledgeChunk() {}

    public KnowledgeChunk(KnowledgeDocument document,
                          int chunkIndex,
                          int tokenCount,
                          String content,
                          float[] embedding,
                          DocumentSourceType sourceType,
                          AccessLevel accessLevel,
                          ServiceEntity service,
                          Map<String, Object> metadata) {
        this.document = document;
        this.chunkIndex = chunkIndex;
        this.tokenCount = tokenCount;
        this.content = content;
        this.embedding = embedding;
        this.sourceType = sourceType;
        this.accessLevel = accessLevel;
        this.service = service;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public KnowledgeDocument getDocument() {
        return document;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public int getTokenCount() {
        return tokenCount;
    }

    public String getContent() {
        return content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public DocumentSourceType getSourceType() {
        return sourceType;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public ServiceEntity getService() {
        return service;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
