# Biztonsági irányelvek

## Támogatott ágak

| Ág / Verzió | Állapot | Megjegyzés |
| :--- | :--- | :--- |
| `develop` | Támogatott | Aktív fejlesztés és folyamatos biztonsági javítások |
| `main` | Támogatott | Stabil kiadások, kritikus és biztonsági javítások |
| `< 1.0.0` | Nem támogatott | Frissítés javasolt az aktuális kiadásra |

## Sérülékenység jelentése

> [!IMPORTANT]
> **Nyilvános GitHub issue-ban vagy PR-ben tilos biztonsági hibát közölni!**

1. **Privát bejelentés (ajánlott):** A GitHub [Security Advisories](https://github.com/Luxeonn/ingatlan-kereso/security/advisories) felületén a **Report a vulnerability** gombbal.
2. **Közvetlen kapcsolat:** Ha a privát űrlap nem érhető el, vedd fel a kapcsolatot a karbantartóval (`@Luxeonn`).
3. **Javasolt tartalom:** Sérülékenység típusa, érintett komponens, reprodukciós lépések / PoC és becsült hatás.
4. **Válaszidő (SLA):** Visszaigazolás **48 órán belül**, kiértékelés **5 munkanapon belül**, kritikus hiba javítása jellemzően **7–14 napon belül**.

## Alkalmazásbiztonsági (AppSec) védelmek

### 1. Szerver, hálózat és HTTP védelem (Spring Boot 4.1.2 / Java 25 + Nginx)
- **Loopback alapértelmezett kötés:** A backend alapértelmezetten csak a `127.0.0.1` címen figyel (`server.address=${SERVER_HOST:127.0.0.1}` az [`application.yml`](src/main/resources/application.yml)-ben); az Actuator nincs jelen, a statikus erőforrás-kiszolgálás kikapcsolt (`spring.web.resources.add-mappings: false`), mert a statikus SPA-t és `/api` proxyt az Nginx ([`frontend/nginx.conf`](frontend/nginx.conf)) végzi.
- **Loopback CORS ([`WebCorsConfig`](src/main/java/ingatlanok/config/WebCorsConfig.java)):** Kizárólag explicit helyi loopback origin-ek (`localhost`, `127.0.0.1`, `0.0.0.0`, `[::1]`) engedélyezettek; az origin nélküli `OPTIONS` preflight kérések szigorúan kezeltek.
- **Biztonsági fejlécek ([`SecurityHeadersFilter`](src/main/java/ingatlanok/config/SecurityHeadersFilter.java) és [`frontend/nginx.conf`](frontend/nginx.conf)):** Minden válasz tartalmazza a `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` és `Referrer-Policy: strict-origin-when-cross-origin` fejléceket.
- **Validáció és hibakezelés:** Jakarta Bean Validation (`@Valid`, `@Validated`, `@NotBlank`, `@Positive`, `@Size`) és [`GlobalExceptionHandler`](src/main/java/ingatlanok/web/GlobalExceptionHandler.java) egységes `ApiErrorResponse` választ ad stack trace szivárgása nélkül (`RequestLoggingFilter` korrelációs azonosítóval).

### 2. Web Scraping, HTML-import és SSRF védelem
- **Egyetlen kimenő HTTP sink ([`HttpFetcher`](src/main/java/ingatlanok/common/HttpFetcher.java)):** Az ArchUnit és a `scripts/validate-skills.sh` kikényszeríti, hogy `Jsoup.connect` kizárólag a `HttpFetcher`-ben szerepelhet. Csak az `ALLOWED_HOST_PREFIXES` listán lévő hazai portálok (`dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, `ingatlan.com`) érhetők el 15 másodperces timeouttal, `PortalRateLimiter`-rel és `PortalCircuitBreaker`-rel.
- **Paraméter-tisztítás:** A keresési adatok a [`SlugUtils`](src/main/java/ingatlanok/common/SlugUtils.java), [`CityUtils`](src/main/java/ingatlanok/common/CityUtils.java) és [`PropertyParser`](src/main/java/ingatlanok/common/PropertyParser.java) osztályokon haladnak át.
- **Kézi HTML-import korlát ([`IngatlanComHtmlImportService`](src/main/java/ingatlanok/core/IngatlanComHtmlImportService.java)):** Legfeljebb **5 MB** HTML fogadható el, és a `sourceUrl` kizárólag `ingatlan.com` vagy `*.ingatlan.com` hosztra mutathat.

### 3. Kliensoldali XSS, perzisztencia és ellátási lánc
- **XSS és külső linkek:** React 19 automatikus escaping; minden külső hirdetés-megnyitás az [`openExternalUrl`](frontend/src/utils/formatters.ts) segédfüggvényen át történik (`noopener,noreferrer`), a közvetlen `window.open` hívást az ESLint tiltja.
- **Adatbázis és konténer:** Paraméterezett Spring Data JPA lekérdezések (H2 / PostgreSQL 17), nem-root konténerfuttatás (`appuser`, UID `10001` a [`Dockerfile`](Dockerfile)-ban).
- **Automatizált CI kapuk:** **Gitleaks** (titokkeresés), **Trivy** (`CRITICAL`/`HIGH` konténerszűrés), **CodeQL**, **Dependabot**, **Checkstyle** és **ArchUnit**.

## Hatókör

- **Érvényes (In-Scope):** RCE, SSRF, XSS, CORS megkerülés, SQL/JPA injection, path traversal (Nginx/API végpontok), jogosulatlan adatelérés vagy validációs megkerülés.
- **Kizárt (Out-of-Scope):** Külső ingatlanportálok saját WAF/Cloudflare tiltása vagy rate-limitje, helyi asztali példány elleni DoS, reprodukálható PoC nélküli elméleti megállapítások.
