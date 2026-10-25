# IncidentIQ — AWS Production Deployment Architecture & Operations Guide

This guide details the reference production deployment architecture for IncidentIQ on Amazon Web Services (AWS) in compliance with the Technical Requirements Document (TRD Section 12).

---

## 1. AWS Architecture Topology

```
                          Internet Traffic
                                 │
                                 ▼
                     [ AWS Route 53 (DNS) ]
                                 │
                                 ▼
              [ Application Load Balancer (ALB) ]
                     HTTPS (ACM TLS Cert)
                 │                          │
                 ▼                          ▼
      [ ECS Fargate (Frontend) ]  [ ECS Fargate (Backend API & WS) ]
       Nginx Reverse Proxy / SPA    Spring Boot 3.3.5 / Java 21
       (Port 80 / Public Subnet)   (Port 8080 / Private Subnet)
                                            │
               ┌────────────────────────────┼───────────────────────────┐
               ▼                            ▼                           ▼
     [ Amazon RDS Postgres 16 ]    [ Amazon ElastiCache ]     [ Amazon MSK Kafka ]
     Multi-AZ with pgvector        Redis 7 (In-transit enc)   KRaft / Multi-AZ
     (Private DB Subnets)          (Private Cache Subnet)     (Private Broker Subnets)
```

---

## 2. Infrastructure Components

### A. Network & VPC
- **VPC CIDR**: `10.0.0.0/16`
- **Subnet Allocation**:
  - `2x Public Subnets`: `10.0.1.0/24` (AZ-a), `10.0.2.0/24` (AZ-b) hosting ALB and NAT Gateways.
  - `2x Private Application Subnets`: `10.0.11.0/24` (AZ-a), `10.0.12.0/24` (AZ-b) hosting ECS Fargate tasks.
  - `2x Private Data Subnets`: `10.0.21.0/24` (AZ-a), `10.0.22.0/24` (AZ-b) hosting RDS, ElastiCache, and MSK.
- **Security Groups**:
  - `alb-sg`: Ingress `443/80` from `0.0.0.0/0`.
  - `ecs-backend-sg`: Ingress `8080` from `alb-sg`. Egress all to private VPC.
  - `rds-sg`: Ingress `5432` from `ecs-backend-sg`.
  - `redis-sg`: Ingress `6379` from `ecs-backend-sg`.
  - `msk-sg`: Ingress `9092/9094` from `ecs-backend-sg`.

### B. Compute (AWS ECS Fargate)
- **Launch Type**: AWS Fargate (Serverless container execution)
- **Backend Service**:
  - 2 to 10 Tasks with Auto-Scaling (Target tracking: Average CPU > 70% or Target Response Time < 200ms).
  - CPU: `1024` (1 vCPU), Memory: `2048` (2 GB).
  - Virtual threads (`spring.threads.virtual.enabled: true`) provide high concurrent throughput.
- **Frontend Service**:
  - 2 Tasks (Multi-AZ) behind ALB path routing (`/` and `/assets/*`).
  - CPU: `512` (0.5 vCPU), Memory: `1024` (1 GB).

### C. Managed Data Stores
- **Amazon RDS for PostgreSQL 16**:
  - Instance Class: `db.m6g.large` (Multi-AZ Deployment).
  - Parameter Group: `shared_preload_libraries = 'vector'`.
  - Storage: Auto-scaling gp3 (100 GB baseline to 1 TB, 3,000 IOPS).
- **Amazon ElastiCache for Redis 7**:
  - Node Type: `cache.m6g.large` (2 nodes with Multi-AZ automatic failover).
  - In-transit encryption and token authentication enabled.
- **Amazon MSK (Managed Streaming for Apache Kafka)**:
  - 3-broker cluster across 3 availability zones with IAM / SCRAM authentication.

---

## 3. Secrets & Configuration (AWS Secrets Manager)

The following production secrets must be provisioned in AWS Secrets Manager and referenced by ECS Task Definitions:

| Secret Name | Key | Description |
|---|---|---|
| `incidentiq/prod/database` | `url`, `username`, `password` | Master RDS PostgreSQL connection string |
| `incidentiq/prod/jwt` | `secret` | 256-bit cryptographically secure HMAC secret |
| `incidentiq/prod/ai` | `anthropic_api_key` | Anthropic Claude API Key for diagnostic RAG |
| `incidentiq/prod/ingest` | `api_key` | Telemetry batch ingestion API key |
| `incidentiq/prod/admin` | `bootstrap_password` | Initial seed administrator password |

---

## 4. Operational Playbook: Deployment Steps

### 1. Build and Push Images to Amazon ECR
```bash
# Log in to Amazon ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com

# Build and tag images
docker build -t incidentiq-backend:latest ./backend
docker tag incidentiq-backend:latest <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/incidentiq-backend:latest
docker push <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/incidentiq-backend:latest

docker build -t incidentiq-frontend:latest ./frontend
docker tag incidentiq-frontend:latest <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/incidentiq-frontend:latest
docker push <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/incidentiq-frontend:latest
```

### 2. Register Task Definitions & Update Services
```bash
# Register backend and frontend task definitions
aws ecs register-task-definition --cli-input-json file://infra/aws/ecs-task-backend.json
aws ecs register-task-definition --cli-input-json file://infra/aws/ecs-task-frontend.json

# Rolling deployment to ECS cluster
aws ecs update-service --cluster incidentiq-prod --service incidentiq-backend --force-new-deployment
aws ecs update-service --cluster incidentiq-prod --service incidentiq-frontend --force-new-deployment
```

### 3. Verify Health & Telemetry
- Validate `/actuator/health` returns `{"status":"UP"}` across target groups.
- Inspect `/actuator/prometheus` scraping metrics in AWS CloudWatch Container Insights or Amazon Managed Prometheus (AMP).
