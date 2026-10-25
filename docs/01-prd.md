# 01 - Product Requirements Document (PRD)

## 1. Product overview
- **Product name**: IncidentIQ
- **Description**: An incident management platform with an evidence-first AI layer.
- **Problem**: Slow triage, scattered knowledge and unexplained root-cause guesses during incidents.
- **Why it should exist**: It combines classic incident workflow with retrieval-augmented analysis whose every claim cites evidence and whose recommendations are never auto-executed.

## 2. Target users
- **Primary users**: Support engineers and developers (`ENGINEER`, `SUPPORT` roles) and SRE or DevOps teams.
- **Their problem**: Hours spent on manual triage and searching history; inconsistent priority and ownership.
- **Desired outcome**: A prioritised, assigned incident with a grounded hypothesis and next diagnostic steps within a minute.
- **Secondary users**: Engineering managers (`VIEWER` or `ENGINEER`) reviewing MTTR, SLA and recurring causes; `ADMIN` managing users and policies.

## 3. Core user outcome
A user reports an incident and quickly sees its category, suggested priority, similar past incidents and runbooks, ranked root-cause hypotheses with cited evidence and confidence, and recommended actions, or an explicit statement that evidence is insufficient.

## 4. Core features

### F1 Authentication and RBAC
- **Tier**: Tier 1
- **User need**: Securely identify users and restrict actions by role.
- **What it does**: Email and password login. JWT access token (15 min) plus rotating refresh token (7 days, httpOnly cookie). Roles `ADMIN`, `ENGINEER`, `SUPPORT`, `VIEWER`. Lockout after 5 failed attempts for 15 minutes. ADMIN-created users, forced password change for temporary passwords. Auth events audited. Login rate limited.
- **Inputs and outputs**: In: email, password. Out: access token and profile (id, name, role).
- **Acceptance criteria**: Passwords hashed with BCrypt (cost 12). Expired access token returns 401 and the client refreshes once. Reuse of a rotated refresh token revokes the whole token family. Every endpoint enforces the permission matrix in TRD section 5. Unauthorised access returns 403 with no data leak. Auth events appear in the audit log.
- **Error or empty states**: Invalid credentials: generic message. Locked account: message with retry time. Rate limit: 429.

### F2 Service Registry
- **Tier**: Tier 1
- **User need**: Tie every incident, metric and deployment to a system.
- **What it does**: ADMIN creates, edits and deactivates services (name, environment, owner team, criticality, description). Deployments (version, time, notes) are recorded per service by ENGINEER or ADMIN.
- **Inputs and outputs**: In: service or deployment fields. Out: paginated lists.
- **Acceptance criteria**: Name plus environment is unique. A service with open incidents cannot be deactivated. Deployment history is listed newest first.
- **Error or empty states**: Duplicate: 409. Empty list: prompt to add a service.

### F3 Incident Management
- **Tier**: Tier 1
- **User need**: Track an incident from report to closure.
- **What it does**: Create with title, description, severity, priority P1-P4, service, tags, optional start time. Assign, escalate (priority moves up one level to a minimum of P1), comment, paste log snippets (max 4 KB). Status flow OPEN, ACKNOWLEDGED, IN_PROGRESS, RESOLVED, CLOSED. Resolve requires root cause and resolution text. SLA due times derive from priority. Keyword search and filters.
- **Inputs and outputs**: In: incident fields and actions. Out: incident, timeline, SLA state.
- **Acceptance criteria**: Invalid transitions return 409 with allowed transitions. Optimistic locking returns 409 on stale version. Every change creates a timeline event with actor. SLA breach is flagged. Search supports text, status, priority, service, assignee, tag and date range; page size default 20, max 100.
- **Error or empty states**: Validation errors per field. Empty list: prompt to create an incident.

### F4 Operational Dashboard
- **Tier**: Tier 1, grows in Tier 2 and 3
- **User need**: See the operational state at a glance.
- **What it does**: KPI cards (open by priority, unassigned, SLA at risk, resolved in 7 days), 30-day trend chart, top affected services, recent incidents. Later adds MTTR (F10) and predicted failures (F11).
- **Inputs and outputs**: Out: KPI values and chart series.
- **Acceptance criteria**: Numbers equal the corresponding list filters. Loads in under 2 seconds on seed data.
- **Error or empty states**: No data: zero values and a helpful empty chart message.

### F5 Event-Driven Processing
- **Tier**: Tier 2
- **User need**: Keep AI and background work off the request path, reliably.
- **What it does**: Transactional outbox publishes events to Kafka (topics in TRD section 9). Consumers are idempotent through `processed_events`. Three retries with backoff, then a dead-letter topic. Consumer lag exposed as a metric.
- **Inputs and outputs**: In: committed domain changes. Out: events and derived work (analyses, ingestion, notifications).
- **Acceptance criteria**: Duplicate delivery of one eventId creates no duplicate analysis, ingestion or incident (Testcontainers test). Events exist only for committed transactions. A Kafka outage does not fail incident creation; events publish later from the outbox.
- **Error or empty states**: Poison messages land in the dead-letter topic and raise an alert metric.

