# Stage 1: Build Frontend SPA (React 19, TypeScript, Vite)
FROM node:20-alpine AS frontend-builder

WORKDIR /frontend

COPY frontend/package*.json ./
RUN npm ci

COPY frontend ./
RUN npm run build

# Stage 2: Build GraalVM Native Image
FROM ghcr.io/graalvm/native-image-community:21 AS builder

WORKDIR /build

# Gradle wrapper and build caching
COPY gradlew gradlew
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts checkstyle.xml spotbugs-exclude.xml ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Source copy & include static SPA assets from frontend-builder
COPY src ./src
COPY --from=frontend-builder /frontend/dist ./src/main/resources/static

# Compile GraalVM Native Image
RUN ./gradlew nativeCompile --no-daemon -x test

# Stage 3: Minimal hardened runtime for Native Binary
FROM debian:bookworm-slim

# Install curl for HEALTHCHECK and clean package lists
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Create non-root system user (UID 10001) for container security
RUN groupadd -g 10001 appuser && \
    useradd -u 10001 -g appuser -s /bin/sh -d /app appuser

WORKDIR /app

# Prepare directories with proper permissions
RUN mkdir -p /app/data /app/logs && \
    chown -R appuser:appuser /app

# Copy GraalVM native executable from builder stage
COPY --from=builder --chown=appuser:appuser /build/build/native/nativeCompile/ingatlan-kereso /app/ingatlan-kereso

USER appuser:appuser

# Default container configurations
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    NO_BROWSER=true

# Declarative volumes for zero-config persistence
VOLUME ["/app/data", "/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || curl -f http://localhost:8080/api/status || exit 1

ENTRYPOINT ["/app/ingatlan-kereso"]
