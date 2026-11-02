# PR Megfelelőségi és AI-Agent Governance Útmutató

Ez a dokumentum rögzíti az **ingatlan-kereso** projektben az AI-agentek és fejlesztők által benyújtott Pull Requestek minőségbiztosítási, visszakövethetőségi (traceability) és governance követelményeit.

---

## 1. Célkitűzés és Alapelvek

Az AI-agentek (pl. Google Antigravity, GitHub Copilot) által készített módosítások megbízhatóságát, visszakövethetőségét és a repository minőségi elvárásainak való megfelelését automatikus CI compliance kapuk garantálják.

A rendszer legfontosabb alapelvei:
1. **Bizonyíték-alapú verifikáció (Evidence-Based Verification):** A PR leírása önmagában nem elegendő; minden állítást futtatott tesztek, kódhivatkozások és diff-elemzés támaszt alá.
2. **Kockázat-vezérelt review kötelezettség:** Nem minden PR igényel minden szakterületi jóváhagyást; a módosított fájlok és a kockázat határozza meg, melyik specialista vizsgálata kötelező.
3. **Fail-Closed működés:** Bármely hiányzó kötelező mező, nem hitelesített státusz (`[NOT_VERIFIED]`) vagy felderített hiba (`[FINDING]`) azonnal sikertelenné teszi a compliance kaput.
4. **Traceability:** Az eredeti issue elfogadási kritériumai közvetlenül összekapcsolódnak az implementációval és a validációs bizonyítékkal.

---

## 2. A PR Leírás Kötelező Struktúrája (5 Szekció)

Minden Pull Request leírásnak (**Hungarian** nyelven, tiszta UTF-8 kódolással BOM nélkül) tartalmaznia kell az alábbi 5 szekciót a [pull request template](../.github/pull_request_template.md) alapján:

### 2.1. 🎯 Miért készült?
Tömör, 1–2 mondatos összefoglaló a változtatás üzleti vagy technikai céljáról és hátteréről.

### 2.2. 🛠️ Mit módosít / Mit javít?
Pontokba szedett összefoglaló a lényegi architektúrális és funkcionális változtatásokról.

### 2.3. 📋 Acceptance Criteria Traceability
Minden issue kritériumhoz egyértelműen hozzá kell rendelni a megvalósítást (fájlútvonalak) és az igazoló bizonyítékot (tesztesetek, parancsok, mérési eredmények). Nem releváns kritérium esetén kötelező az indoklás.

Formátum (Markdown táblázat vagy strukturált lista):
```markdown
| Issue Acceptance Criterion | Megvalósítás (Fájlok / Komponensek) | Igazoló teszt / Validációs bizonyíték |
| :--- | :--- | :--- |
| Kritérium 1 leírása | `src/.../MyService.java` | `MyServiceTest#testFeature` (JaCoCo: 88%) |
| Kritérium 2 leírása | `frontend/src/.../MyView.tsx` | `npm run test` (Vitest PASS) |
```

A Traceability táblázatnak igazolnia kell, hogy a diff-ben található módosítások szigorúan az issue scope-jához igazodnak. Unsolicited refaktorok, verzióemelések vagy nem kért szerződésmódosítások esetén a compliance kapu elutasítja a PR-t. Támogató jellegű kiegészítések csak explicit indoklással fogadhatók el.

### 2.4. 🧪 Tesztelés és Ellenőrzés
A helyben lefuttatott ellenőrző parancsok és azok kimeneti eredményei (pl. `./gradlew check`, `npm run lint`, `bash scripts/validate-skills.sh -v`).

### 2.5. 🛡️ Thematic Agent Approval Gate
A kijelölt szakterületi specialisták formális jóváhagyása az egységes döntési tokenekkel:
```markdown
- [x] **fullstack-architect**: [NO_FINDING] - Architektúra integritás és típusbiztonság igazolva.
- [x] **scraper-security-engineer**: [NOT_APPLICABLE] - Nem érint portál scraper vagy hálózati logikát.
- [x] **quality-devops-engineer**: [NO_FINDING] - Tesztlefedettség (>=80%), Checkstyle és CI/CD gate-ek teljesültek.
```

A leírás végén kötelező az issue összekapcsolása: `Closes #<id>` vagy `Fixes #<id>`.

