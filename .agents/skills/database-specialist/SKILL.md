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

Ez a skill a tárolási architektúra, a JSON perzisztencia réteg, az atomi I/O műveletek, a duplikátumszűrés és az adatbázis-migráció alapelveit és gyakorlatát rögzíti az **ingatlan-kereso** alkalmazáshoz.

---

## 1. Jelenlegi Tárolási Architektúra (Kétszintű Fájlalapú Perzisztencia)

Az alkalmazás jelenleg egy könnyűsúlyú, nulla külső adatbázis-függőséggel működő két-szintű JSON tároló modellt valósít meg az `IngatlanRepository` osztályban:

```text
data/
├── cities/
│   ├── budapest.json         # Városszintű szegmentált hirdetés- és URL-tár
│   ├── debrecen.json
│   └── hajduszoboszlo.json
├── ingatlanok_db.json        # Globális összesített adatbázis (aggregált keresésekhez)
└── last_config.json          # Legutóbbi keresési paraméterek állapota
```

### 1.1 Előnyök és Korlátok
* **Előnyök:** Nulla adatbázis szerver telepítés, ember által is olvasható formátum, egyszerű biztonsági mentés (fájlmásolás), gyors indulás.
* **Kihívások:** Párhuzamos szálak konkurens írása, fájlsérülés kockázata hirtelen leálláskor, nagy adatmennyiségnél memóriaterhelés, hiányzó másodlagos indexek.

---

## 2. Megbízhatósági és Konkurenciakezelési Szabályok

### 2.1 Thread-Safety és Szinkronizáció
* A memóriabeli gyorsítótárakhoz kötelező a `ConcurrentHashMap` és `Collections.newSetFromMap(new ConcurrentHashMap<>())` használata.
* Fájlírási és olvasási fázisban alkalmazz szálbiztos zárolást:
  ```java
  private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
  ```
* Olvasáskor `rwLock.readLock().lock()`, módosítás és lemezre mentés idején `rwLock.writeLock().lock()`.

### 2.2 Atomi Lemezre Írás (Atomic File Write Pattern)
Soha ne írj közvetlenül a végleges `.json` fájlba! Ha a folyamat írás közben leáll (áramszünet, megszakítás), a fájl csonka és sérült lesz.
**Kötelező atomi csere minta:**
1. Írd a tartalmat egy ideiglenes fájlba: `<filename>.tmp`.
2. Helyezd át atomi módon az operációs rendszer szintjén:
   ```java
   Path tempFile = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
   Files.writeString(tempFile, jsonContent, StandardCharsets.UTF_8);
   Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
   ```

### 2.3 Gyors és Pontos Duplikátumszűrés
* **Elsődleges kulcs (Canonical Identity):** A hirdetés normalizált URL-je (query paraméterek és tracking tagek, pl. `utm_*` eltávolításával).
* **Másodlagos ujjlenyomat (Secondary Hash):** Portál + Település + Kikiáltási ár + Méret + Szobaszám ujjlenyomat, ha az URL dinamikusan változna.
* O(1) időbonyolultságú létezés-ellenőrzés a memóriabeli `Set<String>` URL gyűjteményben.

---

## 3. Séma Evolúció és Visszafelé Kompatibilitás

Amikor új mező kerül a modellbe (pl. `minFloorArea`, `viewed`, `viewedAt`):
1. **Biztonságos deszerializáció:** A Gson alapértelmezetten `null` vagy `false` értékkel inicializálja a hiányzó mezőket a korábbi JSON fájlok betöltésekor.
2. **Defensive Getters:** A domain objektumban biztosíts ésszerű alapértelmezést, ha a mező nincs beállítva.
3. **Migrációs ellenőrzés:** Új mező mentésekor a korábbi rekordok zökkenőmentesen kiegészülnek.

---

## 4. Jövőbeli Átállási Útiterv: SQLite / H2 Beágyazott Adatbázis

Amikor a hirdetések száma meghaladja a tízezres nagyságrendet, a JSON tároló helyett beágyazott SQL adatbázisra (pl. SQLite vagy H2) való áttérés javasolt.
A részletes migrációs architektúrát és a javasolt relációs sémát a [SQLite Migrációs Tervben](./references/sqlite-migration-plan.md) találod.

---

## 5. Adatbázis Specialista Ellenőrző Lista

- [ ] **Atomi fájlírás:** Minden JSON mentés temp fájlon keresztül, `ATOMIC_MOVE`-val valósul meg?
- [ ] **Thread-safety:** Minden memóriabeli kollekció thread-safe?
- [ ] **Zero-Data-Loss:** Hibás mentés esetén a korábbi adatállomány érintetlen marad?
- [ ] **Indexelt keresés:** A létezés-ellenőrzés O(1) sebességgel fut memóriában?
- [ ] **Karakterkódolás:** Minden fájl I/O explicit módon `StandardCharsets.UTF_8` kódolást használ?
