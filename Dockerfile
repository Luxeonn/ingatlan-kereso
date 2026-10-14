# Stage 1: Build & Package (Multi-stage)
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

WORKDIR /build

# 1. Dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Source copy & build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Minimal hardened runtime
FROM eclipse-temurin:21-jre-jammy

# Install curl for HEALTHCHECK and clean package lists
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root system user (UID 10001) for container security
RUN groupadd -g 10001 appuser && \
    useradd -u 10001 -g appuser -s /bin/sh -d /app appuser

WORKDIR /app

# Prepare directories with proper permissions
RUN mkdir -p /app/data /app/logs && \
    chown -R appuser:appuser /app

# Copy executable uber-jar from builder stage
COPY --from=builder --chown=appuser:appuser /build/target/ingatlan-1.0.jar /app/ingatlan-1.0.jar

USER appuser:appuser

# Default container configurations
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    NO_BROWSER=true \
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/api/status || exit 1

ENTRYPOINT ["java", "-jar", "ingatlan-1.0.jar"]
