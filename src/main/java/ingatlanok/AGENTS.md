# Backend Type-Safety & Architecture Directives (`src/main/java/ingatlanok/`)

This module-specific guide supplements the root [AGENTS.md](../../../../AGENTS.md), [docs/architecture.md](../../../../docs/architecture.md), and the canonical fullstack type-safety audit ([docs/architecture/type-safety/audit.md](../../../../docs/architecture/type-safety/audit.md)). Every AI agent or developer modifying `src/main/java/ingatlanok/` or `src/test/java/ingatlanok/` must adhere strictly to these rules.

---

## 1. Mandatory Type-Safety & Domain Modeling Principles (Java 25 LTS)

1. **Semantic Domain Types Over Primitive Obsession:**
   - Model closed domain value sets with explicit Java `enum` types (`PropertyType`, `ListingStatus`, `PortalId`, `PortalScrapeStatus`, `SearchExecutionStatus`, `ListingSortOption`, `CurrencyCode`, `QualityDeductionRule`, `ListingObservation.StatusEvent`, `SidecarStatus`) rather than raw `String` constants (`JAVA-001`–`JAVA-010`).
   - Persist enum fields on JPA entities (`PropertyEntity`, `StatusLogEntity`, `ListingObservationEntity`) with `@Enumerated(EnumType.STRING)` so database rows and Java models stay strongly typed (`JAVA-001`, `JAVA-008`).
   - Model bounded or unit-bearing domain concepts with immutable value records (`Money`, `Area`, `RoomCount`, `QualityScore`, `CanonicalId`, `PropertyId`, `MarketSegmentKey`, `QualityDeduction`, `TrackedCityInfo`, `PredefinedCity`) that validate invariants in their compact constructors (`JAVA-012`–`JAVA-016`).
   - Provide case-insensitive Jackson `@JsonCreator(mode = JsonCreator.Mode.DELEGATING)` factories on domain enums to preserve backward compatibility with legacy lowercase slugs and external portal inputs without widening internal domain types.
2. **Typed REST Contracts & Repository Projections:**
   - Every `@RestController` and `@RestControllerAdvice` endpoint in `web/` must declare strongly typed request/response DTO records (`StatusResponseDto`, `CitiesResponseDto`, `ApiErrorResponse`, `PropertyHideResponseDto`, `MarkViewedResponseDto`, `OpenUrlsResponseDto`, `ClearHistoryResponseDto`, `ActionSuccessResponseDto`, `HistoryQueryDto`).
   - Spring Data JPA aggregate queries in `persistence/repository/` must return typed record projections (`PortalCountProjection`, `CityCountProjection`), never raw `List<Object[]>` (`JAVA-021`).
   - Synchronize any presentation or web DTO change immediately with `openapi/api-spec.yaml` and `frontend/src/types/generated-api.ts` (enforced by `OpenApiSpecIntegrityTest.java` and `scripts/verify-pr-compliance.mjs`).
3. **Lombok, MapStruct & Immutability:**
   - Use `@Getter`, `@Builder` (`toBuilder = true`), and `@NoArgsConstructor` (`@Setter` only where post-construction mutation is required). Omit `@Setter` on read-only DTOs and value objects (`READ_ONLY_DTOS_MUST_NOT_HAVE_SETTERS` in `ArchitectureRulesTest.java`).
   - Use MapStruct (`JpaEntityMapper`, `PresentationMapper`) with `componentModel = "spring"` and `unmappedTargetPolicy = ReportingPolicy.ERROR`.
4. **Four-Layer Unidirectional Architecture, SSRF & Virtual Threads:**
   - Respect the strict downward dependency flow (`web` -> `presentation` -> `core` -> `persistence` / `portal`).
   - Route all outbound scraping HTTP calls exclusively through `HttpFetcher.java`.
   - Never use `synchronized` methods or blocks anywhere in `src/main/java/ingatlanok/` (prevents Java 25 virtual thread carrier pinning).

---

## 2. Strictly Forbidden Constructs & Bypasses (CI-Blocked)

The following constructs fail javac (`-Werror`), Checkstyle (`./gradlew checkstyleMain checkstyleTest`), ArchUnit (`ArchitectureRulesTest.java`), or `scripts/validate-skills.sh`:

- **Compiler Warning Suppressions (`@SuppressWarnings`):** Forbidden in `src/main/java/` by Checkstyle (`noSuppressWarnings`) and `scripts/validate-skills.sh`. Fix raw types, unchecked casts, and deprecations instead of suppressing them.
- **Raw Types, Unchecked Casts & Compiler Warnings:** Blocked by javac `-Werror` with `-Xlint:unchecked,deprecation,rawtypes,fallthrough,cast,overrides,divzero,empty`.
- **Untyped `Map<String, Object>` or Wildcard `ResponseEntity<?>` in Controllers/Storage:** Blocked by ArchUnit (`CONTROLLERS_DO_NOT_RETURN_RAW_OR_OBJECT_MAPS`, `STORAGE_INTERFACES_DO_NOT_EXPOSE_OBJECT_MAPS`).
- **Raw `Object[]` Repository Queries:** Blocked by ArchUnit (`REPOSITORIES_DO_NOT_RETURN_OBJECT_ARRAYS`).
- **Reflective Class Loading (`Class.forName`) in Production Code:** Blocked by ArchUnit (`NO_REFLECTIVE_CLASS_LOADING_IN_PRODUCTION`) and Checkstyle (`JAVA-026`).
- **Stringly-Typed Closed Domain Status/Type Fields:** Blocked by ArchUnit (`DOMAIN_AND_ENTITY_CLOSED_SETS_USE_ENUMS`).
- **Forbidden Lombok Annotations (`@Data`, `@EqualsAndHashCode`, `@ToString`, `@SneakyThrows`):** Blocked by Checkstyle and `scripts/validate-skills.sh`.

---

## 3. Non-Tampering Rule & Local Verification

AI agents **must never** remove, weaken, or add broad exclusions to `build.gradle.kts`, `config/checkstyle/checkstyle.xml`, `ArchitectureRulesTest.java`, or `docs/architecture/type-safety/allowlist.md`. All rule files are protected by [.github/CODEOWNERS](../../../../.github/CODEOWNERS).

Before committing or pushing any backend change, run:
```bash
./gradlew checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification check bootJar
```