---

## 3. Szabványos Döntési Státuszok (Decision Tokens)

A Thematic Gate blokkban kizárólag az alábbi 4 token megengedett:

| Token | Jelentés | CI Hatás | Elvárt tartalom |
| :--- | :--- | :--- | :--- |
| **`[NO_FINDING]`** | Sikeres ellenőrzés, hiba nélkül | **PASS** | 1 tömör mondatos indoklás az igazolt szabályokról |
| **`[NOT_APPLICABLE]`** | Nem érinti a specialista területét | **PASS** (ha a diff igazolja) | Kötelező indoklás, miért nem releváns a PR |
| **`[FINDING]`** | Szabálysértés vagy hiba | **FAIL (Blokkoló)** | Hibaleírás és javasolt javítás |
| **`[NOT_VERIFIED]`** | Nem ellenőrzött / nem futtatható | **FAIL (Blokkoló)** | Nem fogadható el kész munkaként |

> [!CAUTION]
> A korábbi alternatív vagy informális jelölések (pl. `[OK]`, `[NOK]`, `[PASS]`, `[N/A]`) **szigorúan tiltottak**; használatuk esetén a CI kapu azonnal elbukik.

---

## 4. Review-Kockázati Mátrix (Scope Ellenőrzés)

A PR-ben módosított fájlok listája alapján a CI compliance motor automatikusan ellenőrzi, hogy a kötelező specialisták jóváhagyása megtörtént-e. Ha egy adott terület fájljai módosultak, az ahhoz rendelt specialista **nem állhat** `[NOT_APPLICABLE]` státuszban:

| Módosítás típusa | Érintett fájlútvonalak (minták) | Kötelező specialista | Elvárt validáció |
| :--- | :--- | :--- | :--- |
| **Backend üzleti logika & modell** | `src/main/java/ingatlanok/core/**`<br>`src/main/java/ingatlanok/model/**` | `fullstack-architect`<br>`quality-devops-engineer` | Egység- és integrációs tesztek, JaCoCo $\ge 80\%$, Checkstyle Clean Code |
| **API & OpenAPI szerződés** | `openapi/**`<br>`src/main/java/ingatlanok/web/**`<br>`src/main/java/ingatlanok/presentation/**`<br>`frontend/src/types/generated-api.ts` | `fullstack-architect` | OpenAPI drift check (`npm run generate-api`), DTO immutabilitás |
| **DB & Perzisztencia** | `src/main/java/ingatlanok/persistence/**` | `fullstack-architect` | Spring Data JPA tesztek, perzisztencia réteghatár (nincs direct SQL a core-ban) |
| **Scraper, parser & hálózati I/O** | `src/main/java/ingatlanok/portal/**`<br>`src/main/java/ingatlanok/common/HttpFetcher.java`<br>`src/main/java/ingatlanok/common/PropertyParser.java`<br>`tools/scrapers/**`<br>`tools/sidecar*` | `scraper-security-engineer` | 0-crash védelem, ReDoS prevenció, SSRF izoláció (`HttpFetcher`), 15s timeout |
| **Frontend UI, állapot & kliens** | `frontend/**` (kivéve `generated-api.ts`) | `fullstack-architect` | TypeScript strict check (`noImplicitReturns`), ESLint (`--max-warnings 0`), Vitest, Playwright |
| **CI/CD, DevOps, Governance** | `.github/**`<br>`.agents/**`<br>`scripts/**`<br>`AGENTS.md`<br>`GEMINI.md`<br>`gemini.md`<br>`CONTRIBUTING.md`<br>`docs/**`<br>`Dockerfile*`<br>`docker-compose.yml`<br>`config/checkstyle/**` | `quality-devops-engineer` | `validate-skills.sh`, PR compliance unit tesztek, konténer security, minimális fail-closed CI kapu |
| **Architektúra & modulhatárok** | `package-info.java`<br>`build.gradle.kts` függőségek | `fullstack-architect` | ArchUnit tesztek (`ArchitectureRulesTest.java`), fail-closed javac (`-Werror`) |

