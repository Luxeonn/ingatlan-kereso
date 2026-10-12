# Ingatlan Kereső - Projekt Dokumentáció és Kontextus (gemini.md)

## 1. Projekt Áttekintés

Az **ingatlan-kereso** egy Java alapú segédprogram, amelynek célja meghatározott ingatlanhirdetési portálok (Debrecen környéki eladó lakások) automatizált figyelése.
A program periodikusan vagy igény szerint lekéri a keresési találati listákat, kiszűri a már korábban látott hirdetéseket, az új linkeket automatikusan megnyitja a felhasználó alapértelmezett böngészőjében, valamint rögzíti azokat egy helyi naplófájlban.

* **Eredeti szerző:** Pusinszky Viktor (2017)
* **Kezelt portálok:**
  * Ingatlan.com (`ingatlan.com`)
  * Duna House (`dh.hu`)
  * Ingatlanbazár (`ingatlanbazar.hu`)
  * Otthon Centrum (`oc.hu`)

---

## 2. Technológiai Stack

* **Nyelv & Keretrendszer:** Java (alapértelmezetten Java 8 körüli szintaktika)
* **Build rendszer:** Apache Maven (`pom.xml`)
  * `org.jsoup:jsoup:1.10.3` – HTML letöltés, DOM parszer és CSS szelektálás
  * `commons-io:commons-io:2.5` – Fájlműveletek (`FileUtils.writeLines`)
  * `org.codehaus.mojo:exec-maven-plugin:1.6.0` – Parancssori futtatás támogatása
* **Rendszerszintű integráció:** Windows API hívás (`rundll32 url.dll,FileProtocolHandler`) a böngészőfülök megnyitásához

---

## 3. Könyvtárstruktúra és Komponensek

```text
ingatlan-kereso/
├── pom.xml                                      # Maven build konfiguráció és függőségek
├── README.md                                    # Eredeti rövid leírás és teendők
├── gemini.md                                    # Rendszer- és architektúra leírás
├── .gitignore
├── LICENSE                                      # GPL licenc
└── src/main/java/
    ├── Main.java                                # Belépési pont, orchestrator
    └── ingatlanok/
        ├── helper/
        │   └── IngatlanHelper.java              # Fájlkezelés (mentés, betöltés), böngésző megnyitás
        ├── persistence/
        │   └── IngatlanDataBaseMock.java        # Memóriabeli Set a már feldolgozott linkek nyilvántartására
        └── service/
            ├── Ingatlan.java                    # Interfész a portál scannerekhez (`getUrls()`)
            └── impl/
                ├── IngatlanStart.java           # Közös absztrakt ősosztály (Jsoup letöltés: `getUrlContent`)
                ├── IngatlanCom.java             # ingatlan.com scraper
                ├── DunaHause.java               # dh.hu scraper
                ├── IngatlanBazar.java           # ingatlanbazar.hu scraper
                └── OtthonCentrum.java           # oc.hu scraper
```

---

## 4. Működési Folyamat (Workflow)

```mermaid
flowchart TD
    A[Start: Main.java] --> B[IngatlanHelper.startContent]
    B --> C[(Fájl olvasása: 2017_06_06.txt -> IngatlanDataBaseMock memóriába)]
    C --> D[Portálok lekérdezése párhuzamosan/egymás után]
    D --> D1[IngatlanCom.getUrls]
    D --> D2[DunaHause.getUrls]
    D --> D3[IngatlanBazar.getUrls]
    D --> D4[OtthonCentrum.getUrls]
    D1 & D2 & D3 & D4 --> E{Új URL-ek szűrése}
    E --> F[IngatlanHelper.openUrl: Megnyitás böngészőben]
    E --> G[IngatlanHelper.saveIngatlanToFile: Új URL-ek mentése fájlba]
    F & G --> H[Kész]
```

