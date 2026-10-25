# Payment Gateway TLS Handshake & Certificate Rotation Runbook

## Overview
Procedures for addressing downstream TLS certificate expirations, handshake failures, and payment provider network timeouts.

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
