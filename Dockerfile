# Stage 1: Build Headless Spring Boot JAR (Java 21 LTS)
FROM eclipse-temurin:21-jdk-jammy AS builder

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
RUN sed -i 's/\r$//' gradlew && ./gradlew bootJar --no-daemon -x test

# Stage 2: Minimal hardened runtime with Temurin JRE 21
FROM eclipse-temurin:21-jre-jammy

# Install curl for HEALTHCHECK and ca-certificates
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

# Copy executable Spring Boot jar from builder stage
COPY --from=builder --chown=appuser:appuser /build/build/libs/ingatlan-1.0.jar /app/app.jar

USER appuser:appuser

# Default container configurations
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    NO_BROWSER=true \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Declarative volumes for zero-config persistence
VOLUME ["/app/data", "/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
