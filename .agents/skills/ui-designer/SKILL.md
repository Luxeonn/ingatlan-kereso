---
name: ui-designer
description: >-
  UI/UX designer és frontend specialista. Használd ezt a skillt, ha a feladat a webes dashboard
  (WebDashboardHtml) felületének tervezése, modernizálása, reszponzív layoutok kialakítása, új interaktív
  komponensek (szűrők, stepperek, chipek, KPI kártyák, találati listák, loading skeletonok) készítése,
  vagy tipográfiai, színelméleti és mikróinterakciós finomítások elvégzése.
metadata:
  icon: palette
---

# UI Designer & Frontend Specialista Skill

Ez a skill a felhasználói élmény (UX), a vizuális hierarchia (UI), a design tokenek és az interaktív komponensek tervezési irányelveit rögzíti az **ingatlan-kereso** Single Page Application felületéhez.

---

## 1. UI/UX Filozófia és Technikai Keretek

### 1.1 Zéró Külső Frontend Függőség (Zero-Dependency Embedded SPA)
* A felület közvetlenül a `WebDashboardHtml.java` osztályban van beágyazva sablonként.
* Nincs Node.js, npm, Webpack vagy külső CDN függőség (az alkalmazás teljes mértékben offline is működőképes).
* Tiszta, modern **HTML5**, szigorúan szervezett **CSS3** (Tailwind CSS ihletésű utility-osztályok és komponens-osztályok), valamint moduláris **Vanilla ES6+ JavaScript**.

### 1.2 Vizuális Tervezési Alapelvek
1. **Adatvezérelt és Tiszta (Data-Dense yet Clean):**
   - Az ingatlanhirdetések információban gazdagok (ár, m², szobaszám, forrás, állapot). A kártyáknak áttekinthetőnek kell lenniük zsúfoltság nélkül.
2. **Azonnali Visszajelzés (Instant Visual Feedback):**
   - Minden felhasználói művelet (szűrés, rendezés, kattintás) azonnali vizuális választ ad (pl. skeleton animációk, badge változások, toast üzenetek).
3. **Reszponzív Rács (Responsive Grid):**
   - Mobiltól (1 oszlop) a táblagépen át (2 oszlop) a széles monitorokig (3-4 oszlop, `minmax(320px, 1fr)`) tökéletesen alkalmazkodó felület.

---

## 2. Design Tokenek és Színpaletta

* **Alap háttér:** Mély pala / sötét árnyalatok (`#0f172a`, `#1e293b`) a prémium sötét témához, vagy letisztult világos (`#f8fafc`, `#ffffff`).
* **Kiemelő színek (Brand Tokens):**
  - Elsődleges akcentus: Kék / Indigó (`#3b82f6` / `#6366f1`) – gombok, aktív fülek, fókusz.
  - Siker / Új találat: Smaragd (`#10b981`) – "✨ ÚJ TALÁLAT" jelvény, pozitív piaci trend.
  - Figyelem / Várakozás: Borostyán (`#f59e0b`) – bot-védelem fallback jelzés, lassabb válasz.
  - Veszély / Törlés: Rózsa / Vörös (`#ef4444`) – előzmény törlése, hibaállapot.
* **Portál Forrás Badge Színek:**
  - Duna House: Kék (`#1d4ed8`)
  - Otthon Centrum: Narancs (`#ea580c`)
  - Jófogás: Zöld / Türkiz (`#059669`)
  - Ingatlanbazár: Lila (`#7c3aed`)
  - Ingatlan.com: Cián / Tengerkék (`#0284c7`)

A részletes tokeneket és komponens mintákat a [Design Tokenek és Komponensek referenciában](./references/design-tokens-and-components.md) találod.

---

## 3. Kulcs Felületi Komponensek

### 3.1 Keresési és Szűrő Vezérlősáv
* **Többvárosos választó (Multi-City Chips):** Kattintható településcímkék törlés gombbal (`×`), valamint gyorsválasztó régiós sablonok (pl. "Budapest és környéke", "Egyetemvárosok").
* **Numerikus Stepperek:** Ár (M Ft) és alapterület ($m^2$) tartományvezérlők finomléptető gombokkal (`-` / `+`) és közvetlen beviteli mezővel.
* **Ingatlantípus Tabs / Dropdown:** Lakás, Ház, Garázs, Telek, Nyaraló, Iroda, Összes.
* **Valós idejű Rendezés:** Ár (növekvő/csökkenő), m² ár, frissesség szerint.

### 3.2 Piaci KPI Statisztikai Kártyák (Market Stats Bar)
* Összes találat száma.
* Új találatok aránya (kiemelt animált számláló).
* Átlagos kikiáltási ár (M Ft).
* Átlagos négyzetméterár (ezer Ft / $m^2$).
* Aktív / sikeres portálok állapota.

### 3.3 Hirdetéskártya (Property Card)
* **Header:** Portál badge (színes pill), ár kiemelten (Ft és M Ft), "ÚJ" pulzáló jelvény.
* **Body:** Cím, település, közvetlen link, alapterület ($m^2$), szobaszám, számított fajlagos ár (Ft/$m^2$).
* **Footer:** Felfedezés dátuma, megtekintettségi állapot jelző, "Megnyitás böngészőben" gomb.

### 3.4 Állapotkezelés (Feedback & Loading States)
* **Keresés folyamatban:** Finom shimmer animációjú skeleton kártyák és százalékos vagy portálonkénti állapotjelző sáv.
* **Üres lista (Empty State):** Barátságos magyarázó szöveg, javasolt szűrőmódosítások és reset gomb.
* **Toast Értesítések:** Lebegő, auto-dismissing értesítések sikeres műveletekről (pl. "3 új ingatlan megnyitva a böngészőben").

---

## 4. UI Designer Ellenőrző Lista

- [ ] **Mobil- és asztali nézet:** A felület 360px-től 2560px-ig törés- és vízszintes görgetésmentes?
- [ ] **Kontraszt és olvashatóság:** A szövegek és badge-ek megfelelnek a WCAG AA kontraszt-követelményeknek?
- [ ] **Mikróinterakciók:** A gombok hover, active és focus-visible állapotai jól láthatóak?
- [ ] **Zéró flicker:** A szűrés és rendezés zökkenőmentes, nem okoz teljes oldal-újratöltést?
- [ ] **Adatbiztonság:** A kártyákban megjelenített dinamikus szövegek megfelelően meg vannak tisztítva az XSS támadások ellen?
