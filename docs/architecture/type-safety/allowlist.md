# Fullstack Type-Safety Exception Allowlist & Governance Registry

- **Parent Issue**: GitHub Issue #404 (Umbrella Issue #398 — Fullstack Type Safety & Boundary Hardening)
- **Canonical Audit Input**: [docs/architecture/type-safety/audit.md](audit.md) (Issue #399)
- **Enforcement Scripts**: `scripts/validate-skills.sh`, `config/checkstyle/checkstyle.xml`, `frontend/eslint.config.js`, `src/test/java/ingatlanok/architecture/ArchitectureRulesTest.java`

---

## 1. Purpose & Non-Bypassable Governance Principle

By default, all type-safety bypasses, warning suppressions, unchecked type assertions, raw types, and closed-set string widenings are **strictly forbidden** and blocked in CI.

An exception is permitted **only** when:
1. A third-party library typing limitation, transitional migration boundary (#400–#403), or test-fixture requirement makes the construct unavoidable;
2. The exception is narrowed to the **smallest possible file and line/symbol scope**;
3. The exception is explicitly registered and justified in the machine-verified allowlist table below; and
4. Any modification to this allowlist is reviewed by the repository owner (`@Luxeonn` via `.github/CODEOWNERS`).

> [!CAUTION]
> **Self-Modification Ban:** AI agents are strictly prohibited from adding ad-hoc entries to this allowlist or weakening `scripts/validate-skills.sh`, `frontend/eslint.config.js`, `config/checkstyle/checkstyle.xml`, or `ArchitectureRulesTest.java` to silence a newly introduced compiler, linter, or architecture error. Fix the underlying type model instead!

---

## 2. Machine-Verified Type-Safety Exception Allowlist

`scripts/validate-skills.sh` parses the table below (rows starting with `| ALLOW-`) to verify that no undocumented bypasses exist in the codebase.

| Allowlist ID | Scope (File Path) | Construct / Rule | Audit Finding / Issue | Expiration / Target Removal | Explicit Technical Justification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `ALLOW-TS-001` | `frontend/src/types/property.ts` | `CanonicalPropertyType \| string` | `TS-001` (#400, #402) | Issue #400 / #402 | Transitional legacy union compatibility until OpenAPI `SearchCriteria.propertyType` and frontend filter types are unified in #402 and #400. |
| `ALLOW-TS-002` | `frontend/src/api/client.ts` | `as unknown as T` | `TS-020` (#400, #402, #403) | Issue #402 / #403 | Transitional generic REST client text fallback prior to OpenAPI-typed client (`openapi-fetch`) and runtime boundary validation in #402/#403. |
| `ALLOW-TS-003` | `frontend/src/components/analytics/CityComparisonChart.tsx` | `as unknown as CityStat` | `TS-022` (#400) | Issue #400 | Recharts 3 `Bar` `onClick` callback types the first parameter loosely; scheduled for replacement with a runtime type guard (`isCityStat`) in #400. |
| `ALLOW-TS-004` | `frontend/src/components/analytics/PriceDistributionChart.tsx` | `as unknown as HistogramBin` | `TS-022` (#400) | Issue #400 | Recharts 3 `Bar` `onClick` callback types payload loosely; scheduled for replacement with a runtime type guard (`isHistogramBin`) in #400. |
| `ALLOW-TS-005` | `frontend/src/hooks/useSearch.ts` | `eslint-disable-line react-hooks/exhaustive-deps` | Tech Infra | Permanent (Scoped) | Intentional trigger of `historyQuery.refetch()` strictly on `dashboardTab` / `listingSubView` transition without re-running on `historyQuery` object identity changes. |
| `ALLOW-JAVA-001` | `config/checkstyle/checkstyle.xml` | `SuppressionSingleFilter (noSynchronized)` | Arch Negative Test | Permanent (Test Fixture) | Allows `FixtureCoreViolatingSynchronized.java` in `src/test/` to compile so `ArchitectureRulesNegativeTest` can deterministically prove that ArchUnit rejects `synchronized` methods. |

---

## 3. Zero-Tolerance Banned Constructs (Never Allowlisted in Production)

The following constructs have **zero** active exceptions in production code and will immediately fail CI:

1. **Java `@SuppressWarnings` in `src/main/java/`**: 0 occurrences permitted (enforced by Checkstyle & `validate-skills.sh`).
2. **Java Raw `Map<String, Object>`, `List<Object[]>`, or wildcard `ResponseEntity<?>` in Web/Persistence/Core**: 0 occurrences permitted (enforced by ArchUnit `ArchitectureRulesTest.java`).
3. **Java Stringly-Typed Closed Domain Enums in `model/` and `persistence/entity/`**: `PropertyType`, `ListingStatus`, and `ListingObservation.StatusEvent` must be strongly typed enums (`@Enumerated(EnumType.STRING)` in JPA entities), never `String` (enforced by ArchUnit).
4. **Java Reflective Class Loading (`Class.forName`) in `src/main/java/`**: 0 occurrences permitted (enforced by ArchUnit & Checkstyle).
5. **TypeScript `@ts-ignore`, `@ts-nocheck`, `@ts-expect-error` in `frontend/src/` and `frontend/e2e/`**: 0 occurrences permitted (enforced by ESLint `@typescript-eslint/ban-ts-comment: error` & `validate-skills.sh`).
6. **TypeScript explicit `any` (`: any`, `as any`)**: 0 occurrences permitted (enforced by ESLint `@typescript-eslint/no-explicit-any: error`).