### F6 Caching and Rate Limiting
- **Tier**: Tier 2
- **User need**: Keep the app fast and protect expensive endpoints.
- **What it does**: Redis caches dashboard KPIs, analytics overview and service health (30-60 s TTL). Fixed-window limits: login 5 per minute per IP and email, AI analysis 10 per user per hour, knowledge search 60 per minute per user (all configurable).
- **Inputs and outputs**: Out: cached responses, 429 with Retry-After.
- **Acceptance criteria**: Cache is invalidated on incident changes. If Redis is down, reads use the database and limits fall back to a per-instance in-memory limiter. Behaviour covered by integration tests.
- **Error or empty states**: 429 message tells the user when to retry.

### F7 Knowledge Base and Semantic Search
- **Tier**: Tier 2
- **User need**: Find runbooks, postmortems and past incidents by meaning.
- **What it does**: ENGINEER and ADMIN add runbooks, FAQs and postmortems (Markdown or text, max 200 KB). Async ingestion parses, chunks (about 500 tokens, 50 overlap, heading-aware), embeds (384 dimensions) and stores in pgvector. Resolved incidents are ingested automatically as INCIDENT documents. Semantic search with service and source filters, restricted by role. Similar-incidents endpoint.
- **Inputs and outputs**: In: documents, query text. Out: ranked chunks with similarity and snippet.
- **Acceptance criteria**: Identical content (same hash) is rejected. Ingestion status is visible (PENDING, PROCESSING, INGESTED, FAILED). Search p95 under 500 ms on seed data. SUPPORT and VIEWER never receive ENGINEERING documents. Re-ingestion is idempotent.
- **Error or empty states**: Unsupported type or oversize: rejected. No results: clear message.

### F8 AI Triage and Summary
- **Tier**: Tier 2
- **User need**: Classify and understand an incident immediately.
- **What it does**: On incident creation (when `ai.auto_analyze` is on) and on demand, an analysis job returns category, suggested priority, summary and diagnostic steps. One click accepts the suggested category or priority; the AI never changes incident fields by itself.
- **Inputs and outputs**: In: incident text. Out: validated structured result (schema in TRD section 8).
- **Acceptance criteria**: Model output is validated against the schema and category list. Analysis stores model, embedding model, prompt version, tokens and latency. Analysis status is visible (PENDING, RUNNING, COMPLETED, INSUFFICIENT_EVIDENCE, FAILED) with a retry action. An AI failure never blocks the incident workflow.
- **Error or empty states**: Provider down: FAILED status with retry. Invalid output: one automatic retry, then FAILED.

### F9 Root Cause Analysis and Recommendations
- **Tier**: Tier 2
- **User need**: Understand why it happened and what to do next, with proof.
- **What it does**: Retrieval-augmented analysis: embed the incident, retrieve top-k chunks (default 5) above a minimum similarity (default 0.40) that the user may read, add a summary of the last 60 minutes of metrics and the last 24 hours of deployments for the service, and ask the LLM for ranked root-cause hypotheses (confidence, evidence references) and recommended actions with rationale. Users give feedback (helpful or not, accept or reject each action).
- **Inputs and outputs**: Out: ranked causes, evidence cards, recommendations.
- **Acceptance criteria**: Every evidence reference points to a retrieved chunk; invalid references are dropped and logged. If nothing passes the similarity threshold the status is INSUFFICIENT_EVIDENCE and the UI says so. Actions are labelled recommended, not executed. Prompts contain only data the requesting user may read. Feedback is stored and counted in F10.
- **Error or empty states**: Insufficient evidence and provider failure each have distinct UI states.

### F10 Analytics
- **Tier**: Tier 2
- **User need**: Understand reliability and AI usefulness over time.
- **What it does**: MTTR, MTTD, volume by day or week and priority, SLA compliance, recurring causes, AI feedback metrics. Definitions in TRD section 10. Ranges 7, 30 or 90 days.
- **Inputs and outputs**: Out: KPI values and chart series.
- **Acceptance criteria**: Results match reference SQL tests on seed data. Responses cached for 60 seconds.
- **Error or empty states**: No incidents in range: empty-state message.

### F11 Metrics and Predictive Detection
- **Tier**: Tier 3
- **User need**: Warn before a service fails.
- **What it does**: Ingest CPU, memory, disk, latency p95, throughput and error rate per service. A detector produces anomaly score (0-1) and risk level; with the optional ML service it also produces failure probability for the next 30 minutes. High-risk predictions publish `failure.prediction.created`, appear on the dashboard and can be turned into a prefilled incident with one click.
- **Inputs and outputs**: In: metric batches. Out: predictions and service health.
- **Acceptance criteria**: Batch ingest is idempotent on (service, metric, timestamp). On the synthetic set the detector reaches at least 0.80 recall with at most 5 percent false positives (targets to be confirmed by the first evaluation run). Failure probability is shown only when produced by a trained model.
- **Error or empty states**: No metrics: service health shows No data.

