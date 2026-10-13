# Ingatlan Kereső (Real Estate Scraper & Monitor)

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Apache%20Maven-3.9%2B-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Jsoup](https://img.shields.io/badge/Jsoup-1.23.2-6DB33F?style=for-the-badge)](https://jsoup.org/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg?style=for-the-badge)](LICENSE)

Java 21 LTS alapú asztali segédprogram és webes vezérlőpult magyar ingatlanhirdetési portálok automatizált figyelésére, duplikátumszűrésére és az új találatok azonnali megtekintésére.

---

## 📌 Áttekintés

Az **ingatlan-kereso** egy modern, beágyazott REST API szerverrel és reszponzív webes felülettel ellátott ingatlanfigyelő rendszer. Megszabadít a portálok manuális böngészésétől: egyetlen kereséssel több vezető hazai hirdetési oldal adatait aggregálja, kiszűri a már korábban látott hirdetéseket, és azonnal megmutatja a friss piaci ajánlatokat.

* **Egykattintásos indítás:** Asztali indítóállomány (`start-ui.bat`), amely elindítja a szervert és automatikusan megnyitja a böngészőt.
* **Többvárosos figyelés:** Egyszerre több település vagy előre definiált régió (pl. Debrecen és agglomerációja, egyetemvárosok) figyelése.
* **Valós idejű szűrés és rendezés:** Hirdetések szűrése településre, árra, típusra, csak új találatokra, valamint azonnali rendezés ár szerint.
* **0-összeomlás garancia:** Robusztus hibatűrés a védelem alatt álló vagy SPA technológiájú portáloknál (graceful fallback).

---

## 🚀 Főbb Funkciók

* **Modern Web Dashboard (SPA):** Letisztult, gyors egyoldalas felület világos és sötét móddal (Dark/Light toggle), azonnali gépelés közbeni gyorskeresővel.
* **Multi-Select Településválasztó:** Több mint 35 előre rögzített magyar város és gyorsválasztó sablonok (Presets), egyedi településnév megadásával kiegészítve.
* **Hirdetéskártyák és Metaadatok:** Ár, alapterület, szobaszám, település, közvetlen portállink és feltűnő **✨ ÚJ TALÁLAT** matrica a friss hirdetéseken.
* **Rendezés és Szűrés:** 
  * Ár szerinti rendezés (alapértelmezett, növekvő vagy csökkenő).
  * Szűrés kizárólag az új (még nem látott) hirdetésekre egyetlen kapcsolóval.
* **Böngészős kényelmi funkciók:** Kapcsoló az új találatok külön böngészőlapokon történő automatikus megnyitásához, illetve dedikált gomb az összes új találat egyidejű megnyitásához.
* **Strukturált JSON Adatbázis:** A hirdetési előzmények városonként szegmentálva (`data/cities/<város>.json`) és aggregált formában (`data/ingatlanok_db.json`) tárolódnak, így a korábbi találatok újraindítás után sem vesznek el.

---

## 🌐 Támogatott Portálok és Állapotuk

| Portál | Weboldal | Állapot | Megjegyzés |
| :--- | :--- | :---: | :--- |
| **Duna House** | [dh.hu](https://dh.hu) | **Működik** | Élő találatok, ár, alapterület, szobaszám kinyerése |
| **Otthon Centrum** | [oc.hu](https://www.oc.hu) | **Működik** | Élő találatok és kártya metaadatok kinyerése |
| **Jófogás Ingatlan** | [jofogas.hu](https://ingatlan.jofogas.hu) | **Működik** | Élő találatok kinyerése ár- és lokációszűrőkkel |
| **Ingatlan.com** | [ingatlan.com](https://ingatlan.com) | ⚠️ *Korlátozott* | Cloudflare bot-védelem alatt; közvetlen keresési linkkel kezelt fallback |
| **Ingatlanbazár** | [ingatlanbazar.hu](https://ingatlanbazar.hu) | ⚠️ *Korlátozott* | Kliensoldali Angular SPA renderelés; közvetlen hivatkozással kezelt fallback |

---

## 💻 Rendszerkövetelmények és Gyorsindítás

### Előfeltételek
* **Java JDK:** Java 21 LTS vagy újabb (OpenJDK, Eclipse Temurin, Oracle JDK vagy JetBrains JBR)
* **Build eszköz:** Apache Maven 3.9+ (vagy a fejlesztői környezetbe épített Maven)
* **Böngésző:** Bármely modern webböngésző (Chrome, Firefox, Edge)

---

### Indítási Módok

#### 1. Egykattintásos indítás Windows alatt (Ajánlott)
Kattints duplán a gyökérkönyvtárban található **`start-ui.bat`** fájlra.  
A parancsfájl automatikusan felderíti a Java környezetet, elindítja a beágyazott szervert, és megnyitja a böngészőt a `http://localhost:8080` címen.

#### 2. Indítás PowerShellből
```powershell
./start-ui.ps1
```

#### 3. Indítás Maven parancssorból
```bash
# Web Dashboard elindítása (alapértelmezett port: 8080)
mvn compile exec:java
```

#### 4. Háttérbeli parancssori (CLI / Headless) futtatás
Ha webes felület nélkül, pusztán a parancssorból szeretnéd lekérni és naplózni az aktuális hirdetéseket:
```bash
mvn compile exec:java "-Dexec.args=--cli"
```

---

## ⚙️ Konfiguráció és Adattárolás

Az alkalmazás nem igényel külső adatbázis-kiszolgálót; a beállításokat és hirdetési előzményeket a helyi `data/` mappában tárolja JSON formátumban:

* **`data/last_config.json`:** Az utolsó keresés során használt szűrőfeltételek (kiválasztott városok, ingatlantípus, min/max ár, aktív portálok, lapmegnyitási beállítások). Az alkalmazás újraindításakor automatikusan betölti ezeket.
* **`data/cities/<city-key>.json`:** Városonként csoportosított hirdetési előzmények a duplikátumok gyors és pontos szűréséhez.
* **`data/ingatlanok_db.json`:** Aggregált hirdetésadatbázis minden valaha rögzített ingatlan metaadataival és az első rögzítés időbélyegével.

> [!TIP]
> A felületen a **"Keresési előzmények törlése"** gombra kattintva bármikor alaphelyzetbe állítható a helyi adatbázis, így a következő kereséskor minden találat újra frissként fog megjelenni.

---

## 🛠 Fejlesztői Útmutató és Hozzájárulás

Szívesen fogadunk hibajavításokat és új funkciókat! A hozzájáruláshoz kövesd az alábbi lépéseket:

### 1. Build és tesztelés
```bash
# Függőségek letöltése és teljes fordítás
mvn clean compile

# Egység- és integrációs tesztek futtatása
mvn test

# Futtatható csomag összeállítása
mvn package
```

### 2. Git munkafolyamat
* A fejlesztések kiindulási és integrációs ága a **`develop`**. Közvetlenül a `main` vagy `develop` ágra pusholni tilos.
* Hozz létre új munkavégző ágat a feladat típusának megfelelő prefixummal:
  * `feature/<rovid-angol-leiras>` – új funkció
  * `fix/<rovid-angol-leiras>` – hibajavítás
  * `docs/<rovid-angol-leiras>` – dokumentáció módosítása
  * `refactor/<rovid-angol-leiras>` – kódstruktúra átalakítása
* Pull Request beküldésekor használj beszédes, angol nyelvű Conventional Commits címet (pl. `feat: add price filter to dashboard`).

### 3. Helyi Push-védelem aktiválása
A véletlen közvetlen push-ok megelőzésére a projekt előre konfigurált git hookokat tartalmaz:
```bash
git config core.hooksPath .githooks
```

---

## 📖 Rendszer- és Architektúra Dokumentáció

A rendszer mélyreható technikai felépítéséről, a beágyazott szerverről, a web scraping mechanizmusokról, valamint az automatizált AI munkafolyamat-szabályokról részletes információ a **[GEMINI.md](GEMINI.md)** fájlban található.

---

## 📄 Licenc és Szerző

* **Eredeti szerző:** Pusinszky Viktor (2017)
* **Licenc:** [GNU General Public License v3.0](LICENSE)
