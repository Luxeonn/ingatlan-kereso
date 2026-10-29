# 🏡 Ingatlan Kereső

[![CI Build](https://img.shields.io/github/actions/workflow/status/Luxeonn/ingatlan-kereso/ci.yml?branch=develop&style=flat-square&label=CI%20Build)](https://github.com/Luxeonn/ingatlan-kereso/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Luxeonn/ingatlan-kereso?style=flat-square&label=Verzi%C3%B3&color=007ec6)](https://github.com/Luxeonn/ingatlan-kereso/releases)
[![License](https://img.shields.io/badge/Licenc-GPL--3.0-blue?style=flat-square)](LICENSE)

![Java](https://img.shields.io/badge/Java-25%20LTS-ed8b00?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6db33f?style=flat-square)
![React](https://img.shields.io/badge/React-19.0-61dafb?style=flat-square)
![TypeScript](https://img.shields.io/badge/TypeScript-5.9-3178c6?style=flat-square)
![Vite](https://img.shields.io/badge/Vite-8.3-646cff?style=flat-square)
![Node.js](https://img.shields.io/badge/Node.js-22%2B-4c1?style=flat-square)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?style=flat-square)

<br />

> **Láss át a hazai ingatlanpiaci zajon.**  
> Egyetlen felület, amely helyetted fésüli át a legnagyobb magyar ingatlanportálokat, kiszűri a trükkös duplikációkat, leleplezi a rejtett árcsökkenéseket, és megmutatja, hol vannak a valódi piaci lehetőségek.

<br />

---

<br />

## ✨ Miért más az Ingatlan Kereső?

A hagyományos portálokon ugyanazt az ingatlant tucatnyi ügynökség hirdeti eltérő áron, pontatlan adatokkal és mesterségesen felpumpált kínálattal. Az **Ingatlan Kereső** a háttérben elvégzi a piszkos munkát, hogy neked már csak a tiszta képpel kelljen foglalkoznod:

- 🚀 **Villámgyors, párhuzamos portál-aggregáció**  
  Egyetlen kereséssel valós időben gyűjti össze a legfontosabb hazai ingatlanportálok kínálatát – több tucat megnyitott böngészőfül nélkül.

- 🧩 **Intelligens duplikáció-szűrés és ár-összevetés**  
  Felismeri, ha ugyanazt az ingatlant több helyen is hirdetik, egyetlen tiszta kártyára vonja össze őket, és azonnal a **legkedvezőbb elérhető árat** teszi eléd.

- 💎 **Piaci radar és „Alkalmi vétel” vadász**  
  Saját statisztikai elemzőmotorja folyamatosan méri a helyi piaci árszintet, és automatikusan megjelöli azokat az ingatlanokat, amelyek érdemben a piaci érték alatt érhetők el.

- 🛡️ **Beépített adatminőség-védelem**  
  Kiszűri a gyanús, hiányos vagy megtévesztő hirdetéseket, így sem a találati listádat, sem a piaci elemzéseket nem torzítják a hibás adatok.

- 📉 **Ártörténet és árváltozás-követés**  
  Nem felejt: rögzíti az árváltozásokat és az újrahirdetéseket, így pontosan látod, melyik eladó engedett az árból az elmúlt időszakban.

<br />

---

<br />

## 🏗️ Milyen komponensekből épül fel?

A rendszer modern, egymástól függetlenül skálázható rétegekre épül:

| Komponens | Technológiai alap | Mit csinál? |
| :--- | :--- | :--- |
| 🎨 **Webes Felület (SPA)** | React 19, TypeScript, Tailwind CSS | Gyors, letisztult keresőfelület, interaktív szűrők és vizuális piaci elemző dashboard. |
| ⚡ **Központi API Szerver** | Java 25 LTS, Spring Boot | Nagy teljesítményű, virtuális szálakra épülő háttérszolgáltatás a kérések azonnali kiszolgálására. |
| 🧠 **Elemző és Normalizáló Motor** | Saját üzleti algoritmusok | Hirdetések egységesítése, kereszt-portál összevonás, minőségellenőrzés és piaci statisztikák. |
| 🕷️ **Portál Konnektorok** | Hibatűrő adatgyűjtő modulok | Párhuzamos és védett kapcsolat a hazai ingatlanportálokkal, automatikus tartalék-stratégiákkal. |
| 🗄️ **Adattár (Perzisztencia)** | Spring Data JPA, H2 / PostgreSQL | Az ingatlanok, forráshirdetések és történeti ármozgások megbízható tárolása. |

<br />

---

<br />

## 🚀 Hogyan indítsd el?

### 🐳 1. Indítás Dockerrel (A legegyszerűbb mód)

Egyetlen lépésben elindítja a teljes rendszert (adatbázis, háttérszerver és webes felület):

```bash
# Előre felépített csomagok letöltése és indítása:
docker compose pull backend frontend
docker compose up -d --no-build

# VAGY teljes felépítés helyi forráskódból:
docker compose up -d --build
```

> 💡 **Tipp Windows felhasználóknak:** Indítsd el a `scripts/start-docker.bat` fájlt, amely egy kényelmes menüből kínálja fel az indítási opciókat!

👉 Indítás után nyisd meg a böngészőben: **[http://localhost:8080](http://localhost:8080)**

<br />

### 💻 2. Lokális fejlesztői indítás

Ha közvetlenül a forráskódból szeretnéd futtatni (**JDK 25** és **Node.js 22+** szükséges):

**1. Háttérszerver (Backend) indítása:**

```bash
# Linux / macOS
./gradlew bootRun

# Windows
.\gradlew.bat bootRun
```

**2. Webes felület (Frontend) indítása** *(külön terminálban)*:

```bash
cd frontend
npm ci
npm run dev
```

👉 Fejlesztői felület: **[http://localhost:5173](http://localhost:5173)** *(automatikusan kapcsolódik a `localhost:8080`-on futó szerverhez)*.

<br />

---

<br />

## 🧪 Minőségellenőrzés

```bash
# Backend ellenőrzés és csomagolás
./gradlew check bootJar

# Frontend ellenőrzés, tesztek és build
cd frontend && npm run lint && npm run typecheck && npm run test && npm run build
```

<br />

---

<br />

## 📚 További tudnivalók

- 🏛️ [Rendszerarchitektúra részletesen](docs/architecture.md)
- 🔌 [REST API Specifikáció (OpenAPI)](openapi/api-spec.yaml)
- 🔒 [Biztonsági irányelvek](SECURITY.md)
- 🤝 [Fejlesztői irányelvek](AGENTS.md)
- ⚖️ [Licenc (GPL-3.0)](LICENSE)
