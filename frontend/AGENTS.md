# Frontend Type-Safety & Engineering Directives (`frontend/`)

This module-specific guide supplements the root [AGENTS.md](../AGENTS.md) and the canonical fullstack type-safety audit ([docs/architecture/type-safety/audit.md](../docs/architecture/type-safety/audit.md)). Every AI agent or developer modifying `frontend/` must adhere strictly to these rules.

---

## 1. Mandatory Type-Safety Principles (React 19 + TypeScript 5.9)

1. **OpenAPI Single Source of Truth (`generated-api.ts`):**
   - Derive all frontend DTO types (`src/types/property.ts`, `src/types/api.ts`, `src/types/portal.ts`) directly from `src/types/generated-api.ts` (`components['schemas']`), generated via `npm run generate-api`.
   - Never hand-duplicate OpenAPI schema interfaces or edit `src/types/generated-api.ts` manually.
   - Always run `npm run generate-api` after any `openapi/api-spec.yaml` change; CI (`openapi-drift` job) blocks merges if `generated-api.ts` is out of date.
2. **Closed Domain Sets & Exhaustiveness:**
   - Represent closed domain value sets (`CanonicalPropertyType`, `PortalId`, `PortalScrapeStatus` in `src/types/`, `SortOption` in `src/store/filterUrlSync.ts`, `ListingSubView` in `src/store/useUiStore.ts`) as strict TypeScript union types or `as const` tuples, never as unconstrained `string` (while backend `ListingStatus` values appear on generated DTO fields in `generated-api.ts`).
   - Use compile-time exhaustiveness checks (`const _exhaustive: never = value`) in `switch` statements over domain unions (`TS-032`).
   - Do not widen closed domain unions with `| string` (`TS-001` is resolved in `src/types/property.ts`); any future transitional exception must be registered in [docs/architecture/type-safety/allowlist.md](../docs/architecture/type-safety/allowlist.md).
3. **Boundary Narrowing Over Type Assertions:**
   - Prefer runtime type guards (`function isX(val: unknown): val is X`) or Zod schemas (`zod`) at external boundaries (DOM `localStorage`, URL query parameters, third-party chart events — e.g., `isCityStat` and `isHistogramBin`) instead of unchecked `as SomeType` assertions (`TS-022`, `TS-023`, `TS-024`).
   - Use `Object.prototype.hasOwnProperty.call(obj, key)` instead of `key in obj` when narrowing record keys against prototype pollution (`TS-027`).
4. **Multi-Source Cluster Invariant & Actions:**
   - `IngatlanItem` represents a deduplicated property cluster across portals. Never read `item.priceInfo.amountHuf` or `item.detailsInfo.pricePerSquareMeter` directly in UI components, hooks, filters, or analytics; always call `getEffectivePrice(item)` and `getEffectiveSqmPrice(item)` from `src/utils/formatters.ts` (enforced via ESLint AST `no-restricted-syntax`).
   - Manual dedup split/merge actions and Ingatlan.com HTML import go through typed OpenAPI-backed clients (`src/api/dedupApi.ts`, `src/api/portalApi.ts`, `src/hooks/useDedupActions.ts`).
5. **Safe External Link Navigation:**
   - Never call `window.open` directly in components; always use `openExternalUrl` from `src/utils/formatters.ts` (enforced via ESLint `no-restricted-properties` and `scripts/validate-skills.sh`).

---

## 2. Strictly Forbidden Constructs & Bypasses (CI-Blocked)

The following patterns fail ESLint (`npm run lint`), TypeScript (`npm run typecheck`), or the repository guardrail validator (`bash scripts/validate-skills.sh -v`):

- **Explicit `any` (`: any`, `as any`, `<any>`):** Blocked by `@typescript-eslint/no-explicit-any: error`.
- **TypeScript Compiler Suppressions (`@ts-ignore`, `@ts-nocheck`, `@ts-expect-error`):** Blocked by `@typescript-eslint/ban-ts-comment: error` and `scripts/validate-skills.sh`.
- **Double Type Assertions (`as unknown as T`):** Blocked in `frontend/src/` (outside `src/test/`) by ESLint AST `no-restricted-syntax` and `scripts/validate-skills.sh`. (`ALLOW-TS-002..004` in `client.ts` and Recharts charts are already resolved.)
- **Closed Union Widening (`CanonicalPropertyType | string`):** Blocked by ESLint AST `no-restricted-syntax` (`ALLOW-TS-001` in `property.ts` is already resolved).
- **Unlisted `eslint-disable` Comments:** Adding `eslint-disable`, `eslint-disable-line`, or `eslint-disable-next-line` in `frontend/src/` or `frontend/e2e/` is blocked by `scripts/validate-skills.sh` unless registered in [docs/architecture/type-safety/allowlist.md](../docs/architecture/type-safety/allowlist.md) (currently only `ALLOW-TS-005` in `src/hooks/useSearch.ts`).

---

## 3. Non-Tampering Rule & Local Verification

AI agents **must never** disable or weaken rules in `frontend/eslint.config.js`, `frontend/tsconfig.json`, `frontend/package.json`, or `scripts/validate-skills.sh` to make a failing check pass. All configuration files are protected by [.github/CODEOWNERS](../.github/CODEOWNERS).

Before committing or pushing any frontend change, run:
```bash
cd frontend && npm run generate-api && npm run typecheck && npm run lint && npm run test && npm run build
```
And run `npm run test:e2e` (in `frontend/`) when modifying UI flows, routing, or Playwright specs.