### F12 Real-Time Updates
- **Tier**: Tier 3
- **User need**: See changes without refreshing.
- **What it does**: WebSocket (STOMP) pushes incident changes, analysis completion, predictions and assignment notifications.
- **Inputs and outputs**: Out: live events.
- **Acceptance criteria**: Auto-reconnect with backoff; polling every 30 seconds while disconnected; JWT checked on connect; users receive only permitted data.
- **Error or empty states**: Connection lost banner.

### F13 Administration
- **Tier**: Tier 1 (users, services), Tier 2 (AI and SLA settings, audit)
- **User need**: Control access and policy.
- **What it does**: ADMIN creates users, changes roles, disables users, resets passwords (temporary password), manages services, edits AI settings (enabled, auto-analyze, top-k, minimum similarity, rate limits) and SLA targets, and browses the audit log.
- **Inputs and outputs**: In: admin forms. Out: changes applied without restart.
- **Acceptance criteria**: Role and setting changes are audited. The last active ADMIN cannot be disabled or demoted. Settings are validated.
- **Error or empty states**: Validation errors inline.

### F14 AI Evaluation Harness
- **Tier**: Tier 3
- **User need**: Prove the AI is useful and grounded.
- **What it does**: An offline runner evaluates held-out synthetic incidents for category accuracy, retrieval recall@5, groundedness (valid evidence references) and correct use of INSUFFICIENT_EVIDENCE on out-of-scope incidents, then writes `docs/ai-evaluation.md`.
- **Inputs and outputs**: Out: report with metrics and per-case failures.
- **Acceptance criteria**: Runs from a Maven profile and CI. Floors are set after the first baseline and CI fails below them. Report states that data is synthetic.
- **Error or empty states**: Provider unavailable: run marked incomplete, not passed.

## 5. Scope
| Included in version one | Explicitly excluded | Possible later additions |
|---|---|---|
| F1-F14 delivered tier by tier. Tier 1 and Tier 2 must ship; Tier 3 ships in the order given in the Implementation Plan. | Automated remediation or any tool execution; email, Slack or SMS notifications; SSO; multi-tenancy; real production data; log ingestion pipelines; mobile app | Hybrid retrieval and reranking; model routing; read-only diagnostic tools; service dependency graph; approval workflow for remediation; multi-tenant SaaS; Slack and email; OpenAPI client generation |

## 6. User stories
- As a support engineer, I want to report an incident and see similar past cases so that I do not start from zero.
- As an engineer, I want ranked root-cause hypotheses with cited evidence so that I can verify them quickly.
- As an SRE, I want to be warned about anomalous services so that I can act before an outage.
- As a manager, I want MTTR, SLA compliance and recurring causes so that I can prioritise reliability work.
- As an admin, I want roles and audit logs so that access is controlled and traceable.
- As a reviewer, I want to see how the AI is evaluated so that I can trust its output.

## 7. Functional requirements (cross-cutting)
- **FR-1**: The AI never executes actions and never modifies incident fields without a human action.
- **FR-2**: Every AI result stores model, prompt version, retrieved evidence, token use and latency for reproducibility.
- **FR-3**: AI or Kafka or Redis failures degrade features gracefully and never block incident creation or updates.
- **FR-4**: Authorisation is applied to data before it is placed in a prompt, and to every API response.
- **FR-5**: All state-changing and AI actions are written to the append-only audit log.
- **FR-6**: Every list endpoint is paginated and filterable.

## 8. Non-functional requirements
- **Performance**: p95 read APIs under 300 ms with 50 concurrent users on seed data (AI excluded). Dashboard under 2 s. Vector query under 100 ms in the database. AI analysis p95 under 45 s (provider dependent).
- **Accessibility**: WCAG 2.1 AA; keyboard operable; meaning never conveyed by colour alone.
- **Privacy**: Synthetic data only. Personal data limited to name, email and role.
- **Security**: OWASP ASVS level 1 mindset: validation, secure session handling, least privilege, secrets outside the repo, prompt-injection defences.
- **Browser support**: Latest Chrome, Edge, Firefox and Safari; usable at 1024 px and wider.
- **Reliability**: At-least-once event delivery with idempotent consumers; graceful degradation without Redis, Kafka or the LLM.

## 9. Success criteria
- End-to-end demo: create incident, automatic triage, similar incidents, RCA with evidence, feedback, analytics update.
- Tier 1 and Tier 2 checklists in the Implementation Plan pass; known failure modes tested (LLM down, Redis down, duplicate event).
- AI evaluation report published with honest synthetic-data caveats.
- `docker compose up` starts the full stack; CI is green; no secrets in the repository.

## 10. Assumptions and open questions
- SLA defaults, similarity threshold (0.40), top-k (5), confidence bands and detector targets are starting values to tune.
- Spring AI and Spring Boot versions must be verified for compatibility at project start.
- Open: AWS Kafka choice (MSK versus a single broker for the demo) depends on budget.
- Open: whether the optional Python ML service (P9b) is built.
