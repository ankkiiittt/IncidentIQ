-- V5__vector_and_knowledge.sql
-- IncidentIQ: Knowledge documents, chunks, and pgvector cosine HNSW index

CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(200) NOT NULL,
    source_type VARCHAR(12) NOT NULL CHECK (source_type IN ('RUNBOOK', 'FAQ', 'POSTMORTEM', 'INCIDENT')),
    source_uri TEXT,
    incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL UNIQUE,
    service_id UUID REFERENCES services(id) ON DELETE SET NULL,
    access_level VARCHAR(12) NOT NULL DEFAULT 'PUBLIC' CHECK (access_level IN ('PUBLIC', 'ENGINEERING')),
    content_hash VARCHAR(64) NOT NULL UNIQUE,
    version INT NOT NULL DEFAULT 1,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'INGESTED', 'FAILED')),
    chunk_count INT NOT NULL DEFAULT 0,
    error_message TEXT,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_knowledge_docs_service ON knowledge_documents(service_id);
CREATE INDEX idx_knowledge_docs_source ON knowledge_documents(source_type);
CREATE INDEX idx_knowledge_docs_access ON knowledge_documents(access_level);
CREATE INDEX idx_knowledge_docs_status ON knowledge_documents(status);

CREATE TABLE knowledge_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    token_count INT NOT NULL,
    content TEXT NOT NULL,
    embedding VECTOR(384) NOT NULL,
    source_type VARCHAR(12) NOT NULL,
    access_level VARCHAR(12) NOT NULL,
    service_id UUID REFERENCES services(id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_doc_chunk_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_chunks_doc_id ON knowledge_chunks(document_id);
CREATE INDEX idx_chunks_filtering ON knowledge_chunks(source_type, access_level, service_id);

-- HNSW vector cosine distance index for 384-dimensional embeddings
CREATE INDEX idx_chunks_embedding_hnsw ON knowledge_chunks USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 64);
