# syntax=docker/dockerfile:1
# Multi-stage Docker build for IncidentIQ Backend (Java 21 / Spring Boot 3.3.5)

# -------------------------------------------------------
# Stage 1: Build Application
# -------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code and build jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# -------------------------------------------------------
# Stage 2: Minimal Production Runtime
# -------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

LABEL maintainer="IncidentIQ Engineering Team"
LABEL description="IncidentIQ - AI-Powered Enterprise Incident Intelligence Platform Backend"

# Install curl for container healthcheck
RUN apk add --no-cache curl tzdata

# Create dedicated non-root user and group
RUN addgroup -g 10001 -S incidentiq && \
    adduser -u 10001 -S incidentiq -G incidentiq

WORKDIR /app

# Copy packaged jar from builder stage
COPY --from=builder --chown=incidentiq:incidentiq /build/target/incidentiq-backend-*.jar /app/app.jar

# Switch to non-root user
USER incidentiq:incidentiq

EXPOSE 8080

# Configure container healthcheck via Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Optimized JVM execution parameters for containerized virtual threads
ENV JAVA_OPTS="-XX:+UseG1GC -XX:+EnableDynamicAgentLoading -XX:InitialRAMPercentage=40.0 -XX:MaxRAMPercentage=80.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
