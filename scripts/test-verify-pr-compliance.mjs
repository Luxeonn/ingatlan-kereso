#!/usr/bin/env node
/**
 * scripts/test-verify-pr-compliance.mjs
 * Comprehensive unit test suite for PR compliance & agent governance checking.
 * Tests positive scenarios, negative boundary conditions, and Review-Risk Matrix enforcement.
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { verifyPrCompliance } from './verify-pr-compliance.mjs';

const VALID_SAMPLE_BODY = `
## 🎯 Miért készült?
Az AI-agent munkák és PR-ek egységes governance- és minőségellenőrzési kapuit vezetjük be.

## 🛠️ Mit módosít / Mit javít?
- Új PR compliance validátor és workflow bevezetése.
- Acceptance criteria traceability és kockázati mátrix dokumentálása.
- Verzió- és státuszellentmondások megszüntetése.

## 📋 Acceptance Criteria Traceability
| Issue Acceptance Criterion | Megvalósítás (Fájlok / Komponensek) | Igazoló teszt / Validációs bizonyíték |
| :--- | :--- | :--- |
| Egységes agent review státuszok | \`.github/pull_request_template.md\`, \`docs/pr-compliance.md\` | \`node scripts/verify-pr-compliance.mjs\` (PASS) |
| Review-kockázati mátrix | \`docs/architecture.md\`, \`scripts/verify-pr-compliance.mjs\` | \`node --test scripts/test-verify-pr-compliance.mjs\` |
| Negatív tesztek igazolása | \`scripts/test-verify-pr-compliance.mjs\` | 12 determinisztikus negatív teszteset sikeres |

## 🧪 Tesztelés és Ellenőrzés
- \`node --test scripts/test-verify-pr-compliance.mjs\` (Összes teszt lefutott).
- \`bash scripts/validate-skills.sh -v\` (Minden skill és link sikeres).

## 🛡️ Thematic Agent Approval Gate
- [x] **fullstack-architect**: [NO_FINDING] - Az architektúra, a DTO-k és a réteghatárok sértetlenek.
- [x] **scraper-security-engineer**: [NOT_APPLICABLE] - Nem érint külső hálózati scrapinget vagy portálokat.
- [x] **quality-devops-engineer**: [NO_FINDING] - CI/CD pipeline, tesztek és governance szabályok maradéktalanul érvényesülnek.

Closes #347
`;

test('valid PR body passes with flying colors', () => {
  const result = verifyPrCompliance({
    bodyText: VALID_SAMPLE_BODY,
    changedFiles: ['docs/pr-compliance.md', 'scripts/verify-pr-compliance.mjs'],
  });
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
});

test('valid PR body with legitimate NOT_APPLICABLE when domain files are untouched', () => {
  const result = verifyPrCompliance({
    bodyText: VALID_SAMPLE_BODY,
    changedFiles: ['docs/architecture.md', 'docs/pr-compliance.md'],
  });
  assert.equal(result.valid, true);
});

test('missing required section triggers fail-closed error', () => {
  const brokenBody = VALID_SAMPLE_BODY.replace(/##\s*📋\s*Acceptance Criteria Traceability[\s\S]*?##\s*🧪/, '## 🧪');
  const result = verifyPrCompliance({ bodyText: brokenBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes("Hiányzó kötelező szekció: 'Acceptance Criteria Traceability'")));
});

test('empty section with only HTML comments triggers error', () => {
  const brokenBody = VALID_SAMPLE_BODY.replace(
    /##\s*🎯\s*Miért készült\?[\s\S]*?##\s*🛠️/,
    '## 🎯 Miért készült?\n<!-- Csak egy html komment áll itt -->\n\n## 🛠️'
  );
  const result = verifyPrCompliance({ bodyText: brokenBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes("Üres szekció: 'Miért készült'")));
});

test('traceability table containing only template placeholders fails', () => {
  const placeholderBody = VALID_SAMPLE_BODY.replace(
    /\| Egységes agent review[\s\S]*?sikeres \|/i,
    '| [Kritérium 1 leírása] | `path/to/file` | `teszt vagy ellenőrző parancs` |'
  );
  const result = verifyPrCompliance({ bodyText: placeholderBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('helykitöltő adatokat tartalmaz')));
});

test('missing issue linking keyword (Closes / Fixes) fails', () => {
  const brokenBody = VALID_SAMPLE_BODY.replace(/Closes #347/, '');
  const result = verifyPrCompliance({ bodyText: brokenBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('Hiányzó issue összekapcsolás')));
});

test('legacy/forbidden status token [OK] triggers failure', () => {
  const legacyBody = VALID_SAMPLE_BODY.replace(/\[NO_FINDING\]/, '[OK]');
  const result = verifyPrCompliance({ bodyText: legacyBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('Tiltott/elavult státuszjelölés')));
});

test('status token [NOT_VERIFIED] blocks the compliance gate', () => {
  const unverifiedBody = VALID_SAMPLE_BODY.replace(
    /fullstack-architect\*\*:\s*\[NO_FINDING\]/,
    'fullstack-architect**: [NOT_VERIFIED]'
  );
  const result = verifyPrCompliance({ bodyText: unverifiedBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('[NOT_VERIFIED]; nem hitelesített review')));
});

test('status token [FINDING] blocks the compliance gate', () => {
  const findingBody = VALID_SAMPLE_BODY.replace(
    /quality-devops-engineer\*\*:\s*\[NO_FINDING\]/,
    'quality-devops-engineer**: [FINDING] - Coverage 75% alatt maradt.'
  );
  const result = verifyPrCompliance({ bodyText: findingBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('[FINDING]; a gate blokkolva van')));
});

test('unchecked checkbox in approval gate blocks the gate', () => {
  const uncheckedBody = VALID_SAMPLE_BODY.replace(
    /- \[x\] \*\*fullstack-architect\*\*/,
    '- [ ] **fullstack-architect**'
  );
  const result = verifyPrCompliance({ bodyText: uncheckedBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('nincs készre jelölve')));
});

