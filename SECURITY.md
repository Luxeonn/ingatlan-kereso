# Security Policy

## Overview

The **ingatlan-kereso** project takes the security and integrity of its software, data, and users seriously. This document outlines the security policies, supported versions, vulnerability reporting guidelines, and application security architecture implemented across the repository.

---

## Supported Versions

Security updates and patches are actively maintained for the following versions and branches:

| Version / Branch | Supported          | Status |
| :--- | :--- | :--- |
| `develop` (latest development) | :white_check_mark: | Actively supported with security patches |
| `main` (production releases)   | :white_check_mark: | Actively supported with critical bug & security fixes |
| < 1.0.0 (legacy branches)      | :x:                | Unsupported; please upgrade to the latest branch |

---

## Reporting a Vulnerability

If you discover a security vulnerability or suspect an exploitable flaw in **ingatlan-kereso**, we appreciate your help in disclosing it to us responsibly.

> [!IMPORTANT]
> **DO NOT disclose vulnerabilities publicly.**
> Never open a public GitHub issue, discussion, or pull request describing an unpatched security vulnerability or zero-day exploit.

### Coordinated Disclosure Procedure

1. **GitHub Private Vulnerability Reporting (Recommended):**
   - Navigate to the repository's [Security Advisories](https://github.com/Luxeonn/ingatlan-kereso/security/advisories) tab.
   - Click **"Report a vulnerability"** to submit a private report.
2. **Direct Contact:**
   - If GitHub Private Reporting is unavailable, contact the project maintainer directly via GitHub profile: [@Luxeonn](https://github.com/Luxeonn).

### What to Include in Your Report

To help us triage and resolve the issue quickly, please provide:
- **Type of vulnerability** (e.g., SSRF, XSS, Path Traversal, CORS bypass, Injection).
- **Component affected** (e.g., Spring Boot REST API, Portal Scraper Engines, Frontend SPA, H2/JPA Storage).
- **Step-by-step reproduction instructions** or a minimal Proof of Concept (PoC).
- **Impact assessment** (what an attacker could achieve if the vulnerability is exploited).
- **Suggested remediation** (optional, but welcome).

---

## Response Timeline & SLA

We follow coordinated vulnerability disclosure practices and commit to the following response targets:

- **Initial Acknowledgment:** Within **48 hours** of receiving the report.
- **Triage & Severity Assessment:** Within **5 business days**, confirming reproduction and risk severity.
- **Patch Development & Testing:** Dependent on complexity; critical issues are prioritized with targeted fixes within **7–14 days**.
- **Public Disclosure:** Coordinated after the fix has been merged into `develop` and `main` and released to users.

---

## Application Security Architecture & Defenses

**ingatlan-kereso** is designed with defense-in-depth security principles across its 4-layer architecture:

### 1. Network & Server Hardening (Spring Boot 4.1.0 / Java 21)
- **Localhost Binding by Default:** The embedded server listens on `127.0.0.1` (`server.address=${SERVER_HOST:127.0.0.1}`) by default to prevent unintentional exposure to local area networks or public interfaces.
- **Strict CORS Policy (`GlobalCorsFilter`):** Only whitelisted origins (`http://localhost:*`, `http://127.0.0.1:*`) are permitted. Unauthorized preflight `OPTIONS` requests receive an immediate `403 Forbidden`.
- **Security HTTP Headers:** Every response is secured with:
  - `Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; img-src 'self' data: https:; connect-src 'self' http://localhost:* http://127.0.0.1:*;`
  - `X-Content-Type-Options: nosniff`
  - `X-Frame-Options: DENY`
  - `Referrer-Policy: strict-origin-when-cross-origin`
- **Actuator Removal & Attack Surface Minimization:** All Spring Boot Actuator endpoints and dependencies have been phased out to reduce runtime overhead and eliminate unneeded operational exposure in the integrated client-backend runtime.

### 2. Web Scraping & SSRF Defense (Scraper Layer)
- **Domain Whitelisting:** Scraper requests (`HttpFetcher`) only target explicit, predefined real estate portals (`dh.hu`, `oc.hu`, `jofogas.hu`, `koltozzbe.hu`, `ingatlanbazar.hu`, `ingatlan.com`). User inputs are never directly evaluated as arbitrary outbound fetch URLs.
- **Input Sanitization:** Search query parameters (city, price, area) are sanitized through strict regular expressions (`PropertyParser`) and diacritic normalizers (`SlugUtils`, `CityUtils`).
- **Resilience & DoS Mitigation:** All outbound HTTP connections enforce a strict 15-second timeout (`DEFAULT_TIMEOUT_MS = 15000`) and handle bot-protection / rate-limiting gracefully without crashing the server.

### 3. Persistence & Injection Defense (Spring Data JPA / H2)
- **Type-Safe Queries:** Data persistence relies on Spring Data JPA repositories and parameterized entity queries, preventing SQL and H2 injection vulnerabilities.
- **Local Embedded Isolation:** H2 database runs in embedded file mode (`./data/ingatlanok_jpa.mv.db`), isolated from remote network sockets.

### 4. Supply Chain & Continuous Integration (CI/CD)
- **Dependabot Automation:** Weekly automated vulnerability scans and updates for both Gradle (Java backend) and npm (React frontend) ecosystems.
- **Static Code Analysis:** Checkstyle and SpotBugs are integrated into CI builds (`./gradlew check`) to catch security and code quality defects prior to merge.
- **End-to-End Verification:** Playwright E2E UI action suites and offline JUnit 5 / MockMvc tests validate runtime integrity and error handling.

---

## Vulnerability Scope

### In-Scope
- Remote Code Execution (RCE)
- Server-Side Request Forgery (SSRF)
- Cross-Site Scripting (XSS)
- Path Traversal in static asset handling (`SpaWebMvcConfig`)
- CORS misconfigurations allowing arbitrary domain exploitation
- SQL / H2 Injection or data tampering
- Authentication/authorization bypasses on sensitive endpoints

### Out-of-Scope
- Third-party portal rate-limiting, Cloudflare CAPTCHAs, or WAF blocking
- Denial of Service (DoS) attacks targeting a locally bound desktop instance
- Theoretical vulnerabilities without a working Proof of Concept
- Vulnerabilities in outdated or unsupported dependencies that cannot be triggered in this application's execution path
