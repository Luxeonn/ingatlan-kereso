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

# Stage 2: Minimal hardened Alpine runtime with Custom JRE 25 + Python curl_cffi sidecar
FROM alpine:3.24

ENV JAVA_HOME=/opt/jre \
    PATH="/opt/jre/bin:${PATH}" \
    PYTHONDONTWRITEBYTECODE=1 \
    SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    NO_BROWSER=true \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Copy minimal custom JRE built in Stage 1
COPY --from=builder /opt/jre /opt/jre

# Apply security patches, install Python sidecar deps, remove pip and unused Python stdlib/caches in one layer
COPY tools/requirements.txt /tmp/requirements.txt
RUN set -eu; \
    apk upgrade --no-cache; \
    apk add --no-cache ca-certificates tzdata python3; \
    apk add --no-cache --virtual .pip-deps py3-pip; \
    pip3 install --no-cache-dir --break-system-packages -r /tmp/requirements.txt; \
    apk del --no-network .pip-deps; \
    rm -rf /tmp/requirements.txt /root/.cache \
      /usr/lib/python3.*/test /usr/lib/python3.*/tests \
      /usr/lib/python3.*/idlelib /usr/lib/python3.*/tkinter \
      /usr/lib/python3.*/turtledemo /usr/lib/python3.*/ensurepip \
      /usr/lib/python3.*/pydoc_data /usr/lib/python3.*/lib2to3; \
    find /usr/lib/python3.* -type d \( -name '__pycache__' -o -name 'tests' -o -name 'test' \) -exec rm -rf {} +; \
    find /usr/lib/python3.* -type f \( -name '*.pyc' -o -name '*.pyo' \) -delete

# Create non-root system user (UID 10001) and app directories before COPY to avoid OverlayFS chown duplication
RUN addgroup -S -g 10001 appuser && \
    adduser -S -u 10001 -G appuser -h /app -s /sbin/nologin appuser && \
    mkdir -p /app/data /app/logs /app/tools/scrapers && \
    chown -R appuser:appuser /app

WORKDIR /app

# Copy executable Spring Boot jar and only runtime Python sidecar files
COPY --from=builder --chown=appuser:appuser /build/build/libs/ingatlan-*.jar /app/app.jar
COPY --chown=appuser:appuser tools/sidecar.py tools/models.py /app/tools/
COPY --chown=appuser:appuser tools/scrapers/ /app/tools/scrapers/

USER appuser:appuser

# Keep logs available as a mount point; Compose assigns the named backend_logs volume.
VOLUME ["/app/logs"]

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=20s --retries=3 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/api/status || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
