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

## 2. Machine-Verified Active Type-Safety Exception Allowlist

`scripts/validate-skills.sh` parses the active table below (rows starting with `| ALLOW-`) to verify that no undocumented bypasses exist in the codebase.

| Allowlist ID | Scope (File Path) | Construct / Rule | Audit Finding / Issue | Expiration / Target Removal | Explicit Technical Justification |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `ALLOW-TS-005` | `frontend/src/hooks/useSearch.ts` | `eslint-disable-line react-hooks/exhaustive-deps` | Tech Infra | Permanent (Scoped) | Intentional trigger of `historyQuery.refetch()` strictly on `dashboardTab` / `listingSubView` transition without re-running on `historyQuery` object identity changes. |
| `ALLOW-JAVA-001` | `config/checkstyle/checkstyle.xml` | `SuppressionSingleFilter (noSynchronized)` | Arch Negative Test | Permanent (Test Fixture) | Allows `FixtureCoreViolatingSynchronized.java` in `src/test/` to compile so `ArchitectureRulesNegativeTest` can deterministically prove that ArchUnit rejects `synchronized` methods. |

---

## 3. Resolved Historical Entries (No Longer Active)

The following transitional exceptions from `#399`–`#403` have been **resolved and removed from source code** (retained below for audit traceability without active `ALLOW-*` row prefixes):

| Finding | Scope (File Path) | Resolved Construct | Resolution Summary |
| :--- | :--- | :--- | :--- |
| `TS-001` (formerly `ALLOW-TS-001`) | `frontend/src/types/property.ts` | `CanonicalPropertyType \| string` | Unified `PropertyType` strictly to `CanonicalPropertyType` (`components['schemas']['PropertyType']`). |
| `TS-020` (formerly `ALLOW-TS-002`) | `frontend/src/api/client.ts` | `as unknown as T` | Replaced with `openapi-fetch` (`createClient<paths>`) and `unwrapApiResponse`. |
| `TS-022` (formerly `ALLOW-TS-003`) | `frontend/src/components/analytics/CityComparisonChart.tsx` | `as unknown as CityStat` | Replaced with runtime type guard `isCityStat(entry)`. |
| `TS-022` (formerly `ALLOW-TS-004`) | `frontend/src/components/analytics/PriceDistributionChart.tsx` | `as unknown as HistogramBin` | Replaced with runtime type guard `isHistogramBin(entry)`. |

---

## 4. Zero-Tolerance Banned Constructs (Never Allowlisted in Production)

The following constructs have **zero** active exceptions in production code and will immediately fail CI:

1. **Java `@SuppressWarnings` in `src/main/java/`**: 0 occurrences permitted (enforced by Checkstyle `noSuppressWarnings` & `validate-skills.sh`).
2. **Java Raw `Map<String, Object>`, `List<Object[]>`, or wildcard `ResponseEntity<?>` in Controllers & Repositories**: 0 occurrences permitted (enforced by ArchUnit `ArchitectureRulesTest.java`: `CONTROLLERS_DO_NOT_RETURN_OBJECT_MAPS_OR_WILDCARDS` and `REPOSITORIES_DO_NOT_RETURN_OBJECT_ARRAYS`).
3. **Java Stringly-Typed Closed Domain Enums in `model/` and `persistence/entity/`**: `PropertyType`, `ListingStatus`, `ListingObservation.StatusEvent`, `PortalId`, `PortalScrapeStatus`, `SearchExecutionStatus`, `ListingSortOption`, `CurrencyCode`, `QualityDeductionRule`, `SidecarStatus`, and `DedupOverrideType` must be strongly typed enums (`@Enumerated(EnumType.STRING)` on JPA entities such as `PropertyEntity`, `StatusLogEntity`, `ListingObservationEntity`, and `DedupOverrideEntity`), never `String` (enforced by ArchUnit `DOMAIN_AND_ENTITY_CLOSED_SETS_USE_ENUMS`).
4. **Java Reflective Class Loading (`Class.forName`) in `src/main/java/`**: 0 occurrences permitted (enforced by ArchUnit `NO_REFLECTIVE_CLASS_LOADING_IN_PRODUCTION` & Checkstyle `noReflectiveClassLoading`).
5. **TypeScript `@ts-ignore`, `@ts-nocheck`, `@ts-expect-error` in `frontend/src/` and `frontend/e2e/`**: 0 occurrences permitted (enforced by ESLint `@typescript-eslint/ban-ts-comment: error` & `validate-skills.sh`).
6. **TypeScript explicit `any` (`: any`, `as any`)**: 0 occurrences permitted (enforced by ESLint `@typescript-eslint/no-explicit-any: error`).
