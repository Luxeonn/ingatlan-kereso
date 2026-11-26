<div align="center">

<br />

# Ingatlan Kereső

### **A hazai ingatlanpiac, zaj nélkül.**

Valódi árak, kiszűrt duplikációk, azonnal látható alkalmi vételek.

<br />

[![CI](https://img.shields.io/github/actions/workflow/status/Luxeonn/ingatlan-kereso/ci.yml?branch=develop&style=flat-square&label=CI&labelColor=1d1d1f)](https://github.com/Luxeonn/ingatlan-kereso/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/Luxeonn/ingatlan-kereso?style=flat-square&label=Verzi%C3%B3&color=0071e3&labelColor=1d1d1f)](https://github.com/Luxeonn/ingatlan-kereso/releases)
[![License](https://img.shields.io/badge/Licenc-GPL--3.0-6e6e73?style=flat-square&labelColor=1d1d1f)](LICENSE)

<br />

[Indítás ›](#inditas) &nbsp;&nbsp;·&nbsp;&nbsp; [Specifikáció ›](#specifikacio) &nbsp;&nbsp;·&nbsp;&nbsp; [Architektúra ›](docs/architecture.md)

<br />

</div>

---

<br />

<div align="center">

## Keresés. Újragondolva.

Ugyanaz az ingatlan több portálon, eltérő árakon.<br />
**Az Ingatlan Kereső rendet tesz a káoszban — automatikusan.**

</div>

<br />

<table>
  <tr>
    <td width="50%" valign="top">
      <h3>Minden forrás. Egyetlen kattintás.</h3>
      <p>Párhuzamos adatgyűjtés a legnagyobb hazai portálokról (<code>dh.hu</code>, <code>oc.hu</code>, <code>jofogas.hu</code>, <code>koltozzbe.hu</code>, <code>ingatlanbazar.hu</code>, <code>ingatlan.com</code>) — Java 25 virtuális szálakon, hibatűrő motorral.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Nulla duplikáció. Valódi ár.</h3>
      <p>Összevonja az azonos ingatlanok hirdetéseit, és mindig a <strong>legkedvezőbb aktív árat</strong> emeli ki. Téves egyezés? Egyetlen kattintással szétválasztható vagy összevonható.</p>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <h3>Alkalmi vétel. Elsőként.</h3>
      <p>Külön számítja a lakóingatlan- és telekpiaci medián m²-árat. Legalább <strong>20%-os árelőnynél</strong> azonnal jelez — áresési toplistával és interaktív grafikonokkal.</p>
    </td>
    <td width="50%" valign="top">
      <h3>Saját radar. Megőrizve.</h3>
      <p>Teljes ártörténet, megtekintési állapotok, kedvencek, archívum és egykattintásos mentett keresési presetek — egyetlen jó lehetőség sem vész el.</p>
    </td>
  </tr>
</table>

<br />

---

<a id="specifikacio"></a>

<br />

<div align="center">

## A motorháztető alatt.

Teljesítmény, típusbiztonság és tiszta architektúra. Kompromisszumok nélkül.

</div>

<br />

| Réteg | Technológia |
| :--- | :--- |
| **Webes felület** | React 19 · TypeScript 5.9 · Tailwind CSS 4 · Vite 8.3 |
| **Backend API** | Java 25 LTS · Spring Boot 4.2 · Virtual Threads · OpenAPI |
| **Adatgyűjtés** | Jsoup · REST · Node.js & Playwright Sidecar · SSRF-védett `HttpFetcher` |
| **Perzisztencia** | Spring Data JPA · PostgreSQL 17 · H2 |

<br />

---

<a id="inditas"></a>

<br />

<div align="center">

## Indítás. Másodpercek alatt.

Egyetlen parancs, teljes rendszer — PostgreSQL, backend, sidecar és Nginx felület.

</div>

<br />

```bash
# Előre felépített képek letöltése és indítása
docker compose pull backend frontend
docker compose up -d --no-build

# Vagy helyi forráskódból
docker compose up -d --build
```

Indítás után nyisd meg: **[http://localhost:8080](http://localhost:8080)**  
<sub>Windows tipp: a <code>scripts/start-docker.bat</code> interaktív menüből indítja a lokális buildet vagy a GHCR képeket.</sub>

<br />

---

<br />

<div align="center">

[Architektúra](docs/architecture.md) ·
[OpenAPI](openapi/api-spec.yaml) ·
[Frontend](frontend/README.md) ·
[Sidecar](tools/README.md) ·
[Közreműködés](CONTRIBUTING.md) ·
[Akadálymentesség](Accessibility.md) ·
[Biztonság](SECURITY.md) ·
[Licenc (GPL-3.0)](LICENSE)

</div>
