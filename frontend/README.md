# Ingatlan Kereső — Frontend SPA

A modern React 19 + TypeScript + Tailwind CSS single page application (SPA) fejlesztői és tesztelési útmutatója.

## Fejlesztési parancsok

```bash
# Függőségek telepítése
npm ci

# Fejlesztői szerver indítása (port: 5173)
npm run dev

# OpenAPI típusok újragenerálása az openapi/api-spec.yaml specifikációból
npm run generate-api

# TypeScript típusellenőrzés
npm run typecheck

# ESLint ellenőrzés (Clean Code és komplexitási szabályok, 0 warning gate)
npm run lint

# Frontend izolált komponens- és unit tesztek futtatása (Vitest)
npm run test

# Böngészős E2E tesztek futtatása (Playwright)
npm run test:e2e

# Éles build készítése (Vite)
npm run build
```

## Frontend Unit Tesztek Futtatása

A frontend tesztcsomag a Vitest keretrendszerre és a React Testing Library-re épül, és teljesen izoláltan, élő backend nélkül, hermetikus mockokkal és Object Mother builder mintával (`src/test/testFixtures.ts`) fut.

```bash
# Gyors, egyszeri tesztfuttatás:
npm run test

# Folyamatos figyelő (watch) mód fejlesztéshez:
npx vitest

# Konkrét tesztfájl futtatása:
npx vitest run src/test/listingFilters.test.ts
```
