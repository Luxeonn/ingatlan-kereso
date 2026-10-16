#!/usr/bin/env sh
set -e

echo "=============================================================================="
echo "          INGATLAN KERESO - DOCKER COMPOSE LAUNCHER (Linux/macOS)"
echo "=============================================================================="
echo ""

if ! command -v docker >/dev/null 2>&1; then
    echo "[HIBA] A Docker nincs telepítve vagy nem található a PATH-ban!"
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "[HIBA] A Docker Daemon nem fut! Kérlek indítsd el a Docker démont."
    exit 1
fi

echo "[1/4] Korábbi monolitikus 'ingatlan-kereso' konténer eltávolítása (ha létezik)..."
docker rm -f ingatlan-kereso >/dev/null 2>&1 || true

echo ""
echo "[2/4] Docker Compose szolgáltatások előkészítése és indítása..."
if docker compose pull 2>/dev/null; then
    docker compose up -d
else
    echo "[INFO] GHCR image-ek letöltése sikertelen vagy lokális build szükséges. Helyi építés..."
    docker compose up -d --build
fi

echo ""
echo "[3/4] Várakozás a rendszer (PostgreSQL -> Backend -> Nginx Frontend) feléledésére..."
READY=0
for i in $(seq 1 30); do
    if curl -s -f http://localhost:8080/ >/dev/null 2>&1; then
        READY=1
        echo "✅ A rendszer elérhető ($i. másodpercben)."
        break
    fi
    sleep 1
done

if [ $READY -ne 1 ]; then
    echo "[FIGYELEM] Az indulás a vártnál több időt vesz igénybe."
fi

echo ""
echo "[4/4] SIKER! A rendszer üzemkész."
echo "=============================================================================="
echo "Web Dashboard:  http://localhost:8080"
echo "REST API:       http://localhost:8080/api/status"
echo "=============================================================================="
echo ""

if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "http://localhost:8080" >/dev/null 2>&1 || true
elif command -v open >/dev/null 2>&1; then
    open "http://localhost:8080" >/dev/null 2>&1 || true
fi
