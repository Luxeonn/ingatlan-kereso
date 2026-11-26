#!/usr/bin/env node
/**
 * .agents/hooks/issue-subagent-router.mjs
 * Antigravity SessionStart & PreInvocation hook:
 * Automatically routes daily tasks and GitHub issues to the 8 narrow-scope custom subagents.
 */

import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);

function getDefaultStateFile() {
  let userPart = 'default';
  try {
    userPart = os.userInfo().username.replace(/[^a-zA-Z0-9_-]/g, '_') || 'default';
  } catch {
    userPart = 'default';
  }
  return path.join(os.tmpdir(), `ingatlan-kereso-subagent-router-${userPart}.json`);
}

function isMainModule(entryArg, currentFile) {
  if (!entryArg) return false;
  const resolved = path.resolve(entryArg);
  if (process.platform === 'win32') {
    return resolved.toLowerCase() === currentFile.toLowerCase();
  }
  return resolved === currentFile;
}

// Unicode-aware left word boundary so Hungarian accented starts (e.g., "ötlet") match cleanly.
const ROUTE_RULES = [
  {
    agent: 'product-researcher',
    phase: 'Kutatás & Roadmap',
    pattern:
      /(?<![\p{L}\p{N}_])(kutat|roadmap|fejl[oő]d|[oö]tlet|trend|proptech|merre\s+(?:érdemes|lehetne)|funkci[oó]javaslat|prioriz[aá]l)/iu,
  },
  {
    agent: 'ui-designer',
    phase: 'UI/UX Tervezés',
    pattern:
      /(?<![\p{L}\p{N}_])(ui\/?ux|diz[aá]jn|design|wireframe|layout|sz[ií]ns[eé]ma|design\s*token|wcag|akad[aá]lymentes|moderniz[aá]l|kin[eé]zet|vizu[aá]lis)/iu,
  },
  {
    agent: 'qa-test-engineer',
    phase: 'Regression Test First (Red) / QA / E2E',
    pattern:
      /(?<![\p{L}\p{N}_])(bug|hiba|hib[aá]t|jav[ií]t|fix|regresszi[oó]|teszt|test|e2e|playwright|vitest|junit|golden\s*dataset|jacoco|lefedetts[eé]g)/iu,
  },
  {
    agent: 'scraper-engineer',
    phase: 'Portál Scraper & Sidecar',
    pattern:
      /(?<![\p{L}\p{N}_])(scraper|scrap|port[aá]l|portal|jsoup|sidecar|waf|cloudflare|dh\.hu|oc\.hu|jofogas|koltozzbe|ingatlanbazar|ingatlancom|ingatlan\.com|html\s*import|httpfetcher|propertyparser|ssrf|pagin[aá]ci[oó])/iu,
  },
  {
    agent: 'domain-algorithm-engineer',
    phase: 'Domain Algoritmusok',
    pattern:
      /(?<![\p{L}\p{N}_])(dedup|deduplik|f[uü]rt|cluster|medi[aá]n|median|bargain|alkalmi\s*v[eé]tel|quality\s*score|adatmin[oő]s[eé]g|outlier|normaliz[aá]l|propertytype|algoritmus|analitika|statisztika)/iu,
  },
  {
    agent: 'backend-developer',
    phase: 'Backend / JPA / OpenAPI',
    pattern:
      /(?<![\p{L}\p{N}_])(backend|java\s*25|spring|rest|api|endpoint|v[eé]gpont|controller|jpa|hibernate|postgres|h2|mapstruct|lombok|dto|openapi|api-spec|persistence|perzisztencia|archunit)/iu,
  },
  {
    agent: 'frontend-developer',
    phase: 'Frontend (React 19 / TS / Tailwind)',
    pattern:
      /(?<![\p{L}\p{N}_])(frontend|react|typescript|tailwind|vite|zustand|tanstack|recharts|komponens|component|hook|store|css|modal|drawer|k[aá]rtya|sz[uű]r[oő]|listan[eé]zet)/iu,
  },
  {
    agent: 'pr-quality-reviewer',
    phase: 'PR Kódminőség Audit (Read-Only)',
    pattern: /(?<![\p{L}\p{N}_])(pr|pull\s*request|review|diff|k[oó]dmin[oő]s[eé]g|audit|checkstyle|eslint|compliance)(?![\p{L}\p{N}_])/iu,
  },
];

