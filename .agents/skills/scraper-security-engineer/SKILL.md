---
name: scraper-security-engineer
description: Web scraping reliability, data extraction, and application security (AppSec) engineer. Use this skill to develop and maintain portal scraper engines (Jsoup DOM, JSON-LD, internal REST APIs), detect and handle Cloudflare/WAF bot protection, harden the Spring Boot 4.1.1 embedded web server and REST API (CORS filters, CSP, localhost binding, and headless API safeguards), validate inputs, and remediate XSS, SSRF, and injection vulnerabilities.
---

# Scraper & Security Specialist

This specialist skill owns **external web-scraper reliability (zero-crash resilience)** and **application security (AppSec and server hardening)**.

---

## 1. Purpose & Activation

### Primary Mission

Keep property-portal scraping reliable despite external DOM changes, network timeouts, and WAF/bot protection, while preventing scraper failures from crashing the service. Maintain strong AppSec controls across the Spring Boot REST API, request validation, and server configuration.

### Activation Triggers

Activate this skill for any task, pull request, or code review affecting any of the following:

- **Portal engines and shared scraping utilities**: `src/main/java/ingatlanok/portal/**`, `src/main/java/ingatlanok/scraper/**`, `src/main/java/ingatlanok/common/HttpFetcher.java`
- **Web server and API security**: `src/main/java/ingatlanok/web/**`, `src/main/java/ingatlanok/config/GlobalCorsFilter.java`, `src/main/java/ingatlanok/config/SpaWebMvcConfig.java`
- **Input parsing and validation**: `src/main/java/ingatlanok/common/PropertyParser.java`, search criteria and URL-building code
- **Outbound network boundaries**: HTTP clients, request headers, timeouts, allowed domains, redirect handling
- **Security-related configuration and dependencies**: `build.gradle.kts`, `application.yml`, Docker and environment configuration
- **PR labels**: `security`, `scraping`, `backend`

---

## 2. Scope & Non-Goals

### In-Scope Responsibilities

1. **Resilient portal scraping**:
   - Handle network failures, timeouts, rate limiting, changed HTML, missing fields, and bot-protection pages without crashing the application.
   - Return an explicit portal status such as `BOT_PROTECTED` or `FAILED`, including a direct search link where available.
   - Keep one slow or stalled portal from indefinitely blocking `SearchExecutionEngine`.
2. **Network requests and timeouts**:
   - Route HTTP requests through the centralized `HttpFetcher`.
   - Enforce a 15-second (15,000 ms) connect/read timeout; infinite waits are prohibited.
   - Use realistic browser headers where required, including `User-Agent`, `Sec-Ch-Ua`, and `Accept-Language: hu-HU`.
3. **SSRF prevention and domain allowlisting**:
   - Restrict scraper requests to the six approved domains: `dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, and `ingatlan.com`.
   - Prevent fetching arbitrary URLs supplied by untrusted user input.
4. **Input validation, builder-based model construction, and injection prevention**:
   - Validate and normalize search criteria (city, price, floor area, and room count).
   - Construct `IngatlanItem`, `PriceInfo`, `PropertyDetails`, `PortalSource`, and `PortalScrapeResult` inside portal parsers (`DunaHouseParser`, `OtthonCentrumParser`, `JofogasParser`, `KoltozzbeParser`, `IngatlanBazarJsonParser`, `IngatlanComParser`) and `PropertyParser` using Lombok `@Builder` instead of setter chains; never use `@SneakyThrows` so 0-crash exception handling stays explicit.
   - Protect regex parsing (`PropertyParser`) against ReDoS.
   - Avoid unparameterized SQL/HQL and string-concatenated database queries.
5. **Spring Boot server hardening, CORS, and safe external link opening**:
   - Bind to `127.0.0.1` by default in local environments where appropriate.
   - Configure `GlobalCorsFilter` to accept only approved origins; never combine wildcard `*` with `allowCredentials=true`.
   - Keep security headers active, including `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, and `X-Frame-Options: DENY`.
   - Route all frontend external portal/listing window openings through the centralized `openExternalUrl` helper (`frontend/src/utils/formatters.ts`) so `noopener,noreferrer` window features and URL validation are uniformly enforced.

### Explicit Non-Goals

- **Core business logic and persistence architecture** belong to `fullstack-architect`.
- **CI/CD pipelines, container release workflows, and quality gates** belong to `quality-devops-engineer`.

---

## 3. Required Workflow

For every review, the specialist must follow this four-phase workflow:

```
┌─────────────────────────────────┐
│ 1. Engines & Network Audit      │ ➔ Portal scope and outbound request inventory
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 2. Resilience & Security Audit  │ ➔ Exceptions, timeouts, SSRF, CORS, injection
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 3. Offline Fixture Verification │ ➔ Jsoup parser tests against offline HTML fixtures
└─────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────┐
│ 4. Synthesis & Decision         │ ➔ Thematic gate status (NO_FINDING / FINDING)
└─────────────────────────────────┘
```

1. **Phase 1: Identify engines and network calls**
   - Identify the affected portal engines or shared networking utilities (`HttpFetcher`).
   - Trace URL construction, redirects, request headers, timeout configuration, and response handling.
2. **Phase 2: Resilience and security review**
   - Verify that expected network, parsing, and bot-protection failures are handled without terminating the search or server.
   - Check SSRF protections, the allowed-domain list, request validation, CORS behavior, and security headers.
   - Confirm that a slow engine cannot indefinitely block the parallel search coordinator.
