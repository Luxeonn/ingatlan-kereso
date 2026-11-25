# Stage 1: Build Headless Spring Boot JAR (Java 25 LTS) & Minimal Custom JRE via jlink
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

# Analyze exact JDK module dependencies from the Spring Boot fat JAR and assemble a stripped custom JRE
RUN set -eu; \
    mkdir -p /tmp/jar-extract; \
    cd /tmp/jar-extract; \
    jar -xf /build/build/libs/ingatlan-*.jar; \
    JDEPS_MODULES=$(jdeps \
      --ignore-missing-deps \
      -q \
      --recursive \
      --multi-release 25 \
      --print-module-deps \
      --class-path '/tmp/jar-extract/BOOT-INF/lib/*' \
      /tmp/jar-extract/BOOT-INF/classes); \
    echo "Detected jdeps modules: ${JDEPS_MODULES}"; \
    jlink \
      --add-modules "${JDEPS_MODULES},java.security.sasl,jdk.crypto.ec,jdk.charsets,jdk.naming.dns,jdk.unsupported" \
      --strip-debug \
      --no-man-pages \
      --no-header-files \
      --compress=zip-6 \
      --output /opt/jre; \
    rm -rf /tmp/jar-extract

# Stage 2: Minimal hardened Alpine runtime with Custom JRE 25 + Node.js Playwright Chromium sidecar
FROM alpine:3.24

ENV JAVA_HOME=/opt/jre \
    PATH="/opt/jre/bin:${PATH}" \
    SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=/usr/bin/chromium-browser \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Copy minimal custom JRE built in Stage 1
COPY --from=builder /opt/jre /opt/jre

# Apply security patches and install only runtime OS dependencies (Node.js + Chromium for Ingatlan.com Playwright sidecar)
RUN set -eu; \
    apk upgrade --no-cache; \
    apk add --no-cache ca-certificates tzdata nodejs chromium

# Create non-root system user (UID 10001) and app directories before COPY
RUN addgroup -S -g 10001 appuser && \
    adduser -S -u 10001 -G appuser -h /app -s /sbin/nologin appuser && \
    mkdir -p /app/data /app/logs /app/tools && \
    chown -R appuser:appuser /app

WORKDIR /app

# Install playwright-core without keeping npm or duplicating OverlayFS layers on chown
COPY --chown=appuser:appuser tools/package.json tools/package-lock.json /app/tools/
RUN set -eu; \
    apk add --no-cache --virtual .npm-deps npm; \
    cd /app/tools; \
    npm ci --omit=dev --no-audit --no-fund; \
    chown -R appuser:appuser /app/tools/node_modules; \
    rm -rf /root/.npm /tmp/npm-*; \
    apk del --no-network .npm-deps

# Copy executable Spring Boot jar and only the runtime Playwright sidecar script
COPY --from=builder --chown=appuser:appuser /build/build/libs/ingatlan-*.jar /app/app.jar
COPY --chown=appuser:appuser tools/sidecar-playwright.mjs /app/tools/

USER appuser:appuser

# Keep logs available as a mount point; Compose assigns the named backend_logs volume.
VOLUME ["/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/status || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
