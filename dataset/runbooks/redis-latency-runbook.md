# Redis Cache Latency & OOM Eviction Runbook

## Overview
Diagnostic steps for distributed caching bottlenecks, memory pressure, and command latency spikes.

## Symptoms
- High response times on cache lookups (>50ms).
- Redis logs showing `OOM command not allowed when used memory > 'maxmemory'`.
- Spike in cache miss rates and cascading load hitting PostgreSQL database.

## Diagnostics
1. Connect to Redis CLI and check memory fragmentation:
   ```bash
   redis-cli info memory
   ```
2. Check slow log for O(N) operations like `KEYS *` or huge `HGETALL`:
   ```bash
   redis-cli slowlog get 10
   ```

## Remediation
1. Flush volatile expired keys:
   ```bash
   redis-cli --scan --pattern "ratelimit:*" | xargs redis-cli del
   ```
2. Adjust maxmemory policy to `volatile-lru` or `allkeys-lru`.
3. Increase Redis cluster node memory allocation.
