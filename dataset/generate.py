#!/usr/bin/env python3
"""
IncidentIQ Synthetic Dataset Generator
Generates realistic seed incidents, service health metrics, and knowledge documents.
"""

import json
import random
import uuid
from datetime import datetime, timedelta

SERVICES = [
    {"name": "checkout-api", "criticality": "CRITICAL", "env": "PROD"},
    {"name": "payment-gateway", "criticality": "CRITICAL", "env": "PROD"},
    {"name": "auth-service", "criticality": "HIGH", "env": "PROD"},
    {"name": "inventory-api", "criticality": "MEDIUM", "env": "PROD"},
    {"name": "notification-service", "criticality": "LOW", "env": "PROD"}
]

CATEGORIES = ["APPLICATION", "DATABASE", "INFRASTRUCTURE", "NETWORK", "SECURITY", "THIRD_PARTY"]
PRIORITIES = ["P1", "P2", "P3", "P4"]
SEVERITIES = ["CRITICAL", "HIGH", "MEDIUM", "LOW"]

INCIDENT_TEMPLATES = [
    {
        "title": "Database connection pool exhaustion on {service}",
        "category": "DATABASE",
        "severity": "HIGH",
        "priority": "P2",
        "description": "High connection latency detected on {service} DB instances. Pool utilization reached 100%.",
        "root_cause": "Long-running report queries holding table exclusive lock preventing worker transactions.",
        "resolution": "Terminated blocking queries with pg_terminate_backend and increased connection pool size."
    },
    {
        "title": "TLS handshake failure connecting to external provider in {service}",
        "category": "THIRD_PARTY",
        "severity": "CRITICAL",
        "priority": "P1",
        "description": "Downstream HTTP 502/504 errors on {service} during payment authorization.",
        "root_cause": "Upstream partner rotated intermediate CA certificate without prior notice.",
        "resolution": "Imported new partner certificate into Java truststore and performed rolling restart."
    },
    {
        "title": "High CPU utilization and OOM kill risk on {service}",
        "category": "INFRASTRUCTURE",
        "severity": "HIGH",
        "priority": "P2",
        "description": "Container memory exceeding 92% of limit on multiple {service} worker pods.",
        "root_cause": "Unbounded caching of raw request payloads in heap memory under peak load.",
        "resolution": "Applied patch limiting payload cache window to 60 seconds and deployed hotfix."
    },
    {
        "title": "Elevated latency on {service} cache cluster",
        "category": "APPLICATION",
        "severity": "MEDIUM",
        "priority": "P3",
        "description": "p99 latency elevated to 450ms across {service} endpoints.",
        "root_cause": "Cache key hot-spotting on global configuration keys.",
        "resolution": "Added local near-cache for static configuration and flushed stale Redis keys."
    }
]

def generate_incidents(count=20):
    incidents = []
    base_time = datetime.now() - timedelta(days=30)
    
    for i in range(count):
        service = random.choice(SERVICES)
        template = random.choice(INCIDENT_TEMPLATES)
        
        created_at = base_time + timedelta(hours=i * 36 + random.randint(1, 10))
        resolved_at = created_at + timedelta(minutes=random.randint(25, 360))
        
        inc = {
            "id": str(uuid.uuid4()),
            "incidentNumber": i + 1,
            "title": template["title"].format(service=service["name"]),
            "description": template["description"].format(service=service["name"]),
            "category": template["category"],
            "severity": template["severity"],
            "priority": template["priority"],
            "serviceName": service["name"],
            "status": "RESOLVED",
            "rootCause": template["root_cause"],
            "resolution": template["resolution"],
            "createdAt": created_at.isoformat() + "Z",
            "resolvedAt": resolved_at.isoformat() + "Z"
        }
        incidents.append(inc)
        
    return incidents

if __name__ == "__main__":
    dataset = {
        "generatedAt": datetime.now().isoformat() + "Z",
        "services": SERVICES,
        "incidents": generate_incidents(25)
    }
    
    with open("dataset/synthetic_incidents.json", "w", encoding="utf-8") as f:
        json.dump(dataset, f, indent=2)
        
    print(f"Generated {len(dataset['incidents'])} synthetic incidents into dataset/synthetic_incidents.json")
