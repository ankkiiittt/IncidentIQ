# Database Connection Pool Exhaustion & Failover Runbook

## Overview
This runbook guides engineers through diagnosing and mitigating connection pool exhaustion, blocked lock chains, and database failover scenarios on high-throughput microservices.

## Symptoms
- API latency spikes exceeding 2500ms p99.
- HikariCP pool errors: `Connection is not available, request timed out after 30000ms`.
- HTTP 500 error responses on checkout and order placement endpoints.

## Immediate Diagnostic Steps
1. Inspect active connection count in PostgreSQL:
   ```sql
   SELECT count(*), state FROM pg_stat_activity GROUP BY state;
   ```
2. Identify long-running transactions holding table locks:
   ```sql
   SELECT pid, now() - query_start AS duration, query
   FROM pg_stat_activity
   WHERE state != 'idle' AND (now() - query_start) > interval '5 seconds';
   ```
3. Check database CPU and memory utilization on RDS/primary node.

## Remediation Actions
1. **Terminate Blocked Sessions**:
   Terminate long-running queries holding exclusive row/table locks:
   ```sql
   SELECT pg_terminate_backend(pid);
   ```
2. **Promote Read-Replica if Primary is Unresponsive**:
   If primary DB node CPU is pinned at 100%, trigger manual failover to hot standby replica.
3. **Scale Hikari Pool Size**:
   If database capacity allows, update `hikari.maximum-pool-size` from 15 to 25.
