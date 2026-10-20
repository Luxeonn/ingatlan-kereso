# Stage 1: Build Headless Spring Boot JAR (Java 25 LTS)
FROM eclipse-temurin:25-jdk-alpine AS builder

ARG APP_VERSION=1.0.0

WORKDIR /build

# Gradle wrapper and build caching
COPY gradlew gradlew
COPY gradle/ gradle/
COPY settings.gradle.kts build.gradle.kts ./
COPY config/ config/
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Copy backend source files
COPY src ./src

# Build headless Spring Boot fat JAR without running tests
RUN sed -i 's/\r$//' gradlew && ./gradlew -PreleaseVersion="$APP_VERSION" bootJar --no-daemon -x test

# Stage 2: Minimal hardened runtime with Temurin JRE 25
FROM eclipse-temurin:25-jre-alpine

# Upgrade packages to apply latest security patches (e.g. libexpat) and install curl/ca-certificates
RUN apk upgrade --no-cache && \
    apk add --no-cache curl ca-certificates

# Create non-root system user (UID 10001) for container security
RUN addgroup -S -g 10001 appuser && \
    adduser -S -u 10001 -G appuser -h /app -s /sbin/nologin appuser

WORKDIR /app

# Prepare directories with proper permissions
RUN mkdir -p /app/data /app/logs && \
    chown -R appuser:appuser /app

# Copy executable Spring Boot jar from builder stage
COPY --from=builder --chown=appuser:appuser /build/build/libs/ingatlan-*.jar /app/app.jar

USER appuser:appuser

# Default container configurations
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    NO_BROWSER=true \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Keep logs available as a mount point; Compose assigns the named backend_logs volume.
VOLUME ["/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/api/status || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
