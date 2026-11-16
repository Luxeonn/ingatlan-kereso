#!/usr/bin/env node
/**
 * scripts/test-verify-pr-compliance.mjs
 * Unit test suite for PR compliance checking.
 * Tests required sections, issue linking, and Cross-Layer Contract Drift enforcement.
 */

import test from 'node:test';
import assert from 'node:assert/strict';
import { verifyPrCompliance } from './verify-pr-compliance.mjs';

const VALID_SAMPLE_BODY = `
## 🎯 Miért készült?
Az AI-agent munkák és PR-ek egységes minőségellenőrzési kapuit vezetjük be.

## 🛠️ Mit módosít / Mit javít?
- Új PR compliance validátor és workflow bevezetése.
- Egyszerűsített PR sablon a legfontosabb szekciókkal.

Closes #347
`;

test('valid PR body passes', () => {
  const result = verifyPrCompliance({
    bodyText: VALID_SAMPLE_BODY,
    changedFiles: ['docs/pr-compliance.md', 'scripts/verify-pr-compliance.mjs'],
  });
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
});

test('missing required section triggers fail-closed error', () => {
  const brokenBody = VALID_SAMPLE_BODY.replace(/##\s*🛠️\s*Mit módosít[\s\S]*?Closes #347/, 'Closes #347');
  const result = verifyPrCompliance({ bodyText: brokenBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes("Hiányzó kötelező szekció: 'Mit módosít'")));
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

test('missing issue linking keyword (Closes / Fixes) fails', () => {
  const brokenBody = VALID_SAMPLE_BODY.replace(/Closes #347/, '');
  const result = verifyPrCompliance({ bodyText: brokenBody });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('Hiányzó issue összekapcsolás')));
});

test('Cross-Layer Contract Drift: presentation DTO modified without openapi/api-spec.yaml and generated-api.ts fails', () => {
  const result = verifyPrCompliance({
    bodyText: VALID_SAMPLE_BODY,
    changedFiles: [
      'src/main/java/ingatlanok/presentation/DashboardResponseDto.java',
      'src/main/java/ingatlanok/presentation/DashboardPresentationService.java',
    ],
  });
  assert.equal(result.valid, false);
  assert.ok(
    result.errors.some((e) =>
      e.includes('Cross-Layer Contract Drift: A PR módosította a prezentációs DTO-kat')
    )
  );
});

test('Cross-Layer Contract Drift: presentation DTO modified with openapi/api-spec.yaml and generated-api.ts passes', () => {
  const result = verifyPrCompliance({
    bodyText: VALID_SAMPLE_BODY,
    changedFiles: [
      'src/main/java/ingatlanok/presentation/DashboardResponseDto.java',
      'openapi/api-spec.yaml',
      'frontend/src/types/generated-api.ts',
    ],
  });
  assert.equal(result.valid, true);
  assert.ok(
    result.passes.some((p) =>
      p.includes('Cross-layer szerződés szinkronizáció rendben')
    )
  );
});

test('Cross-Layer Contract Drift: presentation DTO modified with explicit exemption in PR body passes', () => {
  const bodyWithExemption = VALID_SAMPLE_BODY + '\nMegjegyzés: belső DTO refaktorálás történt, OpenAPI szerződés nem szükséges / változatlan.\n';
  const result = verifyPrCompliance({
    bodyText: bodyWithExemption,
    changedFiles: [
      'src/main/java/ingatlanok/presentation/PortalStatusDto.java',
    ],
  });
  assert.equal(result.valid, true);
  assert.ok(
    result.passes.some((p) =>
      p.includes('Cross-layer szerződés mentesség dokumentálva')
    )
  );
});

test('dependabot[bot] PR passes without Hungarian sections or issue link', () => {
  const result = verifyPrCompliance({
    bodyText: 'Bumps foo from 1.0.0 to 1.1.0.',
    changedFiles: ['build.gradle.kts'],
    author: 'dependabot[bot]',
  });
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
  assert.ok(result.passes.some((p) => p.includes('dependabot[bot]')));
});

test('dependabot[bot] PR still fails if presentation DTO changes without OpenAPI sync', () => {
  const result = verifyPrCompliance({
    bodyText: 'Bumps foo from 1.0.0 to 1.1.0.',
    changedFiles: ['src/main/java/ingatlanok/presentation/DashboardResponseDto.java'],
    author: 'dependabot[bot]',
  });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('Cross-Layer Contract Drift')));
});

test('develop -> main release PR passes without issue link when Hungarian sections are present', () => {
  const releaseBody = VALID_SAMPLE_BODY.replace(/Closes #347/, '');
  const result = verifyPrCompliance({
    bodyText: releaseBody,
    changedFiles: ['README.md'],
    baseRef: 'main',
    headRef: 'develop',
  });
  assert.equal(result.valid, true);
  assert.equal(result.errors.length, 0);
  assert.ok(result.passes.some((p) => p.includes('Release PR (develop -> main)')));
});

test('develop -> main release PR still fails if a required Hungarian section is missing', () => {
  const brokenReleaseBody = VALID_SAMPLE_BODY
    .replace(/##\s*🛠️\s*Mit módosít[\s\S]*?Closes #347/, '')
    .trim();
  const result = verifyPrCompliance({
    bodyText: brokenReleaseBody,
    baseRef: 'main',
    headRef: 'develop',
  });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes("Hiányzó kötelező szekció: 'Mit módosít'")));
});

test('non-release PR targeting main still requires issue link', () => {
  const hotfixBody = VALID_SAMPLE_BODY.replace(/Closes #347/, '');
  const result = verifyPrCompliance({
    bodyText: hotfixBody,
    baseRef: 'main',
    headRef: 'fix/urgent-hotfix',
  });
  assert.equal(result.valid, false);
  assert.ok(result.errors.some((e) => e.includes('Hiányzó issue összekapcsolás')));
});
