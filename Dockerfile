# Stage 1: Build Frontend SPA (React 19, TypeScript, Vite)
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

# Stage 2: Build & Package Java Uber-JAR (Multi-stage)
FROM maven:3.9.9-eclipse-temurin-21-jammy AS builder

WORKDIR /build

# 1. Dependency caching
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Source copy & include static SPA assets from frontend-builder
COPY src ./src
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

RUN mvn clean package -DskipTests

# Stage 3: Minimal hardened runtime
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
    JAVA_TOOL_OPTIONS="-Djava.awt.headless=true -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Declarative volumes for zero-config persistence
VOLUME ["/app/data", "/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/api/status || exit 1

ENTRYPOINT ["java", "-jar", "ingatlan-1.0.jar"]
