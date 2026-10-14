# SQLite / Beágyazott SQL Tárolási Architektúra és Séma
 
Ez a dokumentum a megvalósult beágyazott ACID relációs adatbázis (SQLite) sémáját, indexelési stratégiáját és a jövőbeli optimalizációkat mutatja be.

---

## 1. Működési Konfiguráció és Célok
* **Skálázhatóság:** Több tízezer hirdetés kezelése másodperctöredék alatti lekérdezési idővel.
* **ACID Tranzakciók:** Zéró adatvesztés és automatikus rollback hiba esetén.
* **WAL mód:** `PRAGMA journal_mode = WAL; PRAGMA synchronous = NORMAL;` a szimultán gyors írásért és olvasásért.
* **Másodlagos indexek:** Gyors szűrés ár, város, portál és felfedezési dátum szerint közvetlenül az adatbázis motorban.
* **Zéró szerver üzemeltetés:** Egyetlen beágyazott `.db` fájl a `data/ingatlanok.db` útvonalon (`org.xerial:sqlite-jdbc` függőséggel).

---

## 2. Megvalósult Relációs Adatbázis Séma

```sql
CREATE TABLE IF NOT EXISTS properties (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    canonical_url TEXT NOT NULL UNIQUE,
    portal_id TEXT NOT NULL,
    city_key TEXT NOT NULL,
    title TEXT,
    price_raw TEXT,
    price_numeric INTEGER,
    price_formatted TEXT,
    floor_area REAL,
    rooms INTEGER,
    price_per_sqm REAL,
    details TEXT,
    discovered_at TEXT NOT NULL,
    is_new INTEGER DEFAULT 1,
    viewed INTEGER DEFAULT 0,
    viewed_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_properties_city ON properties(city_key);
CREATE INDEX IF NOT EXISTS idx_properties_price ON properties(price_numeric);
CREATE INDEX IF NOT EXISTS idx_properties_portal ON properties(portal_id);
CREATE INDEX IF NOT EXISTS idx_properties_discovered ON properties(discovered_at DESC);
```

---

## 3. Adatfolyam és Visszafelé Kompatibilitás

1. **Interfész-alapú elválasztás (`ListingStorage`):**
   - Az `SqliteListingStorage` és `JsonFileListingStorage` azonos szerződést teljesít.
2. **Automatikus Adatmigrátor:**
   - Induláskor, ha a `data/ingatlanok.db` üres vagy nem létezik, a `LegacyMigrationService` automatikusan importálja az összes meglévő `data/cities/*.json` és `data/ingatlanok_db.json` rekordot.
3. **Zökkenőmentes Működés:**
   - A `WebServer` és a `SearchExecutionEngine` kizárólag a Facade-en / interfészen keresztül kommunikál, így a tároló réteg módosítása nulla hatással van a kliensre és az üzleti logikára.

---

## 4. Jövőbeli Optimalizációs Útiterv

* **Adatarchiválás:** Inaktív, 90 napnál régebbi hirdetések áthelyezése egy archivált táblába (`properties_archive`).
* **Connection Pooling:** Nagy konkurens REST kérések esetén HikariCP pool illesztése.
* **Full-Text Search (FTS5):** SQLite FTS5 virtuális tábla a hirdetés leírások gyors szöveges kereséséhez.
