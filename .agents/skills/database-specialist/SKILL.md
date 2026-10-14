---
name: database-specialist
description: >-
  Adatbázis és perzisztencia tervezési specialista. Használd ezt a skillt az IngatlanRepository
  fájlalapú és relációs adattárolási mechanizmusainak optimalizálásához, kétszintű JSON tárolási
  stratégiákhoz, szálbiztos konkurenciakezeléshez (ReentrantReadWriteLock, ConcurrentHashMap),
  atomi fájlíráshoz, duplikátumszűréshez, séma-migrációkhoz, vagy beágyazott SQL (SQLite/H2)
  átállás tervezéséhez.
metadata:
  icon: database
---

# Adatbázis és Perzisztencia Specialista Skill

Ez a skill a tárolási architektúra, a beágyazott relációs SQLite perzisztencia réteg, az atomi I/O műveletek, a duplikátumszűrés és az adatbázis-optimalizáció alapelveit és gyakorlatát rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Megvalósult Tárolási Architektúra (SQLite & Leválasztott Perzisztencia)

Az alkalmazás az absztrakt `ListingStorage` és `ConfigStorage` interfészekre épülő, leválasztott perzisztencia modellt valósít meg az `ingatlanok.persistence` csomagban:

```text
data/
├── ingatlanok.db            # Elsődleges relációs SQLite adatbázis (WAL mód, indexek)
├── config.json              # Aktuális keresési konfiguráció
└── cities/                  # Visszafelé kompatibilis JSON export és fallback
    ├── budapest.json
    ├── debrecen.json
    └── ...
```

### 1.1 Fő Implementációk
1. **`SqliteListingStorage` (Elsődleges Tároló):**
   - Beágyazott ACID relációs adatbázis `org.xerial:sqlite-jdbc` meghajtóval.
   - Magas párhuzamosságú olvasás és írás **WAL (Write-Ahead Logging)** módban (`PRAGMA journal_mode=WAL; PRAGMA synchronous=NORMAL;`).
   - B-Tree indexelés az azonnali szűréshez és duplikátum-ellenőrzéshez:
     - `idx_listings_city` a városszintű lekérdezésekhez.
     - `idx_listings_portal` a portálstatisztikákhoz.
     - `idx_listings_discovered_at` az időrendi rendezéshez.
   - Batch mentés tranzakcióban (`connection.setAutoCommit(false)`), minimalizálva a lemezre írási overheadet.
2. **`JsonFileListingStorage` (Atomi Fájlalapú Fallback):**
   - Kétszintű JSON struktúra (`data/cities/*.json` és aggregált napló).
   - `ReentrantReadWriteLock` védett szálbiztos hozzáférés.
   - Szigorúan atomi lemezre írás (`.tmp` + `StandardCopyOption.ATOMIC_MOVE`).
3. **`IngatlanRepository` (Singleton Facade):**
   - Visszafelé kompatibilis átjáró a felsőbb rétegek felé, amely transzparensen delegál a `ListingStorage` és `ConfigStorage` implementációkra.
   - Automatikus migráció induláskor a régebbi JSON fájlokból az új SQLite adatbázisba.

---

## 2. Megbízhatósági és Konkurenciakezelési Szabályok

### 2.1 Thread-Safety és SQLite Konkurencia
* A memóriabeli gyorsítótárakhoz kötelező a `ConcurrentHashMap` és `Collections.newSetFromMap(new ConcurrentHashMap<>())` használata.
* SQLite kapcsolatkezelés:
  - Használj szálbiztos zárolást (`ReentrantReadWriteLock`) az adatbázis műveletek koordinálására, megelőzve a `SQLITE_BUSY` hibákat.
  - Mindig zárd le a JDBC erőforrásokat (`try-with-resources` blokkban a `Connection`, `PreparedStatement` és `ResultSet` objektumokat).
* Fájlalapú JSON műveleteknél:
  - Olvasáskor `rwLock.readLock().lock()`, módosítás és lemezre mentés idején `rwLock.writeLock().lock()`.

### 2.2 Atomi Lemezre Írás (Atomic File Write Pattern)
Ha JSON fájlt kell menteni, soha ne írj közvetlenül a végleges `.json` fájlba:
1. Írd a tartalmat egy ideiglenes fájlba: `<filename>.tmp`.
2. Helyezd át atomi módon az operációs rendszer szintjén:
   ```java
   Path tempFile = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
   Files.writeString(tempFile, jsonContent, StandardCharsets.UTF_8);
   Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
   ```

### 2.3 Gyors és Pontos Duplikátumszűrés
* **Elsődleges kulcs (Canonical Identity):** A hirdetés normalizált URL-je (query paraméterek és tracking tagek, pl. `utm_*` eltávolításával).
* **Adatbázis integritás:** `canonical_url TEXT NOT NULL UNIQUE` megszorítás.
* Memóriabeli `Set<String>` gyorsítótár a lekérdezések gyorsításához, kombinálva az SQLite indexelt lekérdezésekkel.

---

## 3. Séma Evolúció és Visszafelé Kompatibilitás

Amikor új mező kerül a modellbe (pl. `minFloorArea`, `viewed`, `viewedAt`):
1. **Biztonságos deszerializáció:** A Gson és a JDBC mapping alapértelmezetten `null` vagy `false` értékkel inicializálja a hiányzó mezőket a korábbi rekordok betöltésekor.
2. **Defensive Getters:** A domain objektumban biztosíts ésszerű alapértelmezést, ha a mező nincs beállítva.
3. **Idempotens DDL:** Minden táblalétrehozás és indexelés `CREATE TABLE IF NOT EXISTS` és `CREATE INDEX IF NOT EXISTS` szintaxissal történjen.
4. **Automatikus Adatmigráció:** Az `IngatlanRepository` és `LegacyMigrationService` automatikusan és adatvesztés nélkül migrálnak a régebbi tárolókból.

---

## 4. Jövőbeli Bővíthetőség és Skálázási Irányok

Amikor a rendszer hálózatos vagy multi-tenant környezetbe kerül:
* **Külső SQL támogatás:** A `ListingStorage` interfészen keresztül külső PostgreSQL vagy MySQL kapcsolat bevezethető.
* **Adatretenciós és Archiválási Szabályzat:** Periodikus tisztító feladat a 90+ napos inaktív hirdetések archiválására.
* **Connection Pooling:** Nagy párhuzamos konkurens terhelés esetén HikariCP bevezetése.

A részletes sémadefiníciót és a migrációs háttéranyagot a [SQLite Architektúra és Séma Referenciában](./references/sqlite-migration-plan.md) találod.

---

## 5. Adatbázis Specialista Ellenőrző Lista

- [ ] **ACID integritás:** Minden tömeges mentés tranzakcióban (`commit()` / `rollback()`) történik?
- [ ] **WAL mód:** Az SQLite kapcsolat aktiválja a `PRAGMA journal_mode=WAL` beállítást?
- [ ] **Erőforrás-kezelés:** Minden `Connection`, `PreparedStatement` és `ResultSet` le van zárva `try-with-resources` segítségével?
- [ ] **Atomi fájlírás:** Minden JSON mentés temp fájlon keresztül, `ATOMIC_MOVE`-val valósul meg?
- [ ] **Thread-safety:** Minden memóriabeli kollekció thread-safe és nincs race condition?
- [ ] **Indexelt keresés:** A létezés-ellenőrzés és a városszűrés indexen keresztül fut?
- [ ] **Karakterkódolás:** Minden fájl és adatbázis I/O explicit módon `StandardCharsets.UTF_8` kódolást használ?
