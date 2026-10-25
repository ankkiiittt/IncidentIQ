# 06 - Implementation Plan

## 1. Current project state
- **Existing code**: None. Greenfield monorepo named incidentiq.
- **Reusable parts**: None. The synthetic dataset and evaluation set are produced in this project.
- **Missing foundations**: Everything: repositories, build, Compose environment, CI, schema, backend, frontend, dataset, AI layer, deployment.
- **Risks**: Scope size; Spring AI and library version drift; LLM variability; Kafka and AWS cost; synthetic data realism.

## 2. Build principles
- Preserve approved scope. Do not add features, change the stack or skip phases.
- Work in small verifiable phases; test after every phase; do not start a phase while required checks of previous phase fail.
- Backend first for each capability, then its UI. Keep AI behind interfaces so tests use a fake client.
- After each phase stop and report: files changed, tests run and results, open issues, and the recommended next phase.
- Milestones: after P4 the MVP (Tier 1) is demonstrable; after P8 the Advanced build (Tier 2); after P12 the Expert build (Tier 3).

## 3. Repository structure
```
incidentiq/
  backend/            # Java 21, Spring Boot 3.x, Maven
    src/main/java/com/incidentiq/{auth,incident,catalog,knowledge,ai,analytics,metrics,notification,messaging,common}
    src/main/resources/db/migration/ # Flyway V1..Vn
    src/test/java/... # unit, repository, integration (Testcontainers), security
  frontend/           # React + TypeScript + Vite + Tailwind
  ml-service/         # optional FastAPI service (P9b)
  dataset/            # generate.py, generated CSV/JSON, runbooks/, postmortems/, eval/
  infra/              # compose profiles, prometheus.yml, AWS deployment notes or IaC
  docs/               # 00..06 specification files, ai-evaluation.md, architecture.md, api.md
  docker-compose.yml
  .github/workflows/
  README.md
  .env.example
```

## 4. Ordered phases
| Phase | Goal | Features | Tier | Depends on |
|---|---|---|---|---|
| P0 | Foundation: repo, build, Compose (Postgres+pgvector, Redis, Kafka), Flyway, CI, common web layer | - | 1 | - |
| P1 | Auth and RBAC backend, user admin API, bootstrap admin | F1, F13 (users) | 1 | P0 |
| P2 | Services, deployments, incident core: CRUD, state machine, SLA, timeline, comments, search, audit | F2, F3 | 1 | P1 |
| P3 | Frontend shell, Login, Incident List, Create, Detail, Account, Admin (users, services) | F1-F3, F13 UI | 1 | P2 |
| P4 | Dashboard with KPI queries and service list (MVP milestone) | F4 | 1 | P3 |
| P5 | Outbox, Kafka producers and idempotent consumers, DLT; Redis cache and rate limits | F5, F6 | 2 | P2 |
| P6 | Embeddings, knowledge ingestion, pgvector search, similar incidents, synthetic dataset and seed | F7 | 2 | P5 |
| P7 | AI triage and RCA pipeline, validation, resilience, AI UI (S5 panel, S6), AI settings, feedback | F8, F9, F13 (AI) | 2 | P6 |
| P8 | Analytics endpoints and screen with caching (Advanced milestone) | F10 | 2 | P7 |
| P9 | Metrics ingestion, statistical detector, predictions, Service Health UI | F11 | 3 | P5 |
| P9b | Optional ML service (IsolationForest, XGBoost) and fallback | F11 | 3 | P9 |
| P10 | WebSocket updates and notifications | F12 | 3 | P5, P7, P9 |
| P11 | Observability, security hardening, AI evaluation harness, load tests | F14, NFRs | 3 | P8 |
| P12 | Dockerise all, CI/CD, AWS deployment, documentation and demo script | - | 3 | P11 |

## 5. Phase P0 Details
- **Goal**: Runnable skeleton with quality gates.
- **Work and files**: Maven wrapper, Spring Boot app, Actuator, springdoc, Flyway V1 (extensions), Docker Compose with pgvector image, Redis, Kafka (KRaft), Vite React TypeScript Tailwind skeleton, ESLint, Prettier, Spotless or Checkstyle, GitHub Actions for build and tests, common package (ProblemDetail error handling, correlation ID filter, JSON logging, base Testcontainers config).
- **Completion criteria**: `docker compose up` starts infra; backend starts and `/actuator/health` is UP; base Testcontainers / test passes; frontend builds.
- **Risks**: Version incompatibilities. Pin and record versions in `docs/architecture.md`.

## 6. Milestones and Verification
- **Tier 1 (MVP)**: After P4
- **Tier 2 (Advanced)**: After P8
- **Tier 3 (Expert)**: After P12