---

## 5. Automatizált Ellenőrző Eszközök és Workflow-k

### 5.1. Lokális és CI Compliance Ellenőrző (`verify-pr-compliance.mjs`)
A repository tartalmazza a független, 0-függőségű compliance validátort:
```bash
# PR leírás és diff ellenőrzése
node scripts/verify-pr-compliance.mjs --body-file pr_body.md --diff-base origin/develop

# Vagy a bash wrapperrel:
bash scripts/verify-pr-compliance.sh --body-file pr_body.md --diff-base origin/develop

# Validátor önellenőrző és negatív tesztcsomag futtatása:
node --test scripts/test-verify-pr-compliance.mjs
```

### 5.2. Governance és Skill Validátor (`validate-skills.sh`)
Offline determinisztikus ellenőrző 7 ellenőrzési fázissal:
```bash
bash scripts/validate-skills.sh -v
```

### 5.3. GitHub Actions PR Compliance Workflow (`.github/workflows/pr-compliance.yml`)
Minden PR megnyitásakor, szerkesztésekor (`edited`) vagy új commit érkezésekor lefutó gate:
- Ellenőrzi a PR leírás 5 szekcióját és a Traceability táblázatot.
- Lekéri a PR-ben módosított fájlokat és ellenőrzi a Review-Kockázati Mátrixot.
- Futtatja a validátor negatív tesztjeit.
- Ha governance vagy dokumentációs fájlok változtak, automatikusan lefuttatja a `validate-skills.sh`-t.

---

## 6. GitHub Merge Gate & Enforcement Felmérés

A GitHub oldali védelem két szinten szerveződik:

### 6.1. Repositoryban Verziózott Workflow Kapuk (Mindig aktív)
- **`CI Build`** (`.github/workflows/ci.yml`): A központi fail-closed kapu, amely kötelezően megköveteli a backend teszteket (JaCoCo $\ge 80\%$), a frontend lint/teszt lépéseket, az OpenAPI drift ellenőrzést, a Gitleaks secret scant és a governance validációt.
- **`PR Compliance & Agent Governance`** (`.github/workflows/pr-compliance.yml`): A leírás- és governance compliance kapu.
- **`Branch up-to-date with develop`** (`.github/workflows/pr-guards.yml`): Biztosítja, hogy a PR ne legyen lemaradva az alapágtól.
- **`PR title`** (`.github/workflows/pr-build.yml`): Conventional Commit PR cím kényszerítése.

### 6.2. GitHub Repository Beállítások (Adminisztrátori Hatáskör)
A repositoryban jelenleg két aktív Ruleset van definiálva:
1. `develop-1` (ID: 25079337): Védi a `refs/heads/develop` ágat törlés (`deletion`), force-push (`non_fast_forward`) ellen, és automatikus Copilot review-t aktivál.
2. `Copilot review for default branch` (ID: 25079396): Védi az alapértelmezett ágat.

**Adminisztrátori jogosultsági korlátok és ajánlott beállítások:**
- A GitHub API-n és felületen a "Required status checks" beállítása a repository adminisztrátorának (`Luxeonn`) hatásköre.
- Ajánlott kötelező státuszellenőrzések (Required status checks) a GitHub Branch Protection vagy Ruleset felületén:
  - `CI Build`
  - `PR Compliance & Agent Governance`
  - `Branch up-to-date with develop`
  - `PR title`
  - `CodeQL`
- Mivel a repository-szintű workflow-k a PR-ek esetén automatikusan lefutnak és fail-closed módon kilépnek hiba esetén, a review-folyamat még adminisztrátori beavatkozás előtt is maximálisan védett.
