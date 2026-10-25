# IncidentIQ — Evaluator & Demonstration Walkthrough Script

This script provides an end-to-end, reproducible walkthrough of IncidentIQ's capabilities across Tier 1 (MVP), Tier 2 (Advanced), and Tier 3 (Expert) features.

---

## 1. Demo Credentials & Personas

The system bootstraps the following role-based accounts out of the box:

| Persona | Email | Password | Role Permissions |
|---|---|---|---|
| **Platform Administrator** | `admin@incidentiq.local` | `AdminInitialPassword123!` | System settings, user administration, service registry, audit logs |
| **Site Reliability Engineer** | `engineer@incidentiq.local` | `AdminInitialPassword123!` | Incident triage, resolution, RCA, metric ingestion, deployments |
| **Customer Support Lead** | `support@incidentiq.local` | `AdminInitialPassword123!` | Incident creation, customer impact logging, read-only diagnostics |
| **Executive Viewer** | `viewer@incidentiq.local` | `AdminInitialPassword123!` | Read-only access to dashboards, SLAs, and analytics |

---

## 2. Walkthrough Scenarios

### Scenario 1: Incident Creation & SLA State Machine
1. Open the application at `http://localhost:5173` (or Docker port `80`).
2. Log in using `engineer@incidentiq.local`.
3. Notice the **Live Status Pill** in the header indicating 🟢 `Live` (connected to STOMP WebSocket broker at `/ws`).
4. Click **Report Incident** in the navigation header.
5. Enter incident details:
   - **Title**: `Payment gateway HTTP 504 timeout on checkout service`
   - **Service**: `checkout-api`
   - **Severity**: `CRITICAL`
   - **Priority**: `P1`
   - **Category**: `APPLICATION`
   - **Log Snippet**:
     ```
     2026-10-25 14:02:11.892 ERROR [payment-worker-4] c.i.payment.GatewayClient -
     java.net.SocketTimeoutException: Read timed out after 5000ms connecting to api.payment-partner.internal
     ```
6. Click **Submit Incident**.
7. **Expected Result**:
   - Incident is assigned identifier `#INC-0014`.
   - The SLA progress bar displays a 4-hour countdown with live elapsed timers.
   - Initial timeline event `CREATED` and `LOG_ADDED` are recorded.
   - An instant in-app notification appears under the bell icon.

---

### Scenario 2: Evidence-Grounded AI Diagnostic Pipeline (F8, F9)
1. On the Incident Detail page, navigate to the **AI Incident Diagnostics** panel.
2. Click **Run Diagnostic Analysis**.
3. **Inspect the AI Output**:
   - **Summary**: Concise operational failure synthesis.
   - **Confidence Score**: e.g., `85%` with visual color-coded badge.
   - **Root Cause Hypotheses**: Ranked hypotheses citing specific chunk IDs from the knowledge base.
   - **Evidence Groundedness**: Click on any cited runbook link (e.g., `Payment Gateway TLS Handshake Runbook`) to inspect the verified text snippet. Notice that **no citations are hallucinated**.
   - **Action Recommendations**: 2 to 3 actionable remediation steps.
   - **Advisory Banner**: Notice the persistent disclaimer:  
     *“AI output is strictly advisory. Recommendations are suggestions that the system never executes. Humans make every decision.”*
4. Click **Apply Suggested Classification** to automatically update the incident category and priority.

---

### Scenario 3: Real-Time In-App Notifications & WebSocket Updates (F12)
1. In another browser tab or via the PowerShell generator script:
   ```powershell
   ./scripts/seed_live_prediction.ps1
   ```
2. **Observe Instant Real-Time Reaction**:
   - A floating **Live Alert Toast** slides into the top-right corner warning of anomaly detection on `checkout-api`.
   - The **Notification Bell** badge increments with a red counter.
   - Click the **Bell Icon** to inspect the notification flyout showing category icon, message, and relative timestamp.
   - Click the notification to navigate directly to the affected incident or alert.

---

### Scenario 4: Predictive Service Health & Statistical Anomaly Detection (F11)
1. Click **Service Health** in the navigation bar.
2. Review the health matrix of registered microservices:
   - Service Criticality (`CRITICAL`, `HIGH`, `MEDIUM`).
   - Health Risk Status (`HEALTHY`, `DEGRADED`, `ANOMALOUS`).
   - Latest 24h CPU, memory, and p99 latency metrics.
   - Active predictions list.
3. For any service flagged as `ANOMALOUS`:
   - Click **Inspect Contributing Metrics** (e.g. CPU z-score spike > 3.0σ).
   - Click **Create Incident from Prediction** to seamlessly promote a predictive anomaly into an active incident before an outage occurs.

---

### Scenario 5: Operational Analytics & Reliability KPIs (F10)
1. Click **Analytics** in the navigation bar.
2. Toggle between **7 Days**, **30 Days**, and **90 Days** presets.
3. Review executive KPIs:
   - **Mean Time to Resolution (MTTR)** and **Mean Time to Detect (MTTD)**.
   - **SLA Compliance Share** (percentage of incidents resolved within SLA target).
   - **Incident Volume Trends** (interactive priority-split bar chart).
   - **Recurring Failure Causes** (recurrence cluster list identifying repeat failure patterns within 30 days).
   - **AI Feedback & Acceptance Rate** (proportion of helpful ratings and applied recommendations).

---

### Scenario 6: System Governance, Security & Observability (F13, F14)
1. Log in as `admin@incidentiq.local`.
2. Navigate to **Admin Settings**:
   - **Audit Logs**: Filterable audit trail displaying user, IP, action, and `correlation_id` for every state change.
   - **User Administration**: Role management and account lock controls.
3. Verify Observability Endpoints:
   - **Prometheus Domain Telemetry**:
     ```bash
     curl -s http://localhost:8080/actuator/prometheus | grep incidentiq
     ```
     Inspect custom counters: `incidentiq_incidents_created_total`, `incidentiq_metrics_ingested_total`.
   - **AI Evaluation Harness**:
     ```bash
     cd backend
     ./mvnw test -Dtest=AiEvaluationHarnessTest
     ```
     Inspect [`docs/ai-evaluation.md`](file:///c:/Users/Asus/OneDrive/Desktop/IncidentIQ/docs/ai-evaluation.md) for 100% groundedness and 100% out-of-scope rejection.
