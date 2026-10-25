package com.incidentiq.knowledge.service;

import com.incidentiq.catalog.entity.ServiceEntity;
import com.incidentiq.catalog.repository.ServiceRepository;
import com.incidentiq.knowledge.dto.CreateDocumentRequest;
import com.incidentiq.knowledge.entity.AccessLevel;
import com.incidentiq.knowledge.entity.DocumentSourceType;
import com.incidentiq.knowledge.repository.KnowledgeDocumentRepository;
import com.incidentiq.user.entity.User;
import com.incidentiq.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KnowledgeBootstrapService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBootstrapService.class);

    private final KnowledgeService knowledgeService;
    private final KnowledgeDocumentRepository documentRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;

    public KnowledgeBootstrapService(KnowledgeService knowledgeService,
                                     KnowledgeDocumentRepository documentRepository,
                                     ServiceRepository serviceRepository,
                                     UserRepository userRepository) {
        this.knowledgeService = knowledgeService;
        this.documentRepository = documentRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (documentRepository.count() > 0) {
            log.info("Knowledge base already contains {} documents. Skipping initial seed.", documentRepository.count());
            return;
        }

        log.info("Knowledge base is empty. Bootstrapping operational runbooks and knowledge seed...");

        User admin = userRepository.findAll().stream().findFirst().orElse(null);
        List<ServiceEntity> services = serviceRepository.findAll();
        ServiceEntity checkoutSvc = services.stream()
                .filter(s -> s.getName().toLowerCase().contains("checkout"))
                .findFirst()
                .orElse(services.isEmpty() ? null : services.get(0));

        // 1. Database Connection Pool Exhaustion Runbook
        ingestSeedDocument(new CreateDocumentRequest(
                "Database Connection Pool Exhaustion & Failover Runbook",
                DocumentSourceType.RUNBOOK,
                "https://wiki.incidentiq.internal/runbooks/db-pool-exhaustion",
                checkoutSvc != null ? checkoutSvc.getId() : null,
                AccessLevel.PUBLIC,
                """
                # Database Connection Pool Exhaustion & Failover Runbook

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
                """
        ), admin);

        // 2. Payment Gateway TLS Handshake & Certificate Rotation Runbook
        ingestSeedDocument(new CreateDocumentRequest(
                "Payment Gateway TLS Handshake & Certificate Rotation Runbook",
                DocumentSourceType.RUNBOOK,
                "https://wiki.incidentiq.internal/runbooks/payment-tls",
                checkoutSvc != null ? checkoutSvc.getId() : null,
                AccessLevel.PUBLIC,
                """
                # Payment Gateway TLS Handshake & Certificate Rotation Runbook

                ## Symptoms
                - Payment processing worker threads failing with `SSLHandshakeException`.
                - Error messages: `PKIX path building failed: unable to find valid certification path to requested target`.
                - Orders stuck in PENDING state; downstream credit card authorizations failing with 502 Bad Gateway.

                ## Root Cause Patterns
                - Downstream payment partner rotated TLS certificates without advance notification.
                - Java truststore `cacerts` lacks updated root or intermediate CA certificates.

                ## Remediation Steps
                1. Test connectivity and fetch external certificate chain:
                   ```bash
                   openssl s_client -connect api.payment-partner.internal:443 -showcerts
                   ```
                2. Import missing intermediate certificate into Java truststore:
                   ```bash
                   keytool -importcert -alias payment-ca -file partner_ca.crt -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit -noprompt
                   ```
                3. Rolling restart of payment processing application pods.
                4. Verify authorization health check returns HTTP 200.
                """
        ), admin);

        // 3. Redis Cache Latency & Eviction Runbook (Engineering Access Level)
        ingestSeedDocument(new CreateDocumentRequest(
                "Redis Cache Latency & OOM Eviction Runbook",
                DocumentSourceType.RUNBOOK,
                "https://wiki.incidentiq.internal/runbooks/redis-latency",
                checkoutSvc != null ? checkoutSvc.getId() : null,
                AccessLevel.ENGINEERING,
                """
                # Redis Cache Latency & OOM Eviction Runbook

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
                """
        ), admin);

        // 4. Postmortem: Global Checkout Failure Due to TLS Expiration
        ingestSeedDocument(new CreateDocumentRequest(
                "Postmortem: Global Checkout Failure Due to TLS Expiration",
                DocumentSourceType.POSTMORTEM,
                "https://wiki.incidentiq.internal/postmortems/2026-10-checkout-tls",
                checkoutSvc != null ? checkoutSvc.getId() : null,
                AccessLevel.PUBLIC,
                """
                # Postmortem: Global Checkout Failure Due to TLS Expiration

                ## Incident Summary
                On 2026-10-15, checkout traffic experienced 100% failure rate for 42 minutes when processing payment transactions.
                Root cause was identified as an expired root certificate on downstream payment partner's cluster.

                ## Timeline
                - 14:00 UTC: Partner rotated TLS certificate to new intermediate CA.
                - 14:02 UTC: Application workers started throwing `SSLHandshakeException`.
                - 14:15 UTC: P1 Incident declared; incident commanders engaged.
                - 14:32 UTC: New CA certificate imported into Kubernetes secret and mounted into workers.
                - 14:44 UTC: Full traffic restored; p99 latency returned to baseline 120ms.

                ## Preventive Actions
                - Automated daily TLS certificate expiration monitoring for all external endpoints.
                - Automated truststore sync job deployed via CI/CD.
                """
        ), admin);

        // 5. FAQ: Production Incident Escalation and SLA Policies
        ingestSeedDocument(new CreateDocumentRequest(
                "FAQ: Production Incident Escalation and SLA Policies",
                DocumentSourceType.FAQ,
                "https://wiki.incidentiq.internal/faq/incident-sla",
                null,
                AccessLevel.PUBLIC,
                """
                # FAQ: Production Incident Escalation and SLA Policies

                ## What are the response and resolution targets?
                - **P1 (Critical)**: Response target 15 minutes, Resolution target 4 hours. 24x7 page on-call.
                - **P2 (High)**: Response target 30 minutes, Resolution target 8 hours.
                - **P3 (Medium)**: Response target 2 hours, Resolution target 24 hours.
                - **P4 (Low)**: Response target 8 hours, Resolution target 72 hours.

                ## Who can resolve incidents?
                Only users with the role `ADMIN` or `ENGINEER` can resolve or close an incident.
                Resolving requires entering both Root Cause and Resolution descriptions.
                """
        ), admin);

        log.info("Knowledge base bootstrap completed successfully. Total documents: {}", documentRepository.count());
    }

    private void ingestSeedDocument(CreateDocumentRequest request, User uploader) {
        try {
            knowledgeService.createAndIngestDocument(request, uploader);
            log.info("Ingested seed document: {}", request.title());
        } catch (Exception e) {
            log.warn("Could not ingest seed document '{}': {}", request.title(), e.getMessage());
        }
    }
}
