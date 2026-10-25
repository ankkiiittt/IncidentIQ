# IncidentIQ REST & WebSocket API Specification

Base URL: `http://localhost:8080/api` (or `/api` via Nginx)  
Swagger UI: `http://localhost:8080/swagger-ui.html`  
OpenAPI 3.0 JSON: `http://localhost:8080/v3/api-docs`  
Error Format: RFC 7807 Problem Details (`application/problem+json`)

---

## 1. Authentication & Session

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/auth/login` | Public | Authenticates via email/password. Returns JWT access token and sets refresh cookie |
| `POST` | `/auth/refresh` | Public (Cookie) | Rotates refresh token and issues new access token |
| `POST` | `/auth/logout` | Authenticated | Revokes current refresh token |
| `POST` | `/auth/change-password` | Authenticated | Updates password for authenticated user |
| `GET` | `/auth/me` | Authenticated | Retrieves profile of currently authenticated user |

---

## 2. User Administration (ADMIN only)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/users` | ADMIN | Paginated list of user accounts with search and role filters |
| `POST` | `/users` | ADMIN | Creates new user account |
| `GET` | `/users/{id}` | ADMIN | Retrieves user details by UUID |
| `PATCH` | `/users/{id}` | ADMIN | Modifies user role or active status |
| `POST` | `/users/{id}/reset-password` | ADMIN | Resets temporary password for user |

---

## 3. Service Catalog & Health

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/services` | All | Lists registered services (with `activeOnly` query filter) |
| `POST` | `/services` | ADMIN | Registers a new service in catalog |
| `GET` | `/services/{id}` | All | Retrieves service details |
| `PATCH` | `/services/{id}` | ADMIN | Updates service metadata |
| `GET` | `/services/{id}/health` | All | Real-time health status, latest metrics, active predictions, open incidents |
| `GET` | `/services/{id}/deployments` | All | Lists deployment history for service |
| `POST` | `/services/{id}/deployments` | ENGINEER, ADMIN | Records a new deployment event |

---

## 4. Incident Management Lifecycle

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/incidents` | All | Paginated incident list with filters (`q`, `status`, `priority`, `serviceId`, `from`, `to`) |
| `POST` | `/incidents` | SUPPORT, ENGINEER, ADMIN | Creates new incident with SLA calculation and timeline event |
| `GET` | `/incidents/{id}` | All | Detailed incident record with SLA elapsed/remaining and timeline summary |
| `PATCH` | `/incidents/{id}` | Role Matrix | Updates editable incident fields (requires JPA `@Version` optimistic lock) |
| `POST` | `/incidents/{id}/status` | Role Matrix | Executes validated state transition (`ACKNOWLEDGED`, `IN_PROGRESS`, etc.) |
| `POST` | `/incidents/{id}/assign` | SUPPORT, ENGINEER, ADMIN | Assigns incident to team member |
| `POST` | `/incidents/{id}/escalate` | SUPPORT, ENGINEER, ADMIN | Escalates priority (e.g. P2 to P1) |
| `POST` | `/incidents/{id}/resolve` | ENGINEER, ADMIN | Resolves incident (requires root cause and resolution description) |
| `POST` | `/incidents/{id}/close` | ENGINEER, ADMIN | Closes resolved incident |
| `POST` | `/incidents/{id}/reopen` | ENGINEER, ADMIN | Reopens incident with reason |
| `GET` | `/incidents/{id}/timeline` | All | Full chronological timeline of events |
| `GET` | `/incidents/{id}/comments` | All | Comments list |
| `POST` | `/incidents/{id}/comments` | All | Posts comment |
| `POST` | `/incidents/{id}/logs` | Role Matrix | Appends log snippet (max 4 KB) |
| `GET` | `/incidents/{id}/similar` | All | Role-filtered semantic search for similar past resolved incidents |

---

## 5. AI Diagnostics & Recommendations (F8, F9)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/incidents/{id}/ai/analyze` | SUPPORT, ENGINEER, ADMIN | Requests RAG diagnostic analysis (returns 202 Accepted) |
| `GET` | `/incidents/{id}/ai/analyses` | All | Lists historical analyses for incident |
| `GET` | `/ai/analyses/{id}` | All | Detailed analysis with ranked hypotheses, citations, and recommendations |
| `POST` | `/incidents/{id}/ai/accept-suggestion` | SUPPORT, ENGINEER, ADMIN | Accepts AI suggested category or priority |
| `POST` | `/recommendations/{id}/feedback` | SUPPORT, ENGINEER, ADMIN | Submits helpful rating and accept/reject decision |

---

## 6. Knowledge Base & Vector Search (F7)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/knowledge/documents` | Role-filtered | Lists documents accessible by user's access level (`PUBLIC` vs `ENGINEERING`) |
| `POST` | `/knowledge/documents` | ENGINEER, ADMIN | Ingests runbook or postmortem with automatic chunking and embedding |
| `GET` | `/knowledge/documents/{id}` | Role-filtered | Document detail with ingestion status |
| `POST` | `/knowledge/search` | Role-filtered | Semantic pgvector similarity search with role enforcement |

---

## 7. Predictive Health & Telemetry Ingestion (F11)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/metrics/batch` | API Key, ENGINEER, ADMIN | Ingests telemetry metrics; triggers statistical anomaly detection |
| `GET` | `/alerts/predictions` | All | Lists active failure predictions |
| `POST` | `/alerts/predictions/{id}/create-incident` | SUPPORT, ENGINEER, ADMIN | Promotes prediction to incident |
| `POST` | `/alerts/predictions/{id}/dismiss` | ENGINEER, ADMIN | Dismisses active prediction |

---

## 8. Operational Analytics (F10)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/analytics/overview` | All | Executive summary: MTTR, MTTD, open count, SLA compliance % (cached 30s) |
| `GET` | `/analytics/volume` | All | Daily and weekly incident volume time series split by priority |
| `GET` | `/analytics/sla` | All | SLA compliance rate and breach trends |
| `GET` | `/analytics/causes` | All | Recurring root causes clustering analysis |
| `GET` | `/analytics/ai-feedback` | All | AI feedback metrics and recommendation acceptance rates |

---

## 9. In-App Notifications (F12)

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `GET` | `/notifications` | Authenticated | Lists notifications for current user |
| `GET` | `/notifications/unread-count` | Authenticated | Returns unread badge count |
| `POST` | `/notifications/{id}/read` | Authenticated | Marks notification as read |
| `POST` | `/notifications/read-all` | Authenticated | Marks all notifications as read |

---

## 10. Real-Time WebSocket (STOMP over SockJS)

- **Connection Handshake**: `GET /ws/info` (SockJS HTTP) / `ws://localhost:8080/ws`
- **Authentication**: JWT token sent in `Authorization: Bearer <token>` or `access_token` query parameter during `CONNECT` frame.
- **Subscribed Channels**:
  - `/topic/incidents`: Real-time incident creation and status changes.
  - `/topic/incidents/{id}`: Detailed live incident updates (status, comments, AI completions).
  - `/topic/predictions`: Predictive anomaly alerts.
  - `/user/queue/notifications`: User-directed in-app notifications.
  - `/topic/notifications/{userId}`: Broadcast fallback for user notifications.
