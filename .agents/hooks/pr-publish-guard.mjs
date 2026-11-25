#!/usr/bin/env node
/**
 * .agents/hooks/pr-publish-guard.mjs
 * Antigravity PreToolUse hook for run_command: fail-closed PR publication & push guardrails.
 *
 * Enforces:
 *  1. Never auto-merge PRs (`gh pr merge` denied)
 *  2. Never push directly to, force-push, mirror, or delete `main` or `develop`
 *  3. On `gh pr create`:
 *     - Branch must follow `ik-<issue>` (except `develop -> main` release PRs)
 *     - Title must be a verifiable Conventional Commit (or SemVer release)
 *     - Body / --body-file must be verifiable, clean UTF-8 without BOM, and pass
 *       `scripts/verify-pr-compliance.mjs` (including changedFiles OpenAPI/DTO drift check)
 *     - No uncommitted tracked modifications in working tree
 *     - `scripts/validate-skills.sh` must exist at repo root and pass
 */

import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { execFileSync } from 'node:child_process';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const DEFAULT_REPO_ROOT = path.resolve(__dirname, '..', '..');

const CONVENTIONAL_TITLE_RE =
  /^((feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([a-zA-Z0-9._-]+\))?: .+|v?[0-9]+\.[0-9]+\.[0-9]+([-+][0-9A-Za-z.-]+)?)$/;
const ISSUE_BRANCH_RE = /^ik-[0-9]+$/;
const PROTECTED_BRANCHES = new Set(['main', 'develop']);
const VALIDATE_SKILLS_TIMEOUT_MS = 35000;

export function hasUtf8Bom(buf) {
  return Buffer.isBuffer(buf) && buf.length >= 3 && buf[0] === 0xef && buf[1] === 0xbb && buf[2] === 0xbf;
}

export function isMainModule(entryArg, currentFile) {
  if (!entryArg) return false;
  const resolved = path.resolve(entryArg);
  if (process.platform === 'win32') {
    return resolved.toLowerCase() === currentFile.toLowerCase();
  }
  return resolved === currentFile;
}

/**
 * Splits a shell command line into segments on unquoted operators (newline, ;, &&, ||, |).
 */
export function splitCommandSegments(cmd) {
  const segments = [];
  let current = '';
  let quote = null;

  for (let i = 0; i < cmd.length; i += 1) {
    const ch = cmd[i];
    const next = i + 1 < cmd.length ? cmd[i + 1] : '';

    if (quote) {
      if (ch === '\\' && quote === '"' && next === '"') {
        current += '\\"';
        i += 1;
      } else if (ch === quote) {
        quote = null;
        current += ch;
      } else {
        current += ch;
      }
      continue;
    }

    if (ch === '"' || ch === "'") {
      quote = ch;
      current += ch;
      continue;
    }

    if (ch === '\r' || ch === '\n' || ch === ';') {
      if (current.trim()) segments.push(current.trim());
      current = '';
      continue;
    }

    if ((ch === '&' && next === '&') || (ch === '|' && next === '|')) {
      if (current.trim()) segments.push(current.trim());
      current = '';
      i += 1;
      continue;
    }

    if (ch === '|') {
      if (current.trim()) segments.push(current.trim());
      current = '';
      continue;
    }

    current += ch;
  }

  if (current.trim()) {
    segments.push(current.trim());
  }
  return segments;
}

/**
 * Tokenizes a single shell command segment respecting single and double quotes.
 * Preserves literal Windows backslashes inside double quotes (only unescapes `\"`).
 */
export function tokenizeCommand(segment) {
  const tokens = [];
  let current = '';
  let quote = null;

  for (let i = 0; i < segment.length; i += 1) {
    const ch = segment[i];
    if (quote) {
      if (ch === '\\' && quote === '"' && i + 1 < segment.length && segment[i + 1] === '"') {
        current += '"';
        i += 1;
      } else if (ch === quote) {
        quote = null;
      } else {
        current += ch;
      }
    } else if (ch === '"' || ch === "'") {
      quote = ch;
    } else if (/\s/.test(ch)) {
      if (current.length > 0) {
        tokens.push(current);
        current = '';
      }
    } else {
      current += ch;
    }
  }
  if (current.length > 0) {
    tokens.push(current);
  }
  return tokens;
}

