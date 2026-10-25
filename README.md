# IncidentIQ

> **AI-Powered Enterprise Incident Intelligence Platform**

IncidentIQ is a production-grade incident management platform with an evidence-first AI layer. It unifies classic incident management with retrieval-augmented analysis (RAG) whose hypotheses cite verified evidence and whose recommendations are strictly advisory (never auto-executed).

## Core Capabilities
- **Incident Lifecycle**: Report, triage, escalate, resolve, and audit incidents with strict SLA tracking.
- **Evidence-Grounded AI Analysis**: Contextual diagnosis incorporating past incidents, runbooks, metrics, and deployments.
- **Vector Search & Knowledge Base**: Semantic search powered by PostgreSQL `pgvector` with strict pre-retrieval role-based access filtering.
- **Event-Driven Resilience**: Transactional outbox to Kafka with idempotent processing and dead-letter queues.

## Technology Stack
- **Backend**: Java 21, Spring Boot 3.3.5, Spring Data JPA, Spring Security, Spring Kafka, Spring AI
- **Database & Messaging**: PostgreSQL 16 with `pgvector`, Redis 7, Apache Kafka (KRaft)
- **Frontend**: React 18, TypeScript, Tailwind CSS, TanStack Query, Vite
- **Testing**: JUnit 5, Mockito, Testcontainers, Playwright

## Quick Start (Local Development)

### 1. Prerequisites
- Java 21 JDK (or Java 21+ compatible runtime)
- Node.js 20+ and npm
- Docker and Docker Compose

### 2. Start Infrastructure
```bash
docker compose up -d postgres redis kafka
```

### 3. Start Backend
```bash
cd backend
./mvnw spring-boot:run
```
Backend starts on `http://localhost:8080`.
- Health check: `http://localhost:8080/actuator/health`
- Swagger / OpenAPI: `http://localhost:8080/swagger-ui.html`

### 4. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173`.

---
*Disclaimer: IncidentIQ AI output is advisory. Recommendations are suggestions that the system never executes. Humans make every decision.*
