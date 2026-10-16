---
name: scraper-security-engineer
description: Web scraping robusztussági, adatkinyerési és alkalmazásbiztonsági (AppSec) mérnök. Használd ezt a skillt portál scraper motorok (Jsoup DOM, JSON-LD, belső REST API-k) fejlesztéséhez és karbantartásához, Cloudflare/WAF bot-védelem detektálásához és kezeléséhez, valamint a Spring Boot 4.1.0 beágyazott webszerver és REST API hardeninghez (CORS szűrők, CSP, localhost kötés, Actuator biztonság), bemenet-validációhoz, XSS/SSRF és injekciós sebezhetőségek elhárításához.
---

# Scraper & Security Specialist

Ez a szakértői skill felel a **külső web scraping motorok stabilitásáért** és a **rendszer alkalmazásszintű védelméért (AppSec)**.

## 1. Szakterület és Felelősségek

### A. Web Scraping Motorok (Portal Engines)
- **Támogatott portálok:** Duna House (`dh.hu`), Otthon Centrum (`oc.hu`), Jófogás (`jofogas.hu`), Költözzbe (`koltozzbe.hu`), Ingatlanbazár (`ingatlanbazar.hu`), Ingatlan.com (`ingatlan.com` fallback).
- **Kinyerési stratégiák:**
  - Jsoup DOM struktúrák rugalmas lekérdezése (fallback CSS selectorok).
  - Schema.org JSON-LD adatok kinyerése és normalizálása.
  - Belső JSON REST API-k fogyasztása egyedi fejlécekkel.
- **WAF és Bot-védelem:**
  - Cloudflare és WAF észlelése, `BOT_PROTECTED` státusz visszaadása összeomlás helyett.
  - Böngésző ujjlenyomat és modern fejlécek (`Sec-Ch-Ua`, valós User-Agent).
- **0-Crash Graceful Degradation:** Egyetlen portál hibája vagy struktúraváltása sem akaszthatja meg a párhuzamos lekérdezést.

### B. Alkalmazásbiztonság (AppSec) és Spring Boot Hardening
- **Hálózati réteg és Spring Boot szerver:**
  - Kifejezett `127.0.0.1` localhost kötés (`server.address=127.0.0.1`) alapértelmezetten vagy környezeti változóval konfigurált biztonságos bind.
  - `GlobalCorsFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`) szigorú preflight és origin kezeléssel.
  - Biztonsági HTTP fejlécek: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`.
  - Helyes HTTP státuszkódok és metódusellenőrzés (405 Method Not Allowed, 400 Bad Request).
- **Bemenet-validáció és Sebezhetőség-megelőzés:**
  - Path Traversal megelőzése a statikus fájlkiszolgálás során (`SpaWebMvcConfig`).
  - XSS védelem a felületen és a Spring `@RestController` JSON kimeneteken.
  - SSRF megelőzése külső URL-ek megnyitásakor vagy lekérésekor.
  - SQL / H2 injekció megelőzése típusbiztos Spring Data JPA lekérdezésekkel.

## 2. Döntési és Felülvizsgálati Checklist (PR Gate)

A PR-ok jóváhagyásakor (`scraper-security-engineer`) vizsgálandó szempontok:
- [ ] A scraper motorok kezelik az ismeretlen DOM-ot, timeoutot és bot-védelmet (0-crash).
- [ ] Nincsenek hardkódolt szenzitív adatok vagy veszélyes külső hívások.
- [ ] A bemenetek (keresési paraméterek, URL-ek, entitás mezők) szigorúan validáltak és fertőtlenítettek.
- [ ] A Spring Boot beágyazott szerver védett az illetéktelen hozzáféréstől és injection támadásoktól.
