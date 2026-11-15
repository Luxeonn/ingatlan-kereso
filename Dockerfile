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

# Stage 2: Minimal hardened runtime with Temurin JRE 25 + Python & Playwright Chromium sidecars
FROM eclipse-temurin:25-jre-alpine

# Upgrade packages to apply latest security patches and install Python + Node.js + Chromium runtimes
COPY tools/requirements.txt /tmp/requirements.txt
RUN apk upgrade --no-cache && \
    apk add --no-cache curl ca-certificates python3 py3-pip nodejs chromium && \
    pip3 install --no-cache-dir --break-system-packages -r /tmp/requirements.txt && \
    rm -f /tmp/requirements.txt

# Create non-root system user (UID 10001) for container security
RUN addgroup -S -g 10001 appuser && \
    adduser -S -u 10001 -G appuser -h /app -s /sbin/nologin appuser

WORKDIR /app

# Install playwright-core for the Node.js Chromium fallback sidecar without keeping npm in final image
COPY --chown=appuser:appuser tools/package.json tools/package-lock.json /app/tools/
RUN apk add --no-cache --virtual .npm-deps npm && \
    cd /app/tools && \
    npm ci --omit=dev --no-audit --no-fund && \
    rm -rf /root/.npm && \
    apk del .npm-deps

# Prepare directories with proper permissions
RUN mkdir -p /app/data /app/logs /app/tools && \
    chown -R appuser:appuser /app

# Copy executable Spring Boot jar and sidecar scripts
COPY --from=builder --chown=appuser:appuser /build/build/libs/ingatlan-*.jar /app/app.jar
COPY --chown=appuser:appuser tools/ /app/tools/

USER appuser:appuser

# Default container configurations
ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    NO_BROWSER=true \
    PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium-browser \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Keep logs available as a mount point; Compose assigns the named backend_logs volume.
VOLUME ["/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -f http://localhost:8080/api/status || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