1. **Adatbázis inicializálás:** `IngatlanHelper.startContent()` beolvassa a lokális fájlban tárolt korábbi URL-eket a `IngatlanDataBaseMock` in-memory halmazába (`Set<String>`).
2. **Keresési eredmények letöltése:** A scraper osztályok (`IngatlanStart` leszármazottak) Jsoup segítségével letöltik a keresési URL-ek első oldalát a találatszám megállapításához, majd végiglapozzák a találatokat.
3. **Duplikátum-szűrés:** A megtalált hirdetés-linkeket összeveti a memóriában lévő linkekkel.
4. **Értesítés és Mentés:**
   * Ha új link érkezett, a rendszer a Windows beépített `rundll32` parancsával közvetlenül megnyitja azt a böngészőben.
   * Az új linkek listája hozzáfűzésre kerül a helyi fájlhoz.

---

## 5. Azonosított Technikai Adósságok és Kihívások

1. **Elavult HTML szelekció & Bot védelem (Web Scraping törékenység):**
   * A weboldalak (különösen az ingatlan.com és az oc.hu) 2017 óta többször megújultak, modern frontend technológiákra (Next.js/React, SSR/CSR) és erős bot-védelemre (pl. Cloudflare, PerimeterX, Datadome) váltottak.
   * A régi CSS osztályok (`span.results__number__count`, `div.listing__card`, stb.) jelenlegi formájukban nagy valószínűséggel nem működnek.
2. **Bedrótozott konfigurációk:**
   * A mentési fájlnév mereven kódolt (`2017_06_06.txt`).
   * A keresési paraméterek (Debrecen, 9-11 mFt, 35+ m2) az osztályok forráskódjába vannak égetve ahelyett, hogy külső konfigurációból (pl. `application.properties`, JSON, CLI paraméterek) olvasná őket.
3. **Platformfüggőség:**
   * `rundll32 url.dll,FileProtocolHandler` kizárólag Windowson működik; érdemes a standard `java.awt.Desktop.getDesktop().browse(uri)` hívásra cserélni.
4. **Függőségek és Java verzió:**
   * A `pom.xml` nem rögzít Java fordítási verziót (`maven.compiler.source` / `target`).
   * A Jsoup 1.10.3 (2017-es) és a Commons IO 2.5 elavultak, frissítésük javasolt.
5. **Kivételkezelés:**
   * Több helyen üres `catch (Exception e) {}` blokkok találhatók, ami megnehezíti a hibakeresést és a hálózati hibák detektálását.

---

## 6. Javasolt Fejlesztési Irányok (Roadmap)

* [ ] **Konfigurációs réteg kialakítása:**
  * Keresési profilok (város, ár sáv, szobaszám, méret) külső JSON vagy properties fájlba helyezése.
* [ ] **Modernizáció és Függőségfrissítés:**
  * Java 17 vagy 21 beállítása a `pom.xml`-ben.
  * Jsoup frissítése a legfrissebb verzióra.
  * Modern böngészési metódus (`java.awt.Desktop`) bevezetése.
* [ ] **Scraper réteg felülvizsgálata:**
  * Fejlesztői/REST végpontok vagy publikus JSON API-k felkutatása a nyers HTML scraping helyett.
  * Szükség esetén headless böngésző integráció (Playwright Java vagy Selenium) a dinamikus JS és bot-védelem kezelésére.
* [ ] **Adatperzisztencia korszerűsítése:**
  * Egyszerű SQLite vagy H2 adatbázis bevezetése a sima szöveges fájl helyett (időbélyeg, ár, státusz, hirdetés címe tárolásával).
* [ ] **Értesítési csatornák bővítése:**
  * Böngésző ablakok tömeges felnyitása helyett opcionálisan Telegram bot, Discord webhook vagy e-mail értesítés küldése.

---

## 7. Munkafolyamat és Verziókezelési Szabályzat (Workflow Rule)

> [!IMPORTANT]
> **Kötelező GitHub PR Szabályzat az AI Asszisztens számára:**
> 1. **Közvetlen `master`/`main` ágra commitolás és pusholás TILOS.** Minden módosítást új munkavégző ágon (`git checkout -b <branch-name>`) kell elvégezni.
> 2. **Pull Request automatikus létrehozása:** A változtatásokat fel kell tölteni a GitHub-ra (`git push -u origin <branch-name>`), majd Pull Request-et kell nyitni (`gh pr create`).
> 3. **PR Link a válaszban:** A beszélgetésben a feladat befejezésekor **mindig kötelező közvetlen linket adni az elkészült GitHub Pull Request-re**.

