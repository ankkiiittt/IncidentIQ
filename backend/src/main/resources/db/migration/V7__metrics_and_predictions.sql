-- V7__metrics_and_predictions.sql
-- Ingestion of telemetry metrics and predictive anomaly detection

CREATE TABLE IF NOT EXISTS metrics (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    metric_name VARCHAR(25) NOT NULL CHECK (metric_name IN (
        'CPU_PERCENT',
        'MEMORY_PERCENT',
        'DISK_PERCENT',
        'LATENCY_P95_MS',
        'THROUGHPUT_RPS',
        'ERROR_RATE_PERCENT'
    )),
    value DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_metrics_service_metric_time UNIQUE (service_id, metric_name, recorded_at)
);

CREATE INDEX IF NOT EXISTS idx_metrics_service_metric_recorded 
ON metrics(service_id, metric_name, recorded_at DESC);

CREATE TABLE IF NOT EXISTS predictions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    anomaly_score NUMERIC(4,3) NOT NULL CHECK (anomaly_score >= 0 AND anomaly_score <= 1),
    risk_level VARCHAR(6) NOT NULL CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH')),
    failure_probability NUMERIC(4,3) CHECK (failure_probability IS NULL OR (failure_probability >= 0 AND failure_probability <= 1)),
    horizon_minutes INT NOT NULL DEFAULT 30,
    source VARCHAR(12) NOT NULL CHECK (source IN ('STATISTICAL', 'ML')),
    model_version VARCHAR(50) NOT NULL,
    contributing_metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RESOLVED', 'DISMISSED')),
    incident_id UUID REFERENCES incidents(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_predictions_service_created 
ON predictions(service_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_predictions_status 
ON predictions(status);

-- Default risk thresholds configuration
INSERT INTO app_settings (key, value, description)
VALUES (
    'prediction.risk_thresholds',
    '{"high": 0.80, "medium": 0.50}'::jsonb,
    'Anomaly score thresholds for predictive risk classification'
)
ON CONFLICT (key) DO NOTHING;
