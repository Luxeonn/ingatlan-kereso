#!/usr/bin/env sh
set -e

echo "=============================================================================="
echo "                 INGATLAN KERESO - DOCKER LAUNCHER (Linux/macOS)"
echo "=============================================================================="
echo ""

if ! command -v docker >/dev/null 2>&1; then
    echo "[HIBA] A Docker nincs telepítve vagy nem található a PATH-ban!"
    exit 1
fi

echo "[1/4] Legfrissebb GHCR Docker image ellenőrzése és frissítése..."
docker pull ghcr.io/luxeonn/ingatlan-kereso:latest

echo ""
echo "[2/4] Korábbi 'ingatlan-kereso' konténer ellenőrzése és eltávolítása..."
docker rm -f ingatlan-kereso >/dev/null 2>&1 || true

echo ""
echo "[3/4] Konténer indítása (Név: ingatlan-kereso, Port: 8080:8080)..."
docker run -d \
  --name ingatlan-kereso \
  -p 8080:8080 \
  -v ingatlan_data:/app/data \
  -v ingatlan_logs:/app/logs \
  --restart unless-stopped \
  ghcr.io/luxeonn/ingatlan-kereso:latest

echo ""
echo "[4/4] Siker! A konténer 'ingatlan-kereso' néven azonnal elérhető."
echo "Web Dashboard: http://localhost:8080"
echo ""

if command -v xdg-open >/dev/null 2>&1; then
    xdg-open "http://localhost:8080" >/dev/null 2>&1 || true
elif command -v open >/dev/null 2>&1; then
    open "http://localhost:8080" >/dev/null 2>&1 || true
fi
