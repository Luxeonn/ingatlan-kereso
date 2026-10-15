# Konténer Orkesztrációs és Üzemeltetési Útmutató

Ez a dokumentum a Docker és Docker Compose környezet üzembe helyezését, konfigurálását és parancskészletét mutatja be.

---

## 1. Példa `Dockerfile` (Production Multi-Stage)

```dockerfile
# 1. Építési fázis (Build Stage)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Függőségek gyorstárazása
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Forráskód fordítása és fat JAR csomagolása
COPY src ./src
RUN mvn package -DskipTests -B

# 2. Futtatási fázis (Runtime Stage)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root rendszerfelhasználó létrehozása
RUN addgroup -g 10001 -S appgroup && \
    adduser -u 10001 -S appuser -G appgroup

# Kötetmappák létrehozása és jogosultságok beállítása
RUN mkdir -p /app/data /app/logs && \
    chown -R appuser:appgroup /app

# Fat JAR másolása a build fázisból
COPY --from=builder --chown=appuser:appgroup /workspace/target/ingatlan-kereso-*.jar /app/app.jar

USER appuser

ENV SERVER_HOST=0.0.0.0 \
    SERVER_PORT=8080 \
    SQLITE_DB_PATH=/app/data/ingatlanok.db

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:ZAllocationSpikeTolerance=5", "-Xms128m", "-Xmx384m", "-jar", "/app/app.jar"]
```

---

## 2. Példa `docker-compose.yml` (Orkesztráció)

```yaml
services:
  ingatlan-kereso:
    build:
      context: .
      dockerfile: Dockerfile
    image: ghcr.io/luxeonn/ingatlan-kereso:latest
    container_name: ingatlan-kereso-app
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:8080"
    volumes:
      - ingatlan_data:/app/data
      - ingatlan_logs:/app/logs
    environment:
      - SERVER_HOST=0.0.0.0
      - SERVER_PORT=8080
      - SQLITE_DB_PATH=/app/data/ingatlanok.db
      - APP_LOG_LEVEL=INFO
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 512M
        reservations:
          memory: 256M

volumes:
  ingatlan_data:
    name: ingatlan_data
  ingatlan_logs:
    name: ingatlan_logs
```

---

## 3. Gyakori Üzemeltetési Parancsok

### Indítás és Leállítás
```bash
# Háttérben történő indítás építéssel
docker compose up -d --build

# Naplók valós idejű követése
docker compose logs -f ingatlan-kereso

# Konténer leállítása az adatok megőrzésével
docker compose down
```

### Biztonsági Mentés és Adatbázis Ellenőrzés
```bash
# SQLite adatbázis konzisztencia ellenőrzése
sqlite3 data/ingatlanok.db "PRAGMA integrity_check;"

# Adatbázis biztonsági mentése forró állapotban (online backup)
sqlite3 data/ingatlanok.db ".backup 'data/backup_$(date +%Y%m%d_%H%M%S).db'"
```
