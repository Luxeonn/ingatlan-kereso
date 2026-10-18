#!/usr/bin/env bash
set -eo pipefail

JAR_PATH="${1:-build/libs/ingatlan-1.0.0.jar}"
PORT="${SERVER_PORT:-8099}"
HOST="${SERVER_HOST:-127.0.0.1}"

echo "=== 🚀 Headless REST API Runtime Smoke Teszt Indítása (Spring Boot 4.1.0) ==="

if [ ! -f "$JAR_PATH" ]; then
  echo "::error::A buildelt JAR fájl nem található: $JAR_PATH"
  exit 1
fi

SERVER_PORT="$PORT" SERVER_HOST="$HOST" NO_BROWSER=true java -jar "$JAR_PATH" &
APP_PID=$!
echo "Szerver elindítva a háttérben PID: $APP_PID (Port: $PORT)"

cleanup() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    echo "Szerver leállítása (PID: $APP_PID)..."
    kill -15 "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

# Várakozás a szerver felszállására (max 15 másodperc)
READY=0
for i in {1..15}; do
  if curl -s -f "http://${HOST}:${PORT}/api/status" > /dev/null 2>&1; then
    READY=1
    echo "✅ A headless Spring Boot REST API sikeresen elindult a ${PORT}-es porton ($i. másodpercben)."
    break
  fi
  sleep 1
done

if [ $READY -ne 1 ]; then
  echo "::error::A headless REST API nem indult el 15 másodpercen belül!"
  kill -9 "$APP_PID" 2>/dev/null || true
  exit 1
fi

echo "=== 🔍 Végpontok és REST API Szerződés-ellenőrzése ==="

# 1. REST API Status és Actuator Hiány ellenőrzés
curl -s -f "http://${HOST}:${PORT}/api/status" | grep -q '"status":"UP"' || {
  echo "::error::REST API /api/status nem UP!"
  exit 1
}
echo "✅ REST API /api/status sikeresen ellenőrizve."

ACTUATOR_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${HOST}:${PORT}/actuator/health")
if [ "$ACTUATOR_CODE" != "404" ]; then
  echo "::error::Actuator endpoint nem 404-gyel tért vissza (kód: $ACTUATOR_CODE)!"
  exit 1
fi
echo "✅ Actuator health hiánya (404 Not Found) sikeresen ellenőrizve."

# 2. CORS Preflight ellenőrzés
curl -s -I -X OPTIONS "http://${HOST}:${PORT}/api/search" | grep -qi "Access-Control-Allow-Origin" || {
  echo "::error::CORS preflight nem adott Access-Control-Allow-Origin fejlécet"
  exit 1
}
echo "✅ CORS Preflight megfelelően működik."

# 3. Alapvető REST API végpontok szerződés-ellenőrzése
echo "REST API végpontok ellenőrzése..."
curl -s -f "http://${HOST}:${PORT}/api/config" | grep -q "cities" || {
  echo "::error::/api/config nem adott érvényes választ"
  exit 1
}
curl -s -f "http://${HOST}:${PORT}/api/portals" | grep -q "id" || {
  echo "::error::/api/portals nem adott érvényes választ"
  exit 1
}
curl -s -f "http://${HOST}:${PORT}/api/cities" | grep -q "Budapest" || {
  echo "::error::/api/cities nem adott érvényes választ"
  exit 1
}
curl -s -f "http://${HOST}:${PORT}/api/status" | grep -q "UP" || {
  echo "::error::/api/status nem adott érvényes választ"
  exit 1
}

# 4. Headless tisztaság ellenőrzése: nincs beágyazott SPA fallback router, ismeretlen útvonal 404
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://${HOST}:${PORT}/dashboard")
if [ "$HTTP_CODE" -ne 404 ]; then
  echo "::error::Headless REST API nem 404-et adott ismeretlen útvonalra: $HTTP_CODE"
  exit 1
fi
echo "✅ Headless végpont-izoláció (404 Not Found a nem létező SPA útvonalakra) ellenőrizve."

echo "✅ Headless Smoke Teszt sikeresen befejeződött."