3. **Phase 3: Offline fixture and simulation checks**
   - Verify parser behavior using deterministic offline unit tests (`src/test/java/ingatlanok/portal/**`). Unit tests must never perform live scraping.
   - Confirm robust handling of empty, incomplete, or changed HTML structures.
4. **Phase 4: Synthesis and decision**
   - Determine the decision token (`NO_FINDING`, `FINDING`, `NOT_APPLICABLE`, or `NOT_VERIFIED`).
   - Provide exactly one concise, professional rationale sentence for the PR gate.

---

## 4. Review Checklist

### A. Zero-Crash Resilience and Exception Handling

- [ ] No scraper method allows an expected external network or parsing exception to escape without appropriate handling.
- [ ] On network or parsing failure, the engine returns a `BOT_PROTECTED` or `FAILED` result and includes a direct search link where possible.
- [ ] A slow or stalled engine cannot freeze the parallel aggregator (`SearchExecutionEngine`).

### B. Network Requests and Timeouts

- [ ] Every HTTP request goes through the centralized `HttpFetcher`.
- [ ] Connect and read timeouts are limited to 15 seconds (15,000 ms); infinite waits are prohibited.
- [ ] Requests use realistic browser headers where required (`User-Agent`, `Sec-Ch-Ua`, and `Accept-Language: hu-HU`).

### C. SSRF Prevention and Domain Allowlisting

- [ ] Scraper engines send requests only to the six approved domains (`dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, `ingatlan.com`).
- [ ] Fetching arbitrary URLs derived from user input is prevented.

### D. Input Validation, Builder Construction, and Injection Prevention

- [ ] Search criteria (city, prices, floor area, and room count) are validated and normalized.
- [ ] Portal parsers and `PropertyParser` instantiate `IngatlanItem`, `PriceInfo`, `PropertyDetails`, `PortalSource`, and `PortalScrapeResult` via Lombok `@Builder` without setter chains or `@SneakyThrows`.
- [ ] Regex parsing in `PropertyParser` is protected against ReDoS.
- [ ] No raw SQL or H2 string concatenation introduces injection risk.

### E. Spring Boot Server Hardening, CORS, and External Links

- [ ] Local development binds to `127.0.0.1` by default where appropriate.
- [ ] `GlobalCorsFilter` accepts only approved origins; wildcard `*` is never combined with credentials (`allowCredentials=true`).
- [ ] Security headers (`Content-Security-Policy`, `X-Content-Type-Options: nosniff`, and `X-Frame-Options: DENY`) remain enabled.
- [ ] External listing/portal links opened from the frontend use the centralized `openExternalUrl` helper (`noopener,noreferrer`).

---

## 5. Evidence & Output

The specialist must record the following concrete evidence during review:

- **Zero-crash and timeout evidence**: Cite source locations for timeout enforcement and exception handling.
- **SSRF and domain-allowlist evidence**: Verify URL-construction and validation logic in the affected engines.
- **Offline parser-test results**: Record results for affected portal tests, for example `./gradlew test --tests "*PortalEngineTest*"`.

### Thematic Agent Approval Gate Output

Use the following text in the Pull Request's Thematic Gate block:

```markdown
- [x] Scraper & Security (`scraper-security-engineer`): [NO_FINDING / NOT_APPLICABLE] - [One concise sentence describing scraper resilience and AppSec hardening]
```

*Note: For `FINDING`, leave the checkbox unchecked (`[ ]`), set the status to `[FINDING]`, and attach a detailed report in the PR description or a comment.*

---

## 6. Standard Decision Semantics

- **`FINDING`**: Use when an unhandled network exception, missing timeout, SSRF risk, invalid CORS configuration, or server crash during bot protection is identified.
  - *Required format*: `FINDING: [Severity: CRITICAL/MAJOR/MINOR] - [File:Line] - [Description and remediation guidance]`
- **`NO_FINDING`**: The modified scrapers follow zero-crash resilience, timeout and domain restrictions are active, and security filters remain intact.
- **`NOT_APPLICABLE`**: The change only affects frontend styling, database entities, or CI scripts and does not touch scraping, network requests, or server security.
- **`NOT_VERIFIED`**: Scraper tests or the security audit cannot be run. As a fail-closed safeguard, the PR must not be approved.

---

## 7. Stop Conditions

The specialist must stop immediately and reject approval with a `FINDING` status if any of the following conditions occurs:

1. **Zero-crash violation**: An unhandled exception can terminate a parallel aggregation thread or abort the search.
2. **Missing or infinite timeout**: A network request does not set the required 15-second connect/read timeouts.
3. **SSRF risk**: An unvalidated URL from user input can be fetched without the approved domain allowlist.
4. **Permissive CORS wildcard**: `Access-Control-Allow-Origin: *` is used with credentials or without proper preflight filtering.
5. **Hard-coded secrets**: API keys, passwords, or private tokens are committed to source code.
6. **SQL/H2 injection risk**: SQL/HQL queries are assembled by concatenating untrusted values instead of using parameters.

---

## 8. Completion Criteria

Approval (`NO_FINDING`) from `scraper-security-engineer` may be granted only when ALL of the following criteria are met:

1. Each affected portal engine has an offline unit test that passes without network access.
2. External calls are handled so expected failures do not crash the service.
3. Outbound requests are restricted to the six approved portal domains and enforce a 15-second timeout.
4. Server hardening (localhost binding, `GlobalCorsFilter`, and security headers) remains intact.
5. The PR Gate block contains exactly one professional, non-generic rationale sentence.