export function extractIssueNumbers(text) {
  if (!text) return [];
  const found = new Set();
  const re = /(?:^|[\s([,])(?:ik-|#|issue\s*#?)(\d{2,5})(?![\p{L}\p{N}_])/giu;
  let match;
  while ((match = re.exec(text)) !== null) {
    found.add(match[1]);
  }
  return [...found];
}

export function classifyPrompt(text) {
  if (!text || typeof text !== 'string') {
    return { issues: [], agents: [] };
  }
  const issues = extractIssueNumbers(text);
  const agents = [];
  for (const rule of ROUTE_RULES) {
    if (rule.pattern.test(text)) {
      agents.push({ agent: rule.agent, phase: rule.phase });
    }
  }
  return { issues, agents };
}

function shouldEmitForPrompt(conversationId, lastUserInput, stateFile = getDefaultStateFile()) {
  const key = crypto
    .createHash('sha256')
    .update(`${conversationId || 'default'}::${lastUserInput.trim()}`)
    .digest('hex');
  let state = {};
  try {
    if (fs.existsSync(stateFile)) {
      state = JSON.parse(fs.readFileSync(stateFile, 'utf8'));
    }
  } catch {
    state = {};
  }
  if (state[conversationId || 'default'] === key) {
    return false;
  }
  state[conversationId || 'default'] = key;
  const keys = Object.keys(state);
  if (keys.length > 50) {
    for (const oldKey of keys.slice(0, keys.length - 50)) {
      delete state[oldKey];
    }
  }
  try {
    fs.writeFileSync(stateFile, JSON.stringify(state), { encoding: 'utf8', mode: 0o600 });
  } catch {
    // Best-effort dedup cache
  }
  return true;
}

export function buildRoutingOutput(payload, options = {}) {
  const eventName = payload?.hookEventName || '';

  if (eventName === 'SessionStart') {
    return {
      injectSteps: [
        {
          systemMessage: {
            systemMessage:
              'Aktív a 8 fős szűk szerepkörű ügynökcsapat (.agents/agents/): product-researcher, ui-designer, qa-test-engineer, backend-developer, frontend-developer, scraper-engineer, domain-algorithm-engineer, pr-quality-reviewer. Napi fejlesztésben alapértelmezett a feladathoz/issue-hoz illő subagent(ek) delegálása (`invoke_subagent`), PR nyitás előtt pedig a `pr-quality-reviewer` read-only audit futtatása.',
          },
        },
      ],
    };
  }

  const lastUserInput = payload?.lastUserInput || '';
  const { issues, agents } = classifyPrompt(lastUserInput);
  if (issues.length === 0 && agents.length === 0) {
    return {};
  }

  const stateFile = options.stateFile || getDefaultStateFile();
  if (!options.skipDedup && !shouldEmitForPrompt(payload?.conversationId, lastUserInput, stateFile)) {
    return {};
  }

  const parts = [];
  if (issues.length > 0) {
    const issueList = issues.map((id) => `#${id}`).join(', ');
    parts.push(
      `Issue észlelve (${issueList}): először olvasd be a részleteket (\`gh issue view ${issues[0]}\`), majd a címkék és érintett rétegek alapján delegáld a munkát a megfelelő szűk szerepkörű subagent(ek)nek (\`invoke_subagent\`).`,
    );
  }
  if (agents.length > 0) {
    const agentList = agents.map((a) => `\`${a.agent}\` (${a.phase})`).join(', ');
    parts.push(`Javasolt subagent útvonal ehhez a kéréshez: ${agentList}.`);
  }
  if (
    agents.some((a) =>
      ['backend-developer', 'frontend-developer', 'scraper-engineer', 'domain-algorithm-engineer'].includes(a.agent),
    )
  ) {
    parts.push(
      'PR feladás előtt kötelező a `pr-quality-reviewer` read-only audit.',
    );
  }

  return {
    injectSteps: [
      {
        systemMessage: {
          systemMessage: `[Subagent Router] ${parts.join(' ')}`,
        },
      },
    ],
  };
}

function main() {
  try {
    const raw = fs.readFileSync(0, 'utf8').trim();
    if (!raw) {
      process.stdout.write('{}');
      return;
    }
    const payload = JSON.parse(raw);
    const out = buildRoutingOutput(payload);
    process.stdout.write(JSON.stringify(out));
  } catch {
    process.stdout.write('{}');
  }
}

if (isMainModule(process.argv[1], __filename)) {
  main();
}