function normalizeExeName(token) {
  const base = path.basename(token).toLowerCase();
  return base.replace(/\.(exe|cmd|bat)$/i, '');
}

/**
 * Finds the start index of the actual executable token in a segment,
 * skipping env assignments (`KEY=VAL`), PowerShell variable assignments (`$x =`), and `&`.
 */
function findCommandStart(tokens) {
  let idx = 0;
  while (idx < tokens.length) {
    const tok = tokens[idx];
    if (tok === '&' || tok === 'env' || tok === 'command') {
      idx += 1;
      continue;
    }
    if (/^[A-Za-z_][A-Za-z0-9_]*=/.test(tok)) {
      idx += 1;
      continue;
    }
    if (tok.startsWith('$') && tokens[idx + 1] === '=') {
      idx += 2;
      continue;
    }
    break;
  }
  return idx;
}

/**
 * Inspects a tokenized segment for `git` subcommand and optional `-C <dir>`.
 */
function inspectGitSegment(tokens, startIdx) {
  if (startIdx >= tokens.length || normalizeExeName(tokens[startIdx]) !== 'git') {
    return null;
  }
  let gitDir = null;
  let i = startIdx + 1;
  while (i < tokens.length) {
    const tok = tokens[i];
    if (tok === '-C' && i + 1 < tokens.length) {
      gitDir = tokens[i + 1];
      i += 2;
      continue;
    }
    if (tok.startsWith('-C') && tok.length > 2) {
      gitDir = tok.slice(2);
      i += 1;
      continue;
    }
    if ((tok === '-c' || tok === '--git-dir' || tok === '--work-tree') && i + 1 < tokens.length) {
      i += 2;
      continue;
    }
    if (tok.startsWith('-')) {
      i += 1;
      continue;
    }
    return { subcommand: tok.toLowerCase(), subIdx: i, gitDir };
  }
  return null;
}

/**
 * Inspects a tokenized segment for `gh pr create` or `gh pr merge`.
 */
function inspectGhPrSegment(tokens, startIdx) {
  if (startIdx + 2 >= tokens.length || normalizeExeName(tokens[startIdx]) !== 'gh') {
    return null;
  }
  if (tokens[startIdx + 1].toLowerCase() !== 'pr') {
    return null;
  }
  const action = tokens[startIdx + 2].toLowerCase();
  if (action === 'create' || action === 'merge') {
    return { action, argStartIdx: startIdx + 3 };
  }
  return null;
}

/**
 * Parses `gh pr create` flags from tokenized arguments.
 */
export function parseGhPrCreateFlags(cmd) {
  const segments = splitCommandSegments(cmd);
  for (const seg of segments) {
    const tokens = tokenizeCommand(seg);
    const startIdx = findCommandStart(tokens);
    const gh = inspectGhPrSegment(tokens, startIdx);
    if (!gh || gh.action !== 'create') continue;
    return parseGhPrCreateTokens(tokens, gh.argStartIdx);
  }
  return null;
}

