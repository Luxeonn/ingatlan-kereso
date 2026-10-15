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

Ez a skill a tárolási architektúra, a beágyazott NoSQL dokumentum adatbázis (Nitrite DB + MVStore) perzisztencia réteg, az atomi I/O műveletek, a duplikátumszűrés és az adatbázis-optimalizáció alapelveit és gyakorlatát rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Megvalósult Tárolási Architektúra (Nitrite NoSQL & Leválasztott Perzisztencia)

Az alkalmazás az absztrakt `ListingStorage` és `ConfigStorage` interfészekre épülő, leválasztott perzisztencia modellt valósít meg az `ingatlanok.persistence` csomagban:

```text
data/
└── ingatlanok_nosql.db      # Elsődleges beágyazott NoSQL adatbázis (Nitrite DB + MVStore tömörítéssel)
```

### 1.1 Fő Implementációk
1. **`NitriteListingStorage` (Elsődleges Tároló):**
   - Beágyazott dokumentum-orientált NoSQL adatbázis `org.dizitart:nitrite` és `nitrite-mvstore-adapter` motorral.
   - Magas párhuzamosságú olvasás és írás MVStore tömörített tárolóval.
   - B-Tree indexelés az azonnali szűréshez és O(1) duplikátum-ellenőrzéshez:
     - Egyedi (Unique) B-Tree index az `url`-re (garantált kanonikus integritás).
     - Non-unique indexek: `city`, `normalizedCity`, `portal`, `discoveredAt`.
   - Idősoros ártörténet (`price_history`) és státusznapló (`status_log`) külön NoSQL kollekciókban.
2. **`NitriteConfigStorage`:**
   - Keresési konfigurációk és rendszerbeállítások NoSQL dokumentumként való tárolása.
3. **`IngatlanRepository` (Singleton Facade):**
   - Átjáró a felsőbb rétegek felé, amely transzparensen delegál a `ListingStorage` és `ConfigStorage` implementációkra. Tiszta lappal indul, felesleges legacy migráció nélkül.

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
1. **Biztonságos deszerializáció:** A NoSQL dokumentum modell alapértelmezetten `null` vagy `false` értékkel inicializálja a hiányzó mezőket a korábbi rekordok betöltésekor.
2. **Defensive Getters:** A domain objektumban biztosíts ésszerű alapértelmezést, ha a mező nincs beállítva.
3. **Idempotens Séma:** Minden Nitrite kollekció és B-Tree index inicializálása idempotens módon történik.

---

## 4. Jövőbeli Bővíthetőség és Skálázási Irányok

Amikor a rendszer hálózatos vagy multi-tenant környezetbe kerül:
* **Külső SQL / NoSQL támogatás:** A `ListingStorage` interfészen keresztül külső PostgreSQL, MongoDB vagy Elasticsearch kapcsolat bevezethető.
* **Adatretenciós és Archiválási Szabályzat:** Periodikus tisztító feladat a 90+ napos inaktív hirdetések archiválására.

---

## 5. Adatbázis Specialista Ellenőrző Lista

- [ ] **ACID integritás:** Minden tömeges mentés tranzakcióban (`commit()` / `rollback()`) történik?
- [ ] **WAL mód:** Az SQLite kapcsolat aktiválja a `PRAGMA journal_mode=WAL` beállítást?
- [ ] **Erőforrás-kezelés:** Minden `Connection`, `PreparedStatement` és `ResultSet` le van zárva `try-with-resources` segítségével?
- [ ] **Atomi fájlírás:** Minden JSON mentés temp fájlon keresztül, `ATOMIC_MOVE`-val valósul meg?
- [ ] **Thread-safety:** Minden memóriabeli kollekció thread-safe és nincs race condition?
- [ ] **Indexelt keresés:** A létezés-ellenőrzés és a városszűrés indexen keresztül fut?
- [ ] **Karakterkódolás:** Minden fájl és adatbázis I/O explicit módon `StandardCharsets.UTF_8` kódolást használ?
