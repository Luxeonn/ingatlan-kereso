#!/usr/bin/env bash
# ==============================================================================
# scripts/smoke-test-docker-compose.sh
# Deterministic Multi-Container Smoke Test for Docker Compose 3-Tier Stack
#
# Components verified:
#   1. db: PostgreSQL 17 Alpine (service health, connectivity, schema)
#   2. backend: Spring Boot 4.1.1 on Temurin JRE 25 (REST API, /api/status UP)
#   3. frontend: Nginx 1.27 Alpine (SPA serving, /api/ reverse proxy)
#   4. Database Persistence: Read/write roundtrip via /api/config and /api/status
#
# Behavior:
#   - Fails closed on any error, timeout, or unhealthy container
#   - Sets up cleanup trap (EXIT) that tears down stack and volumes
#   - Dumps container logs on failure prior to cleanup
#   - Exits 0 on complete pass, 1 on any failure
# ==============================================================================

set -eo pipefail

# Determine repository root
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

PORT="${HOST_PORT:-8080}"
TIMEOUT="${SMOKE_TIMEOUT:-120}"
BASE_URL="http://127.0.0.1:${PORT}"

echo "=============================================================================="
echo "      INGATLAN KERESO — DOCKER COMPOSE MULTI-CONTAINER SMOKE TEST             "
echo "=============================================================================="
echo "Working directory: $REPO_ROOT"
echo "Target Base URL:   $BASE_URL"
echo "Health Timeout:    ${TIMEOUT}s"
echo ""

# 1. Verify Docker and Docker Compose availability
echo "--- [1/4] Checking Docker and Docker Compose Availability ---"
if ! command -v docker >/dev/null 2>&1; then
    echo "::error::Docker is not installed or not in PATH!"
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "::error::Docker daemon is not running!"
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    echo "::error::Docker Compose (v2) is not available!"
    exit 1
fi

echo "✅ Docker CLI and Docker Compose are available."
docker --version
docker compose version
echo ""

# 2. Cleanup Trap Handler
TEST_SUCCESS=0
cleanup() {
    local exit_code=$?
    if [ "$TEST_SUCCESS" -ne 1 ] || [ "$exit_code" -ne 0 ]; then
        echo ""
        echo "=== ⚠️ SMOKE TEST FAILED (Exit code: $exit_code) ==="
        echo "=== 📋 Dumping Docker Compose container logs ==="
        docker compose logs --tail 100 || true
        echo ""
        echo "=== 📋 Docker Compose container status ==="
        docker compose ps -a || true
    fi
    echo ""
    echo "=== 🧹 Cleaning up Docker Compose stack and volumes ==="
    docker compose down -v --remove-orphans || true
}
trap cleanup EXIT

# Clean up any leftover containers from previous runs
echo "--- [2/4] Ensuring Clean Initial Environment ---"
docker compose down -v --remove-orphans 2>/dev/null || true
echo "✅ Environment cleaned."
echo ""

# 3. Build and Spawn Multi-Container Stack
echo "--- [3/4] Spawning Docker Compose Stack (up -d --build) ---"
docker compose up -d --build
echo "✅ Docker Compose stack launched in background."
echo ""

# 4. Poll Container Status and Health
echo "--- [4/4] Polling Container Status and Health (Timeout: ${TIMEOUT}s) ---"
START_TIME=$(date +%s)
DB_HEALTHY=0
BACKEND_HEALTHY=0
FRONTEND_HEALTHY=0

