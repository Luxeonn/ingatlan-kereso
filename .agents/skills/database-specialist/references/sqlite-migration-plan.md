# SQLite / Beágyazott SQL Migrációs Terv

Ez a dokumentum a fájlalapú JSON perzisztencia beágyazott ACID relációs adatbázisra (SQLite) történő jövőbeli migrálási tervét mutatja be.

---

## 1. Motiváció és Célok
* **Skálázhatóság:** Több tízezer hirdetés kezelése másodperctöredék alatti lekérdezési idővel.
* **ACID Tranzakciók:** Zéró adatvesztés és automatikus rollback hiba esetén.
* **Másodlagos indexek:** Gyors szűrés ár, város, alapterület és felfedezési dátum szerint közvetlenül az adatbázis motorban.
* **Zéró szerver üzemeltetés:** Egyetlen beágyazott `.db` fájl a `data/ingatlanok.db` útvonalon (`sqlite-jdbc` függőséggel).

---

## 2. Javasolt Relációs Adatbázis Séma

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

## 3. Lépésről Lépésre Történő Migrációs Stratégia

1. **Interfész Kivonása (`Repository Interface`):**
   - Hozz létre egy `IngatlanRepositoryInterface` interfészt, amelyet mind a `JsonIngatlanRepository`, mind az új `SqliteIngatlanRepository` megvalósít.
2. **Kettős Írási Fázis (Dual-Write Phase - Opcionális):**
   - Rövid ideig mindkét tárolóba menthetünk az adatintegritás verifikálásához.
3. **Automatikus Adatmigrátor:**
   - Induláskor, ha a `data/ingatlanok.db` üres vagy nem létezik, automatikusan importálja az összes meglévő `data/cities/*.json` és `data/ingatlanok_db.json` rekordot.
4. **Zökkenőmentes Átállás:**
   - A `WebServer` és az `IngatlanSearchService` kizárólag az interfészen keresztül kommunikál, így a migrálás nulla hatással van a kliensre és az üzleti logikára.
