# 00 - App Brief

| Attribute | Details |
|---|---|
| **App name** | IncidentIQ |
| **One-line idea** | A platform that triages IT incidents, finds similar past cases and runbooks, and produces evidence-grounded root-cause hypotheses and recommended actions. |
| **Problem it solves** | Support and SRE teams lose time classifying incidents, searching old tickets and runbooks, and guessing causes. Knowledge is scattered and recurring failures are re-investigated from scratch. |
| **Who it is for** | Support engineers and developers investigating incidents; SRE and DevOps teams; engineering managers reviewing reliability; administrators. Secondary: hiring managers reviewing the project. |
| **Main action** | Report an incident and receive an evidence-backed AI analysis with similar incidents and next steps. |
| **Must-have features** | **Tier 1 (MVP)**: authentication with roles, service registry, incident management, dashboard, admin.<br>**Tier 2 (Advanced)**: Kafka events, Redis caching and rate limiting, knowledge base with pgvector search, AI triage, RAG root-cause analysis and recommendations, analytics.<br>**Tier 3 (Expert)**: metrics and predictive detection, real-time updates, AI evaluation harness, observability, AWS deployment. |
| **Nice-to-have** | Hybrid retrieval (keyword plus vector), reranking, model routing, read-only diagnostic tool calling, service dependency graph, approval workflow for automated remediation, multi-tenant SaaS, email or Slack notifications, SSO. |
| **Platform** | Web single-page app, desktop-first, usable on tablets. |
| **Business model** | None. Portfolio project positioned for Java full-stack, backend, cloud and AI roles. |
| **Constraints** | Deadline: none fixed; three milestones (MVP, Advanced, Expert). Budget: free tiers plus LLM API usage; AWS cost only in the last phase. Tools: Java 21, Spring Boot 3.x, Spring AI, PostgreSQL with pgvector, Redis, Kafka, React with TypeScript and Tailwind, Docker, GitHub Actions, AWS. |
| **Privacy and security** | Synthetic data only. JWT with role-based access. Secrets in environment or a secret manager. Untrusted text isolated in prompts. Data authorised before it enters a prompt. |
| **References** | Common incident tools (for layout familiarity only). Blueprint document: IncidentIQ Full-Stack Java + AI. |

---

### App Disclaimer
> **Disclaimer to show in the app**: IncidentIQ AI output is advisory. Recommendations are suggestions that the system never executes. Humans make every decision.
