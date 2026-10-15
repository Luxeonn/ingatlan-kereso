---
name: devops-specialist
description: >-
  Konténerizációs, felhő és DevOps rendszermérnök. Használd ezt a skillt Dockerfile tervezéséhez és
  multi-stage optimalizálásához, Docker Compose orkesztrációhoz, környezeti változók és perzisztens
  kötetek kezeléséhez, GHCR konténer image-ek konfigurálásához, valamint fej nélküli (headless) és
  szerveroldali futtatási architektúrák kialakításához.
metadata:
  icon: cloud
---

# DevOps és Konténerizációs Specialista Skill (Container & DevOps Engineer)

Ez a skill a Docker konténerizáció, a mikroszolgáltatás futtatókörnyezet, az erőforrás-gazdálkodás, a kötetkezelés és a felhő/konténer orkesztráció irányelveit rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Konténerizációs Architektúra és Filozófia

Az alkalmazás natív asztali és izolált Docker konténer környezetben egyaránt első osztályú állampolgárként működik.

### 1.1 Multi-Stage Docker Építési Minta
A konténer image-nek minimális méretűnek, biztonságosnak és gyorsan indíthatónak kell lennie:

1. **Stage 1 (Builder):**
   - Hivatalos Maven image Temurin JDK 21 alapon (`maven:3.9-eclipse-temurin-21-alpine`).
   - Függőségek előtöltése és gyorstárazása (`pom.xml` másolása, `mvn dependency:go-offline`).
   - Fat JAR építés a `maven-shade-plugin` segítségével, optimalizált MANIFEST belépési ponttal (`ingatlanok.Main`).
2. **Stage 2 (Runtime):**
   - Karcsúsított, hivatalos JRE futtatókörnyezet (`eclipse-temurin:21-jre-alpine`).
   - Szigorúan nem tartalmaz fordítóeszközöket, csomagkezelő forrásokat vagy felesleges binárisokat.
   - Dedikált, korlátozott jogosultságú felhasználó (`appuser:appgroup`, UID 10001).

---

## 2. Orkesztráció és Környezetkezelés (`docker-compose.yml`)

### 2.1 Kötetkezelés és Adatintegritás
Az SQLite relációs adatbázis és az alkalmazásnaplók megmaradnak a konténer újraindítása vagy új verzióra frissítése után is:
* **Deklaratív `VOLUME ["/app/data", "/app/logs"]`:** Lehetővé teszi a zéró-konfigurációs indítást (`docker run -d -p 8080:8080 <image>`), ahol a Docker automatikusan felügyeli a perzisztens tárolást.
* **Menedzselt nevesített kötetek (`named volumes`):** A `docker-compose.yml` és CLI futtatás során az `ingatlan_data:/app/data` és `ingatlan_logs:/app/logs` köteteket használjuk, elkerülve a hoszt fájlrendszer jogosultsági és útvonalkülönbségeit.
* **Hoszt bind mountok (opcionális):** `./data:/app/data` és `./logs:/app/logs` közvetlen hozzáférést ad a hoszt felőli vizsgálathoz.

### 2.2 Környezeti Változók és Rugalmas Konfiguráció
Minden hálózati és alkalmazás-paraméter külső környezeti változókból konfigurálható:
* `SERVER_HOST`: Alapértelmezés konténeren belül `0.0.0.0` (hálózati elérés a hídon keresztül), míg asztali gépen `127.0.0.1`.
* `SERVER_PORT`: Kiszolgáló port (alapértelmezés: `8080`).
* `SQLITE_DB_PATH`: SQLite adatbázis fájl útvonala (alapértelmezés: `data/ingatlanok.db`).
* `APP_LOG_LEVEL`: Naplózási részletesség (`INFO`, `DEBUG`, `WARN`, `ERROR`).

### 2.3 Erőforrás-korlátok és Újraindítási Szabályzat
```yaml
services:
  ingatlan-kereso:
    restart: unless-stopped
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 512M
        reservations:
          memory: 256M
```

---

## 3. Fej Nélküli Szerver Környezet (Headless Operation)

* **Zero-Server-Desktop-Call:** Konténer környezetben nem létezik grafikus felület vagy asztali böngésző (`java.awt.Desktop.isDesktopSupported() == false`). A böngésző lapok megnyitását a szerver elegánsan átengedi a kliensoldali JavaScriptnek (`window.open`), így semmilyen kivétel vagy hiba nem történik szerveroldalon.
* **Graceful Shutdown:** A konténer leállításakor (`SIGTERM`) a szerver leállítja a beágyazott HTTP szervert és a háttérszálakat, lezárva a JDBC SQLite kapcsolatokat és a WAL fájlokat.

A részletes orkesztrációs konfigurációt és telepítési mintákat a [Konténer Orkesztrációs Útmutatóban](./references/container-orchestration-guide.md) találod.

---

## 4. DevOps Specialista Ellenőrző Lista

- [ ] **Multi-stage build:** A futtatási image kizárólag a JRE-t és a fordított fat JAR-t tartalmazza?
- [ ] **Non-root felhasználó:** Az alkalmazás `USER appuser` környezetben fut?
- [ ] **Kötetcsatolások:** A `./data` és `./logs` perzisztens kötetként van konfigurálva megfelelő jogosultságokkal?
- [ ] **Host port binding:** A host oldalon a biztonságos `127.0.0.1:8080:8080` port-mapping van érvényben?
- [ ] **Erőforrás limitek:** Be van állítva a memóriakorlát (pl. 512M) a konténer elszaladásának megelőzésére?
- [ ] **Zero Desktop hiba:** A szerver nem kísérel meg GUI/Desktop hívásokat fejetlen konténerben?