while true; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))

    # A. Check PostgreSQL DB health
    if [ "$DB_HEALTHY" -ne 1 ]; then
        if docker compose ps db | grep -qi "healthy"; then
            DB_HEALTHY=1
            echo "  ✅ [${ELAPSED}s] db (PostgreSQL 17 Alpine): Container reached healthy state."
        fi
    fi

    # B. Check Spring Boot Backend health via /api/status through reverse proxy or direct
    if [ "$BACKEND_HEALTHY" -ne 1 ]; then
        BE_RESP=$(curl -s -f -m 3 "${BASE_URL}/api/status" 2>/dev/null || true)
        if echo "$BE_RESP" | grep -q '"status":"UP"'; then
            BACKEND_HEALTHY=1
            echo "  ✅ [${ELAPSED}s] backend (Spring Boot 4.1.1): /api/status returned status=UP."
        fi
    fi

    # C. Check Frontend Nginx SPA serving via /
    if [ "$FRONTEND_HEALTHY" -ne 1 ]; then
        FE_RESP=$(curl -s -f -m 3 "${BASE_URL}/" 2>/dev/null || true)
        if echo "$FE_RESP" | grep -qi "<html"; then
            FRONTEND_HEALTHY=1
            echo "  ✅ [${ELAPSED}s] frontend (Nginx 1.27 Alpine): Serving SPA root index.html."
        fi
    fi

    if [ "$DB_HEALTHY" -eq 1 ] && [ "$BACKEND_HEALTHY" -eq 1 ] && [ "$FRONTEND_HEALTHY" -eq 1 ]; then
        echo ""
        echo "🎉 All containers (db, backend, frontend) verified healthy after ${ELAPSED}s!"
        break
    fi

    if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
        echo ""
        echo "::error::Multi-container health check timed out after ${TIMEOUT} seconds!"
        echo "Health summary: DB=$DB_HEALTHY, Backend=$BACKEND_HEALTHY, Frontend=$FRONTEND_HEALTHY"
        exit 1
    fi

    sleep 2
done

# 5. Database Read/Write Persistence Verification
echo ""
echo "=== 💾 Verifying Database Read/Write Persistence ==="

# Step 1: Verify /api/status contains database tracking metrics
STATUS_PAYLOAD=$(curl -s -f "${BASE_URL}/api/status")
echo "  GET /api/status: $STATUS_PAYLOAD"
if ! echo "$STATUS_PAYLOAD" | grep -q '"totalInDb"'; then
    echo "::error::/api/status missing totalInDb metric!"
    exit 1
fi
echo "  ✅ Database status metric contract verified."

# Step 2: Write search criteria configuration to PostgreSQL
TEST_CONFIG='{"city":"Debrecen","minPriceMillion":42,"maxPriceMillion":88,"enabledPortals":["dh","oc"]}'
WRITE_RESP=$(curl -s -f -X POST "${BASE_URL}/api/config" \
    -H "Content-Type: application/json" \
    -d "$TEST_CONFIG")
echo "  POST /api/config response: $WRITE_RESP"
if ! echo "$WRITE_RESP" | grep -q '"success":true'; then
    echo "::error::POST /api/config failed to persist configuration to PostgreSQL!"
    exit 1
fi
echo "  ✅ Search criteria persisted to PostgreSQL via POST /api/config."

# Step 3: Read back persisted configuration from PostgreSQL
READ_CONFIG=$(curl -s -f "${BASE_URL}/api/config")
echo "  GET /api/config response: $READ_CONFIG"
if ! echo "$READ_CONFIG" | grep -q '"minPriceMillion":42'; then
    echo "::error::GET /api/config did not return persisted values from PostgreSQL!"
    exit 1
fi
echo "  ✅ Database read/write persistence roundtrip verified."

# Step 4: Verify core REST API contracts
PORTALS_RESP=$(curl -s -f "${BASE_URL}/api/portals")
if ! echo "$PORTALS_RESP" | grep -q "id"; then
    echo "::error::GET /api/portals did not return portal descriptors!"
    exit 1
fi
echo "  ✅ Portal descriptors endpoint /api/portals verified."

CITIES_RESP=$(curl -s -f "${BASE_URL}/api/cities")
if ! echo "$CITIES_RESP" | grep -q "Budapest"; then
    echo "::error::GET /api/cities did not return predefined cities!"
    exit 1
fi
echo "  ✅ Cities endpoint /api/cities verified."

# Step 5: Mark test success so cleanup does not dump error logs
TEST_SUCCESS=1
echo ""
echo "=============================================================================="
echo "  ✅ DOCKER COMPOSE MULTI-CONTAINER SMOKE TEST COMPLETED SUCCESSFULLY!        "
echo "=============================================================================="
exit 0