test('[NOT_APPLICABLE] without non-empty rationale fails', () => {
  const emptyRationaleBody = VALID_SAMPLE_BODY.replace(
    /scraper-security-engineer\*\*:\s*\[NOT_APPLICABLE\]\s*-\s*[^\n]+/,
    'scraper-security-engineer**: [NOT_APPLICABLE] - Indoklás.'
  );
  const result = verifyPrCompliance({ bodyText: emptyRationaleBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('hiányzik az indoklás')));
});

test('Review Risk Matrix: scraper-security-engineer cannot be NOT_APPLICABLE when scraper files are modified', () => {
  const result = verifyPrCompliance({
    bodyText: VALID_SAMPLE_BODY,
    changedFiles: ['src/main/java/ingatlanok/portal/dh/DunaHousePortalEngine.java'],
  });
  assert.equal(result.valid, false);
  assert.ok(
    result.errors.some((e) =>
      e.includes("Review-Kockázati Mátrix sértés: 'scraper-security-engineer' [NOT_APPLICABLE]-ként szerepel")
    )
  );
});

test('Review Risk Matrix: fullstack-architect cannot be NOT_APPLICABLE when backend core files are modified', () => {
  const bodyWithNaFullstack = VALID_SAMPLE_BODY.replace(
    /fullstack-architect\*\*:\s*\[NO_FINDING\]\s*-\s*[^\n]+/,
    'fullstack-architect**: [NOT_APPLICABLE] - Csak tesztek változtak.'
  );
  const result = verifyPrCompliance({
    bodyText: bodyWithNaFullstack,
    changedFiles: ['src/main/java/ingatlanok/core/SearchExecutionEngine.java'],
  });
  assert.equal(result.valid, false);
  assert.ok(
    result.errors.some((e) =>
      e.includes("Review-Kockázati Mátrix sértés: 'fullstack-architect' [NOT_APPLICABLE]-ként szerepel")
    )
  );
});

test('Review Risk Matrix: quality-devops-engineer cannot be NOT_APPLICABLE when CI workflow is modified', () => {
  const bodyWithNaDevops = VALID_SAMPLE_BODY.replace(
    /quality-devops-engineer\*\*:\s*\[NO_FINDING\]\s*-\s*[^\n]+/,
    'quality-devops-engineer**: [NOT_APPLICABLE] - Nem érint CI-t.'
  );
  const result = verifyPrCompliance({
    bodyText: bodyWithNaDevops,
    changedFiles: ['.github/workflows/ci.yml'],
  });
  assert.equal(result.valid, false);
  assert.ok(
    result.errors.some((e) =>
      e.includes("Review-Kockázati Mátrix sértés: 'quality-devops-engineer' [NOT_APPLICABLE]-ként szerepel")
    )
  );
});
