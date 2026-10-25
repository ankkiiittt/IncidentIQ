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
