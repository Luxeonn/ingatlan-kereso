#!/usr/bin/env node
/**
 * scripts/verify-pr-compliance.mjs
 * Deterministic offline compliance validator for AI-agent and developer Pull Requests.
 *
 * Verifies:
 *  1. Required 2 Hungarian sections in PR description ('Miért készült?', 'Mit módosít / Mit javít?')
 *  2. Issue closing reference (Closes #... / Fixes #... / Resolves #...)
 *  3. Cross-Layer Presentation DTO & OpenAPI Contract Drift
 *
 * Exit codes:
 *  0: All compliance checks passed
 *  1: One or more violations detected (fail-closed)
 */

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';
import { execSync } from 'node:child_process';

const REQUIRED_SECTIONS = [
  { name: 'Miért készült', regex: /##\s*🎯\s*Miért készült\?/i },
  { name: 'Mit módosít', regex: /##\s*🛠️\s*Mit módosít\s*\/\s*Mit javít\?/i },
];

function printError(msg, isGh = false) {
  if (isGh || process.env.GITHUB_ACTIONS) {
    console.error(`::error::${msg}`);
  }
  console.error(`❌ [FAIL] ${msg}`);
}

function printPass(msg, verbose = false) {
  if (verbose) {
    console.log(`✅ [PASS] ${msg}`);
  }
}

/**
 * Strips HTML comments from markdown content.
 */
function stripHtmlComments(text) {
  let sanitized = text;
  let previous;
  do {
    previous = sanitized;
    sanitized = sanitized.replace(/<!--[\s\S]*?-->/g, '');
  } while (sanitized !== previous);
  return sanitized;
}

/**
 * Extracts the body of a markdown section by heading.
 */
function extractSectionContent(fullText, headingRegex) {
  const match = fullText.match(headingRegex);
  if (!match) return null;
  const startIndex = match.index + match[0].length;
  const rest = fullText.slice(startIndex);
  const nextHeading = rest.search(/\n##\s+/);
  const sectionRaw = nextHeading === -1 ? rest : rest.slice(0, nextHeading);
  return stripHtmlComments(sectionRaw).trim();
}

/**
 * Parses and verifies the PR body and modified files.
 */
export function verifyPrCompliance({
  bodyText,
  changedFiles = [],
}) {
  const errors = [];
  const passes = [];

  if (!bodyText || typeof bodyText !== 'string' || bodyText.trim().length === 0) {
    errors.push('PR leírás (body) hiányzik vagy teljesen üres.');
    return { valid: false, errors, passes };
  }

  // 1. Verify required sections
  for (const sec of REQUIRED_SECTIONS) {
    const content = extractSectionContent(bodyText, sec.regex);
    if (content === null) {
      errors.push(`Hiányzó kötelező szekció: '${sec.name}'`);
    } else if (content.length === 0) {
      errors.push(`Üres szekció: '${sec.name}' nem tartalmaz érdemi leírást (csak kommentet vagy semmit).`);
    } else {
      passes.push(`Szekció jelen van és nem üres: '${sec.name}'`);
    }
  }

  // 2. Verify Issue linking
  const issueLinkMatch = bodyText.match(/\b(Closes|Fixes|Resolves)\s+#(\d+)\b/i);
  if (!issueLinkMatch) {
    errors.push("Hiányzó issue összekapcsolás (pl. 'Closes #<id>' vagy 'Fixes #<id>').");
  } else {
    passes.push(`Issue kapcsolat rendben: ${issueLinkMatch[0]}`);
  }

  // 3. Verify Cross-Layer Presentation DTO & OpenAPI Contract Drift
  if (changedFiles.length > 0) {
    const dtoFiles = changedFiles.filter((f) => /^src\/main\/java\/ingatlanok\/presentation\/.*Dto\.java$/.test(f));
    if (dtoFiles.length > 0) {
      const touchesOpenApi = changedFiles.includes('openapi/api-spec.yaml');
      const touchesGeneratedApi = changedFiles.includes('frontend/src/types/generated-api.ts');
      const hasContractSync = touchesOpenApi && touchesGeneratedApi;

      const bodyHasContractExemption =
        /(?:OpenAPI|contract|szerz[őo]d[ée]s|generated-api).*?(?:nem sz[üu]ks[ée]ges|nem v[áa]ltozott|v[áa]ltozatlan|no change|nincs v[áa]ltoz[áa]s|nem ig[ée]nyel)/i.test(bodyText);

      if (!hasContractSync && !bodyHasContractExemption) {
        errors.push(
          `Cross-Layer Contract Drift: A PR módosította a prezentációs DTO-kat (${dtoFiles.join(', ')}), de az 'openapi/api-spec.yaml' vagy a 'frontend/src/types/generated-api.ts' nem szerepel a módosított fájlok között, és hiányzik az explicit indoklás a leírásból.`
        );
      } else if (hasContractSync) {
        passes.push('Cross-layer szerződés szinkronizáció rendben (DTO -> OpenAPI -> generated-api.ts)');
      } else {
        passes.push('Cross-layer szerződés mentesség dokumentálva a PR leírásban');
      }
    }
  }

  const valid = errors.length === 0;
  return { valid, errors, passes };
}

/**
 * Resolves list of changed files from args or git.
 */
function resolveChangedFiles(diffBase, changedFilesArg, changedFilesFile) {
  if (changedFilesArg) {
    return changedFilesArg.split(',').map((f) => f.trim()).filter(Boolean);
  }
  if (changedFilesFile && fs.existsSync(changedFilesFile)) {
    return fs.readFileSync(changedFilesFile, 'utf8').split('\n').map((l) => l.trim()).filter(Boolean);
  }
  if (diffBase) {
    try {
      const out = execSync(`git diff --name-only ${diffBase}...HEAD`, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
      return out.split('\n').map((l) => l.trim()).filter(Boolean);
    } catch {
      try {
        const out2 = execSync(`git diff --name-only ${diffBase}`, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] });
        return out2.split('\n').map((l) => l.trim()).filter(Boolean);
      } catch {
        return [];
      }
    }
  }
  return [];
}

// CLI entrypoint
if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const args = process.argv.slice(2);
  let bodyFile = null;
  let bodyText = null;
  let diffBase = null;
  let changedFilesArg = null;
  let changedFilesFile = null;
  let prNumber = null;
  let verbose = false;

  for (let i = 0; i < args.length; i++) {
    const a = args[i];
    if (a === '--body-file' && args[i + 1]) bodyFile = args[++i];
    else if (a === '--body' && args[i + 1]) bodyText = args[++i];
    else if (a === '--diff-base' && args[i + 1]) diffBase = args[++i];
    else if (a === '--changed-files' && args[i + 1]) changedFilesArg = args[++i];
    else if (a === '--changed-files-file' && args[i + 1]) changedFilesFile = args[++i];
    else if (a === '--pr' && args[i + 1]) prNumber = args[++i];
    else if (a === '-v' || a === '--verbose') verbose = true;
    else if (a === '-h' || a === '--help') {
      console.log('Használat: node scripts/verify-pr-compliance.mjs [opciók]');
      console.log('Opciók:');
      console.log('  --body-file <útvonal>      PR leírást tartalmazó fájl');
      console.log('  --body <szöveg>            PR leírás közvetlen szövegként');
      console.log('  --diff-base <ág|commit>    Git összehasonlítási alap a módosított fájlokhoz');
      console.log('  --changed-files <f1,f2>    Vesszővel elválasztott módosított fájllista');
      console.log('  --pr <szám>                PR adatok lekérése a gh CLI-vel');
      console.log('  -v, --verbose              Részletes kimenet');
      process.exit(0);
    }
  }

  if (prNumber) {
    try {
      const prJson = execSync(`gh pr view ${prNumber} --json body,files`, { encoding: 'utf8' });
      const prData = JSON.parse(prJson);
      bodyText = prData.body;
      if (Array.isArray(prData.files)) {
        changedFilesArg = prData.files.map((f) => f.path).join(',');
      }
    } catch (e) {
      printError(`Nem sikerült lekérni a PR #${prNumber} adatait a gh CLI-vel: ${e.message}`);
      process.exit(1);
    }
  } else if (bodyFile) {
    if (!fs.existsSync(bodyFile)) {
      printError(`A megadott body fájl nem létezik: ${bodyFile}`);
      process.exit(1);
    }
    bodyText = fs.readFileSync(bodyFile, 'utf8');
  }

  const changedFiles = resolveChangedFiles(diffBase, changedFilesArg, changedFilesFile);

  console.log('==============================================================================');
  console.log('          PULL REQUEST COMPLIANCE GATE                                        ');
  console.log('==============================================================================');
  if (changedFiles.length > 0) {
    console.log(`Módosított fájlok száma: ${changedFiles.length}`);
  }

  const result = verifyPrCompliance({ bodyText, changedFiles });

  if (verbose) {
    for (const p of result.passes) {
      printPass(p, true);
    }
  }

  if (!result.valid) {
    console.log('');
    for (const err of result.errors) {
      printError(err);
    }
    console.log('==============================================================================');
    console.log(`❌ COMPLIANCE GATE FAILED: ${result.errors.length} szabálysértés található.`);
    console.log('Lásd az irányelveket: docs/pr-compliance.md');
    console.log('==============================================================================');
    process.exit(1);
  } else {
    console.log('');
    console.log('==============================================================================');
    console.log('✅ COMPLIANCE GATE PASSED: A PR leírás megfelel.');
    console.log('==============================================================================');
    process.exit(0);
  }
}
