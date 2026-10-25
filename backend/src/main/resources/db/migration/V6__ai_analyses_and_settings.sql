-- V6__ai_analyses_and_settings.sql
-- Phase P7: AI Diagnostics, Root Cause Analysis, Recommendations, and App Settings

-- Seed AI application settings into existing app_settings table
INSERT INTO app_settings (key, value, description) VALUES
('ai.enabled', 'true'::jsonb, 'Master switch for AI analysis pipeline'),
('ai.auto_analyze', 'true'::jsonb, 'Automatically trigger AI triage upon incident creation'),
('ai.rag.top_k', '5'::jsonb, 'Number of vector knowledge chunks retrieved for RAG context'),
('ai.rag.min_similarity', '0.40'::jsonb, 'Minimum cosine similarity threshold for evidence chunks'),
('ai.rate_limit_per_user_per_hour', '10'::jsonb, 'Maximum manual AI analyses per user per hour'),
('prediction.risk_thresholds', '{"high": 0.80, "medium": 0.50}'::jsonb, 'Thresholds for anomaly risk classification'),
('retention.metrics_days', '90'::jsonb, 'Retention window for timeseries metric points')
ON CONFLICT (key) DO NOTHING;

-- AI analyses table
CREATE TABLE ai_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    trigger VARCHAR(6) NOT NULL,
    requested_by UUID REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(22) NOT NULL,
    model VARCHAR(100) NOT NULL,
    embedding_model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    category VARCHAR(20),
    suggested_priority VARCHAR(2),
    summary TEXT,
    confidence NUMERIC(3,2),
    root_causes JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    evidence_sufficient BOOLEAN NOT NULL DEFAULT false,
    input_tokens INT,
    output_tokens INT,
    latency_ms INT,
    error_code VARCHAR(100),
    correlation_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_analysis_trigger CHECK (trigger IN ('AUTO', 'MANUAL')),
    CONSTRAINT chk_analysis_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'INSUFFICIENT_EVIDENCE', 'FAILED')),
    CONSTRAINT chk_analysis_confidence CHECK (confidence IS NULL OR (confidence >= 0.0 AND confidence <= 1.0))
);

CREATE INDEX idx_ai_analyses_incident_created ON ai_analyses(incident_id, created_at DESC);
CREATE INDEX idx_ai_analyses_status ON ai_analyses(status);

-- Recommendations table
CREATE TABLE recommendations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id UUID NOT NULL REFERENCES ai_analyses(id) ON DELETE CASCADE,
    position INT NOT NULL,
    action TEXT NOT NULL,
    rationale TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT 'SUGGESTED',
    feedback_rating VARCHAR(11),
    feedback_comment TEXT,
    feedback_by UUID REFERENCES users(id) ON DELETE SET NULL,
    feedback_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_rec_status CHECK (status IN ('SUGGESTED', 'ACCEPTED', 'REJECTED', 'COMPLETED')),
    CONSTRAINT chk_rec_feedback CHECK (feedback_rating IS NULL OR feedback_rating IN ('HELPFUL', 'NOT_HELPFUL')),
    CONSTRAINT uq_rec_analysis_pos UNIQUE (analysis_id, position)
);

CREATE INDEX idx_recommendations_analysis ON recommendations(analysis_id);
