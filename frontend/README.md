# Ingatlan Kereső — Frontend SPA

A React 19 + TypeScript 5.9 + Vite 8.3 + Tailwind CSS v4 alapú webes felület fejlesztői és tesztelési útmutatója. A teljes projekt bemutatása a [gyökér README](../README.md)-ben található.

## Fejlesztési és ellenőrzési parancsok

```bash
# Függőségek telepítése
npm ci

# Fejlesztői szerver indítása (port: 5173, API proxy -> localhost:8080)
npm run dev

# OpenAPI típusok újragenerálása az openapi/api-spec.yaml fájlból
npm run generate-api

# TypeScript típusellenőrzés
npm run typecheck

# ESLint ellenőrzés (Clean Code és komplexitási szabályok, 0 warning gate)
npm run lint

# Vitest unit- és komponens-tesztek
npm run test

# Böngészős E2E tesztek (Playwright)
npm run test:e2e

# Éles build és helyi előnézet (port: 4173)
npm run build
npm run preview
```

## OpenAPI Single Source of Truth

A `src/types/generated-api.ts` fájlt kizárólag az `npm run generate-api` paranccsal szabad előállítani a [`../openapi/api-spec.yaml`](../openapi/api-spec.yaml) alapján — kézzel ne szerkeszd! A CI (`openapi-drift`) ellenőrzi, hogy a generált kliens naprakész-e.

## Unit és E2E Tesztek

- **Vitest (`src/test/`):** Hermetikus unit- és komponens-tesztek backend nélkül, közös [`src/test/testFixtures.ts`](src/test/testFixtures.ts) mock-gyárakkal (`npx vitest` watch módhoz, `npx vitest run <fájl>` egyedi futtatáshoz).
- **Playwright (`e2e/`):** Első futtatás előtt telepítsd a Chromiumot: `npx playwright install --with-deps chromium`. A tesztek alapértelmezetten a `vite preview` szerveren futnak a **`4173`**-as porton (felülírható a `PLAYWRIGHT_BASE_URL` környezeti változóval).

## Kapcsolódó dokumentáció

- [Akadálymentességi útmutató (WCAG 2.2 AA)](../Accessibility.md)
- [Közreműködői útmutató](../CONTRIBUTING.md)
- [REST API Specifikáció (OpenAPI)](../openapi/api-spec.yaml)
- [Frontend AI fejlesztői irányelvek](AGENTS.md)
