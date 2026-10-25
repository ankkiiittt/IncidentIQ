# IncidentIQ — Architecture & Pinned Versions

## Pinned Technologies & Versions

| Component | Pinned Version | Notes |
|---|---|---|
| **Java** | 21 LTS | Language level and target bytecode; virtual threads enabled |
| **Spring Boot** | 3.3.5 | Latest stable 3.3.x line |
| **Spring Data JPA / Hibernate** | 6.5.x | Via Spring Boot 3.3.5 starter; `ddl-auto: validate` |
| **Spring Security** | 6.3.x | JWT authentication, RBAC method security |
| **Spring Kafka** | 3.2.x | Transactional outbox integration and idempotent consumer |
| **Spring AI** | 1.0.0-M3 / 1.0.0-M4 | ChatClient + local ONNX embedding transformers |
| **Flyway** | 10.10.x | Database migrations (PostgreSQL dialect) |
| **PostgreSQL** | 16 with pgvector | Vector embedding dimension: 384 (`vector_cosine_ops`) |
| **Redis** | 7-alpine | Cache & rate limiting |
| **Apache Kafka** | 3.7.0 (KRaft) | Event stream broker |
| **springdoc-openapi** | 2.6.0 | OpenAPI 3.0 / Swagger UI documentation |
| **Node.js** | 20+ (using v24.11.1) | Frontend runtime |
| **React** | 18.3.1 | Single page application library |
| **TypeScript** | 5.5+ | Strict type checking enabled |
| **Tailwind CSS** | 3.4.x | Token-based styling per UI/UX Brief |
| **Vite** | 5.4.x | Build and dev server |
| **TanStack Query** | 5.51+ | Server state management & caching |

## Network Ports (Local Development)

| Service | Host Port | Internal Port | Protocol |
|---|---|---|---|
| Frontend (Vite) | `5173` | 5173 | HTTP |
| Backend (Spring Boot) | `8080` | 8080 | HTTP / REST & STOMP |
| PostgreSQL (pgvector) | `5432` | 5432 | TCP |
| Redis | `6379` | 6379 | TCP |
| Kafka (KRaft Host) | `9092` | 9092 | PLAINTEXT |
| Prometheus (Optional) | `9090` | 9090 | HTTP |
| Grafana (Optional) | `3000` | 3000 | HTTP |

## Key Architectural Invariants
1. **AI Advisory Only**: AI recommendations are never executed automatically.
2. **Pre-Retrieval Authorization**: Knowledge vector queries enforce user access levels (`PUBLIC` vs `ENGINEERING`) at the SQL layer before any chunk text enters the prompt context.
3. **No Entities in API Responses**: Every REST controller maps to clean, strongly typed Java Records for DTOs.
4. **Resilient Failure Modes**: Outage of Redis, Kafka, or LLM degrades gracefully and never blocks incident creation.
