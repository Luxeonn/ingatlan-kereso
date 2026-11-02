#!/usr/bin/env node
/**
 * scripts/verify-pr-compliance.mjs
 * Deterministic offline compliance validator for AI-agent and developer Pull Requests.
 *
 * Verifies:
 *  1. Required 5 Hungarian sections in PR description
 *  2. Acceptance criteria traceability mapping (issue criteria -> implementation -> verification)
 *  3. Issue closing reference (Closes #... / Fixes #...)
 *  4. Standard Thematic Agent Approval Gate decision tokens (NO_FINDING, NOT_APPLICABLE)
 *  5. Prohibition of legacy/forbidden tokens ([OK], [NOK], [PASS], [N/A])
 *  6. Fail-closed rejection of [NOT_VERIFIED] and [FINDING]
 *  7. Non-empty rationale for [NOT_APPLICABLE]
 *  8. Review-Kockázati Mátrix alignment against actual modified files
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

const SPECIALISTS = [
  {
    id: 'fullstack-architect',
    title: 'Fullstack Architecture & UI',
    patterns: [
      /^src\/main\/java\/ingatlanok\/(core|model|persistence|presentation|web|config)\//,
      /^openapi\//,
      /^frontend\/(src|public|index\.html|package\.json|vite\.config|eslint\.config)/,
      /^build\.gradle\.kts$/,
      /^settings\.gradle\.kts$/,
    ],
  },
  {
    id: 'scraper-security-engineer',
    title: 'Scraper & Security',
    patterns: [
      /^src\/main\/java\/ingatlanok\/portal\//,
      /^src\/main\/java\/ingatlanok\/common\/HttpFetcher\.java$/,
      /^src\/main\/java\/ingatlanok\/common\/PropertyParser\.java$/,
      /^tools\/scrapers\//,
      /^tools\/sidecar/,
    ],
  },
  {
    id: 'quality-devops-engineer',
    title: 'Quality & DevOps',
    patterns: [
      /^\.github\//,
      /^\.agents\//,
      /^scripts\//,
      /^config\/checkstyle\//,
      /^src\/test\//,
      /^frontend\/(src\/test|e2e)\//,
      /^Dockerfile/,
      /^docker-compose\.yml$/,
      /^AGENTS\.md$/,
      /^GEMINI\.md$/,
      /^gemini\.md$/,
      /^CONTRIBUTING\.md$/,
      /^docs\//,
      /^SECURITY\.md$/,
    ],
  },
];

const REQUIRED_SECTIONS = [
  { name: 'Miért készült', regex: /##\s*🎯\s*Miért készült\?/i },
  { name: 'Mit módosít', regex: /##\s*🛠️\s*Mit módosít\s*\/\s*Mit javít\?/i },
  { name: 'Acceptance Criteria Traceability', regex: /##\s*📋\s*Acceptance Criteria Traceability/i },
  { name: 'Tesztelés és Ellenőrzés', regex: /##\s*🧪\s*Tesztelés és Ellenőrzés/i },
  { name: 'Thematic Agent Approval Gate', regex: /##\s*🛡️\s*Thematic Agent Approval Gate/i },
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
  verbose = false,
}) {
  const errors = [];
  const passes = [];

  if (!bodyText || typeof bodyText !== 'string' || bodyText.trim().length === 0) {
    errors.push('PR leírás (body) hiányzik vagy teljesen üres.');
    return { valid: false, errors, passes };
  }

  // 1. Verify 5 required sections
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

  // 3. Verify Acceptance Criteria Traceability section content
  const criteriaContent = extractSectionContent(bodyText, /##\s*📋\s*Acceptance Criteria Traceability/i);
  if (criteriaContent) {
    const lines = criteriaContent
      .split('\n')
      .map((l) => l.trim())
      .filter((l) => l.length > 0 && !l.startsWith('<!--'));

    // Check for table rows or checklist items
    const tableRows = lines.filter((l) => l.startsWith('|') && !l.includes('---') && !l.includes('Issue Acceptance Criterion'));
    const checklistItems = lines.filter((l) => /^[-*]\s*\[[ xX]\]/.test(l));

    if (tableRows.length === 0 && checklistItems.length === 0) {
      errors.push("Az 'Acceptance Criteria Traceability' nem tartalmaz lefedettségi táblázatot vagy ellenőrzőlistát.");
    } else {
      let validEntries = 0;
      for (const row of tableRows) {
        const cells = row.split('|').map((c) => c.trim()).filter((c, idx, arr) => idx > 0 && idx < arr.length - 1);
        if (cells.length >= 3) {
          const [crit, impl, proof] = cells;
          if (
            crit && !crit.includes('[Kritérium 1 leírása]') &&
            impl && !impl.includes('path/to/file') &&
            proof && !proof.includes('teszt vagy ellenőrző parancs')
          ) {
            validEntries++;
          }
        }
      }
      for (const item of checklistItems) {
        if (!item.includes('[Kritérium') && item.length > 20) {
          validEntries++;
        }
      }

      if (validEntries === 0) {
        errors.push("Az 'Acceptance Criteria Traceability' csak sablon/helykitöltő adatokat tartalmaz; valódi kritériumokat, megvalósítási fájlokat és tesztbizonyítékokat kell megadni.");
      } else {
        passes.push(`Acceptance criteria traceability érvényes bejegyzések száma: ${validEntries}`);
      }
    }
  }

  // 4. Verify Thematic Agent Approval Gate
  const gateContent = extractSectionContent(bodyText, /##\s*🛡️\s*Thematic Agent Approval Gate/i);
  if (gateContent) {
    // Check for banned legacy tokens
    const bannedMatches = gateContent.match(/\[(OK|NOK|PASS|N\/A)\]/gi);
    if (bannedMatches) {
      errors.push(`Tiltott/elavult státuszjelölés található a Thematic Gate-ben: ${bannedMatches.join(', ')}. Kizárólag [NO_FINDING] vagy [NOT_APPLICABLE] megengedett.`);
    }

    // Inspect each specialist signoff
    for (const spec of SPECIALISTS) {
      // Find line matching specialist id or title
      const specLineRegex = new RegExp(`^[-*]\\s*\\[([ xX])\\]\\s*\\*?\\*?.*${spec.id}.*`, 'mi');
      const lineMatch = gateContent.match(specLineRegex);

      if (!lineMatch) {
        errors.push(`Hiányzó specialist review sor a Thematic Gate-ben: '${spec.id}'`);
        continue;
      }

      const isChecked = lineMatch[1].toLowerCase() === 'x';
      const fullLine = lineMatch[0];

      // Check status token
      const tokenMatch = fullLine.match(/\[(NO_FINDING|NOT_APPLICABLE|FINDING|NOT_VERIFIED|OK|NOK|PASS|N\/A)\]/i);
      if (!tokenMatch) {
        errors.push(`A(z) '${spec.id}' sorában nem található érvényes státusz token ([NO_FINDING], [NOT_APPLICABLE]).`);
        continue;
      }

      const token = tokenMatch[1].toUpperCase();

      if (token === 'FINDING') {
        errors.push(`A(z) '${spec.id}' vizsgálat státusza [FINDING]; a gate blokkolva van a hibák javításáig.`);
      } else if (token === 'NOT_VERIFIED') {
        errors.push(`A(z) '${spec.id}' vizsgálat státusza [NOT_VERIFIED]; nem hitelesített review, a gate fail-closed módon elbukik.`);
      } else if (!isChecked) {
        errors.push(`A(z) '${spec.id}' vizsgálat nincs készre jelölve (a checkbox bejelöletlen: '[ ]').`);
      } else if (token === 'NOT_APPLICABLE') {
        // Must provide non-empty rationale
        const restOfLine = fullLine.slice(fullLine.indexOf(tokenMatch[0]) + tokenMatch[0].length).replace(/^[\s\-–—:]+/, '').trim();
        if (!restOfLine || restOfLine.toLowerCase() === 'indoklás.' || restOfLine.length < 5) {
          errors.push(`A(z) '${spec.id}' [NOT_APPLICABLE] státuszt kapott, de hiányzik az indoklás, hogy miért nem releváns a változtatás.`);
        } else {
          passes.push(`'${spec.id}' [NOT_APPLICABLE] indoklással elfogadva: "${restOfLine}"`);
        }

        // Check Review Risk Matrix if changed files are known
        if (changedFiles.length > 0) {
          const touchedDomainFiles = changedFiles.filter((f) => spec.patterns.some((p) => p.test(f)));
          if (touchedDomainFiles.length > 0) {
            errors.push(
              `Review-Kockázati Mátrix sértés: '${spec.id}' [NOT_APPLICABLE]-ként szerepel, de a PR módosította a szakterületéhez tartozó fájlokat: ${touchedDomainFiles.slice(0, 3).join(', ')}${touchedDomainFiles.length > 3 ? '...' : ''}`
            );
          }
        }
      } else if (token === 'NO_FINDING') {
        passes.push(`'${spec.id}' státusza érvényes: [NO_FINDING]`);
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
      // Fallback: try direct diff against base
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
  console.log('          PULL REQUEST COMPLIANCE & AI-AGENT GOVERNANCE GATE                  ');
  console.log('==============================================================================');
  if (changedFiles.length > 0) {
    console.log(`Módosított fájlok száma: ${changedFiles.length}`);
  }

  const result = verifyPrCompliance({ bodyText, changedFiles, verbose });

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
    console.log('✅ COMPLIANCE GATE PASSED: A PR leírás és az agent review-k megfelelnek.');
    console.log('==============================================================================');
    process.exit(0);
  }
}
