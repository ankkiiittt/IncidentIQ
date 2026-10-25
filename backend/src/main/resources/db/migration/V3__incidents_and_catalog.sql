-- V3__incidents_and_catalog.sql
-- IncidentIQ: Catalog (Services, Deployments), Incidents, Incident Events, Comments, App Settings

CREATE TABLE services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    environment VARCHAR(20) NOT NULL CHECK (environment IN ('PROD', 'STAGING', 'DEV')),
    owner_team VARCHAR(100) NOT NULL,
    criticality VARCHAR(10) NOT NULL CHECK (criticality IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_services_name_environment UNIQUE (name, environment)
);

CREATE TABLE deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_id UUID NOT NULL REFERENCES services(id),
    version VARCHAR(50) NOT NULL,
    deployed_at TIMESTAMPTZ NOT NULL,
    deployed_by VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_deployments_service_deployed ON deployments(service_id, deployed_at DESC);

CREATE SEQUENCE incident_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    number BIGINT NOT NULL DEFAULT nextval('incident_number_seq') UNIQUE,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW')),
    priority VARCHAR(2) NOT NULL DEFAULT 'P3' CHECK (priority IN ('P1', 'P2', 'P3', 'P4')),
    status VARCHAR(15) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    category VARCHAR(20) CHECK (category IS NULL OR category IN ('DATABASE', 'NETWORK', 'APPLICATION', 'INFRASTRUCTURE', 'DEPLOYMENT', 'SECURITY', 'THIRD_PARTY', 'PERFORMANCE', 'OTHER')),
    service_id UUID NOT NULL REFERENCES services(id),
    reporter_id UUID NOT NULL REFERENCES users(id),
    assignee_id UUID REFERENCES users(id),
    tags TEXT[] NOT NULL DEFAULT '{}',
    started_at TIMESTAMPTZ,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    escalated_at TIMESTAMPTZ,
    escalation_count INT NOT NULL DEFAULT 0,
    root_cause TEXT,
    resolution TEXT,
    sla_response_due_at TIMESTAMPTZ,
    sla_resolve_due_at TIMESTAMPTZ,
    sla_breached BOOLEAN NOT NULL DEFAULT false,
    search_vector TSVECTOR,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_incidents_resolve_fields CHECK (
        (status NOT IN ('RESOLVED', 'CLOSED')) OR (root_cause IS NOT NULL AND resolution IS NOT NULL)
    )
);

CREATE INDEX idx_incidents_service_created ON incidents(service_id, created_at DESC);
CREATE INDEX idx_incidents_assignee ON incidents(assignee_id);
CREATE INDEX idx_incidents_status_priority ON incidents(status, priority);
CREATE INDEX idx_incidents_tags ON incidents USING GIN (tags);
CREATE INDEX idx_incidents_search_vector ON incidents USING GIN (search_vector);

CREATE OR REPLACE FUNCTION incidents_search_vector_trigger()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', coalesce(NEW.title, '') || ' ' || coalesce(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_incidents_search_vector
BEFORE INSERT OR UPDATE OF title, description ON incidents
FOR EACH ROW
EXECUTE FUNCTION incidents_search_vector_trigger();

CREATE TABLE incident_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN (
        'CREATED', 'STATUS_CHANGED', 'ASSIGNED', 'ESCALATED',
        'PRIORITY_CHANGED', 'COMMENT_ADDED', 'LOG_ADDED',
        'DEPLOYMENT_LINKED', 'AI_ANALYSIS_COMPLETED',
        'AI_ANALYSIS_FAILED', 'PREDICTION_LINKED'
    )),
    actor_id UUID REFERENCES users(id),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incident_events_incident_created ON incident_events(incident_id, created_at);

CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    edited_at TIMESTAMPTZ
);

CREATE INDEX idx_comments_incident_created ON comments(incident_id, created_at);

CREATE TABLE app_settings (
    key VARCHAR(100) PRIMARY KEY,
    value JSONB NOT NULL,
    description TEXT,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Seed default SLA targets
INSERT INTO app_settings (key, value, description)
VALUES (
    'sla.targets',
    '{"P1":{"responseMinutes":15,"resolveMinutes":240},"P2":{"responseMinutes":30,"resolveMinutes":480},"P3":{"responseMinutes":240,"resolveMinutes":4320},"P4":{"responseMinutes":1440,"resolveMinutes":10080}}'::jsonb,
    'SLA response and resolve targets in minutes per priority'
)
ON CONFLICT (key) DO NOTHING;