function parseGhPrCreateTokens(tokens, argStartIdx) {
  const flags = {
    base: null,
    head: null,
    title: null,
    body: null,
    bodyFile: null,
    fill: false,
  };

  for (let i = argStartIdx; i < tokens.length; i += 1) {
    const tok = tokens[i];
    const next = i + 1 < tokens.length ? tokens[i + 1] : null;
    if ((tok === '--base' || tok === '-B') && next !== null) {
      flags.base = next;
      i += 1;
    } else if (tok.startsWith('--base=')) {
      flags.base = tok.slice('--base='.length);
    } else if ((tok === '--head' || tok === '-H') && next !== null) {
      flags.head = next;
      i += 1;
    } else if (tok.startsWith('--head=')) {
      flags.head = tok.slice('--head='.length);
    } else if ((tok === '--title' || tok === '-t') && next !== null) {
      flags.title = next;
      i += 1;
    } else if (tok.startsWith('--title=')) {
      flags.title = tok.slice('--title='.length);
    } else if ((tok === '--body' || tok === '-b') && next !== null) {
      flags.body = next;
      i += 1;
    } else if (tok.startsWith('--body=')) {
      flags.body = tok.slice('--body='.length);
    } else if ((tok === '--body-file' || tok === '-F') && next !== null) {
      flags.bodyFile = next;
      i += 1;
    } else if (tok.startsWith('--body-file=')) {
      flags.bodyFile = tok.slice('--body-file='.length);
    } else if (tok === '--fill' || tok === '-f' || tok === '--fill-first' || tok === '--fill-verbose') {
      flags.fill = true;
    }
  }
  return flags;
}

function defaultResolveRepoRoot(workDir) {
  try {
    return execFileSync('git', ['rev-parse', '--show-toplevel'], {
      cwd: workDir,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    }).trim();
  } catch {
    return '';
  }
}

function defaultGitBranch(repoDir) {
  try {
    return execFileSync('git', ['rev-parse', '--abbrev-ref', 'HEAD'], {
      cwd: repoDir,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    }).trim();
  } catch {
    return null;
  }
}

function defaultTrackedDirtyFiles(repoDir) {
  try {
    const out = execFileSync('git', ['status', '--porcelain'], {
      cwd: repoDir,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'ignore'],
    });
    return out
      .split(/\r?\n/)
      .filter((line) => line.length >= 3 && !line.startsWith('??'))
      .map((line) => line.trim());
  } catch {
    return null;
  }
}

function defaultChangedFiles(repoDir, baseRef) {
  const candidates = [`origin/${baseRef}...HEAD`, `${baseRef}...HEAD`];
  for (const range of candidates) {
    try {
      const out = execFileSync('git', ['diff', '--name-only', range], {
        cwd: repoDir,
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'ignore'],
      });
      return out
        .split(/\r?\n/)
        .map((line) => line.trim())
        .filter(Boolean);
    } catch {
      // try next range
    }
  }
  return [];
}

function findBashExecutable() {
  if (process.platform === 'win32') {
    const gitBash = 'C:\\Program Files\\Git\\bin\\bash.exe';
    if (fs.existsSync(gitBash)) return gitBash;
  }
  return 'bash';
}

function defaultRunValidateSkills(repoDir) {
  const scriptPath = path.join(repoDir, 'scripts', 'validate-skills.sh');
  if (!fs.existsSync(scriptPath)) {
    return { ok: false, output: `Hiányzó validátor script: ${scriptPath}` };
  }
  try {
    const bashExe = findBashExecutable();
    execFileSync(bashExe, ['scripts/validate-skills.sh'], {
      cwd: repoDir,
      encoding: 'utf8',
      stdio: ['ignore', 'pipe', 'pipe'],
      timeout: VALIDATE_SKILLS_TIMEOUT_MS,
    });
    return { ok: true, output: '' };
  } catch (err) {
    const out = `${err.stdout || ''}\n${err.stderr || ''}`.trim();
    const failLines = out
      .split(/\r?\n/)
      .filter((l) => l.includes('[FAIL]') || l.includes('FAILED'))
      .slice(0, 5)
      .join(' | ');
    return { ok: false, output: failLines || err.message };
  }
}

async function loadComplianceVerifier(repoDir) {
  const candidate = path.join(repoDir, 'scripts', 'verify-pr-compliance.mjs');
  if (!fs.existsSync(candidate)) return null;
  const mod = await import(pathToFileURL(candidate).href);
  return mod.verifyPrCompliance;
}

function normalizeRefName(rawRef) {
  let target = rawRef.includes(':') ? rawRef.split(':').pop() : rawRef;
  target = target.replace(/^\+/, '');
  target = target.replace(/^refs\/heads\//, '');
  return target;
}

function detectProtectedPushViolation(tokens, subIdx, currentBranch) {
  const pushArgs = tokens.slice(subIdx + 1);
  if (pushArgs.some((t) => t === '--all' || t === '--mirror')) {
    return 'A `git push --all` és `--mirror` parancsok tiltottak, mert védett ágakat (`main`, `develop`) érinthetnek.';
  }

  const positional = [];
  for (let i = 0; i < pushArgs.length; i += 1) {
    const tok = pushArgs[i];
    if (tok === '--repo' || tok === '-o' || tok === '--push-option' || tok === '--receive-pack' || tok === '--exec') {
      i += 1;
      continue;
    }
    if (tok.startsWith('-')) continue;
    positional.push(tok);
  }

  const refspecs = positional.slice(1);
  if (refspecs.length === 0) {
    if (!currentBranch) {
      return 'Nem állapítható meg az aktuális git branch a `git push` biztonsági ellenőrzéséhez (fail-closed).';
    }
    if (PROTECTED_BRANCHES.has(currentBranch)) {
      return `Közvetlen push a '${currentBranch}' ágra tiltva! Készíts ik-<issue> ágat a 'develop' ágból, és nyiss Pull Requestet.`;
    }
    return null;
  }

  for (const rawRef of refspecs) {
    const clean = normalizeRefName(rawRef);
    if (PROTECTED_BRANCHES.has(clean)) {
      return `Védett ág ('${clean}') közvetlen push-olása, force-push-olása vagy törlése tiltva!`;
    }
    if (rawRef.includes(':')) {
      const source = rawRef.split(':')[0].replace(/^\+/, '').replace(/^refs\/heads\//, '');
      if (source === '' && PROTECTED_BRANCHES.has(clean)) {
        return `Védett ág ('${clean}') távoli törlése tiltva!`;
      }
    }
  }
  return null;
}

/**
 * Evaluates a run_command CommandLine and returns a PreToolUse decision object.
 */
export async function evaluateCommand(cmd, options = {}) {
  if (!cmd || typeof cmd !== 'string') {
    return { decision: 'allow' };
  }

  const segments = splitCommandSegments(cmd);
  const baseCwd = options.cwd || DEFAULT_REPO_ROOT;
  let activeCwd = baseCwd;
  let hasGitCommitInCmd = false;

  const gitPushOps = [];
  const ghPrCreateOps = [];

  for (const seg of segments) {
    const tokens = tokenizeCommand(seg);
    const startIdx = findCommandStart(tokens);
    if (startIdx >= tokens.length) continue;

    const exe = normalizeExeName(tokens[startIdx]);
    if (exe === 'cd' || exe === 'push-location' || exe === 'set-location') {
      const targetArg = tokens.slice(startIdx + 1).find((t) => !t.startsWith('-'));
      if (targetArg && !targetArg.startsWith('$')) {
        activeCwd = path.resolve(activeCwd, targetArg);
      }
      continue;
    }

    const gh = inspectGhPrSegment(tokens, startIdx);
    if (gh?.action === 'merge') {
      return {
        decision: 'deny',
        reason:
          'PR auto-merge tiltva (`gh pr merge`): a PR merge-elése kizárólag a repository tulajdonos joga (AGENTS.md / gemini.md).',
      };
    }
    if (gh?.action === 'create') {
      ghPrCreateOps.push({
        workDir: activeCwd,
        flags: parseGhPrCreateTokens(tokens, gh.argStartIdx),
      });
      continue;
    }

    const gitInfo = inspectGitSegment(tokens, startIdx);
    if (gitInfo?.subcommand === 'commit') {
      hasGitCommitInCmd = true;
    } else if (gitInfo?.subcommand === 'push') {
      const pushDir =
        gitInfo.gitDir && !gitInfo.gitDir.startsWith('$')
          ? path.resolve(activeCwd, gitInfo.gitDir)
          : activeCwd;
      gitPushOps.push({ workDir: pushDir, tokens, subIdx: gitInfo.subIdx });
    }
  }

  if (gitPushOps.length === 0 && ghPrCreateOps.length === 0) {
    return { decision: 'allow' };
  }

  const resolveRepoRoot = options.resolveRepoRoot || defaultResolveRepoRoot;
  const getBranch = options.getBranch || defaultGitBranch;
  const getDirtyFiles = options.getDirtyFiles || defaultTrackedDirtyFiles;
  const getChangedFiles = options.getChangedFiles || defaultChangedFiles;
  const runValidateSkills = options.runValidateSkills || defaultRunValidateSkills;

  for (const pushOp of gitPushOps) {
    const repoRoot = resolveRepoRoot(pushOp.workDir) || pushOp.workDir;
    const currentBranch = getBranch(repoRoot);
    const pushError = detectProtectedPushViolation(pushOp.tokens, pushOp.subIdx, currentBranch);
    if (pushError) {
      return { decision: 'deny', reason: pushError };
    }
  }

  for (const prOp of ghPrCreateOps) {
    const repoRoot = resolveRepoRoot(prOp.workDir);
    if (!repoRoot) {
      return {
        decision: 'deny',
        reason: `PR feladási hook (pr-publish-guard) blokkolta: nem található git repository gyökér itt: ${prOp.workDir}`,
      };
    }

    const currentBranch = getBranch(repoRoot);
    const { flags } = prOp;
    const violations = [];

    if (flags.head && flags.head.startsWith('$')) {
      violations.push("A '--head' nem lehet ellenőrizhetetlen shell változó; adj meg literális branch nevet.");
    }
    if (flags.base && flags.base.startsWith('$')) {
      violations.push("A '--base' nem lehet ellenőrizhetetlen shell változó; adj meg literális alap branch nevet.");
    }

    const headRef = flags.head && !flags.head.startsWith('$') ? flags.head : currentBranch;
    const baseRef = flags.base && !flags.base.startsWith('$') ? flags.base : 'develop';
    const isReleasePr = baseRef === 'main' && headRef === 'develop';

    // 1. Branch naming check (fail-closed if branch cannot be resolved)
    if (!headRef) {
      violations.push('Nem állapítható meg a PR forrás branch-e (fail-closed).');
    } else if (!isReleasePr) {
      if (PROTECTED_BRANCHES.has(headRef)) {
        violations.push(`Nem nyitható PR védett ágról ('${headRef}'). Használj 'ik-<issue>' branch-et!`);
      } else if (!ISSUE_BRANCH_RE.test(headRef)) {
        violations.push(`Branch név ('${headRef}') nem felel meg a kötelező 'ik-<issue>' formátumnak.`);
      }
    }

    // 2. Conventional Commit title check (fail-closed)
    if (!flags.title || flags.title.startsWith('$')) {
      violations.push("Kötelező literális Conventional Commit '--title' paraméter PR nyitáskor.");
    } else if (!CONVENTIONAL_TITLE_RE.test(flags.title)) {
      violations.push(
        `A PR cím ('${flags.title}') nem Conventional Commit formátumú (pl. 'feat(scope): ...' vagy 'fix(scope): ...').`,
      );
    }

    // 3. Body & UTF-8 BOM + PR compliance check (fail-closed)
    let bodyText = null;
    if (flags.bodyFile) {
      if (flags.bodyFile.startsWith('$')) {
        violations.push("A '--body-file' nem lehet ellenőrizhetetlen shell változó; adj meg literális fájlútvonalat.");
      } else {
        const resolvedBodyPath = path.isAbsolute(flags.bodyFile)
          ? flags.bodyFile
          : path.resolve(prOp.workDir, flags.bodyFile);
        if (!fs.existsSync(resolvedBodyPath)) {
          violations.push(`A '--body-file' fájl nem létezik: ${resolvedBodyPath}`);
        } else {
          const buf = fs.readFileSync(resolvedBodyPath);
          if (hasUtf8Bom(buf)) {
            violations.push(`A '--body-file' UTF-8 BOM-ot tartalmaz (${flags.bodyFile}). Mentsd BOM nélküli UTF-8-ként!`);
          }
          bodyText = buf.toString('utf8');
        }
      }
    } else if (flags.body) {
      if (flags.body.startsWith('$')) {
        violations.push("A '--body' nem lehet ellenőrizhetetlen shell változó; használj '--body-file' fájlt.");
      } else {
        bodyText = flags.body;
      }
    } else {
      violations.push(
        "Kötelező a '--body-file' (vagy literális '--body') megadása; a body nélküli vagy '--fill' PR nyitás tiltott.",
      );
    }

    if (bodyText !== null) {
      const verifyCompliance = options.verifyCompliance || (await loadComplianceVerifier(repoRoot));
      if (!verifyCompliance) {
        violations.push('Nem tölthető be a `scripts/verify-pr-compliance.mjs` ellenőrző (fail-closed).');
      } else {
        const changedFiles = getChangedFiles(repoRoot, baseRef);
        const res = verifyCompliance({
          bodyText,
          changedFiles,
          author: '',
          baseRef,
          headRef: headRef || '',
        });
        if (!res.valid) {
          for (const err of res.errors) {
            violations.push(`PR compliance hiba: ${err}`);
          }
        }
      }
    }

    // 4. Uncommitted tracked changes check (fail-closed on git error)
    const dirtyTracked = getDirtyFiles(repoRoot);
    if (dirtyTracked === null) {
      violations.push('Nem sikerült a `git status --porcelain` futtatása a munkakönyvtár ellenőrzéséhez (fail-closed).');
    } else if (dirtyTracked.length > 0 && !hasGitCommitInCmd) {
      violations.push(
        `Commitolatlan módosítások vannak a munkakönyvtárban (${dirtyTracked.slice(0, 3).join(', ')}). Commitold vagy tedd el PR nyitás előtt!`,
      );
    }

    // 5. Skills & governance validator
    if (violations.length === 0) {
      const skillResult = runValidateSkills(repoRoot);
      if (!skillResult.ok) {
        violations.push(`scripts/validate-skills.sh sikertelen: ${skillResult.output}`);
      }
    }

    if (violations.length > 0) {
      return {
        decision: 'deny',
        reason: [
          'PR feladási hook (pr-publish-guard) blokkolta a műveletet:',
          ...violations.map((v) => `- ${v}`),
          'Javítsd a fenti hibákat, futtasd a pr-quality-reviewer auditot, és próbáld újra. (Ne felejtsd el PR nyitás után: gh pr edit <id> --add-reviewer "@copilot".)',
        ].join('\n'),
      };
    }
  }

  return { decision: 'allow' };
}

async function main() {
  let raw = '';
  try {
    raw = fs.readFileSync(0, 'utf8').trim();
    if (!raw) {
      process.stdout.write(JSON.stringify({ decision: 'allow' }));
      return;
    }
    const payload = JSON.parse(raw);
    const cmd = payload?.toolCall?.args?.CommandLine ?? payload?.toolCall?.args?.command ?? '';
    const cwd = payload?.cwd || payload?.workspacePaths?.[0] || DEFAULT_REPO_ROOT;
    const result = await evaluateCommand(cmd, { cwd });
    process.stdout.write(JSON.stringify(result));
  } catch (err) {
    if (/\b(gh\s+pr\s+(create|merge)|git\s+[^\n]*push)\b/i.test(raw)) {
      process.stdout.write(
        JSON.stringify({
          decision: 'deny',
          reason: `PR feladási hook váratlan hiba miatt fail-closed blokkolt: ${err?.message || 'ismeretlen hiba'}`,
        }),
      );
      return;
    }
    process.stdout.write(JSON.stringify({ decision: 'allow' }));
  }
}

if (isMainModule(process.argv[1], __filename)) {
  await main();
}
