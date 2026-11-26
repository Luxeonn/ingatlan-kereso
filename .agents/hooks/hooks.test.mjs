import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';

import {
  evaluateCommand,
  hasUtf8Bom,
  parseGhPrCreateFlags,
  splitCommandSegments,
} from './pr-publish-guard.mjs';
import { buildRoutingOutput, classifyPrompt, extractIssueNumbers } from './issue-subagent-router.mjs';

const VALID_BODY = [
  '## 🎯 Miért készült?',
  '',
  'Automatikus PR feladási hookok és subagent routing bevezetése.',
  '',
  '## 🛠️ Mit módosít / Mit javít?',
  '',
  '- Hozzáadja a `.agents/hooks.json` konfigurációt.',
  '',
  'Closes #515',
].join('\n');

const RELEASE_BODY = [
  '## 🎯 Miért készült?',
  '',
  'Kiadási PR develop -> main.',
  '',
  '## 🛠️ Mit módosít / Mit javít?',
  '',
  '- Kiadás előkészítése.',
].join('\n');

function makeSafeOptions(overrides = {}) {
  return {
    resolveRepoRoot: (dir) => dir,
    getBranch: () => 'ik-515',
    getDirtyFiles: () => [],
    getChangedFiles: () => [],
    runValidateSkills: () => ({ ok: true, output: '' }),
    ...overrides,
  };
}

test('pr-publish-guard allows ordinary commands and does not false-positive on commit messages or grep', async () => {
  const res1 = await evaluateCommand('npm run test && ./gradlew check');
  assert.equal(res1.decision, 'allow');

  const resCommit = await evaluateCommand('git commit -m "feat(hooks): block gh pr merge and git push origin develop"');
  assert.equal(resCommit.decision, 'allow');

  const resGrep = await evaluateCommand('grep "gh pr create" .agents/hooks/pr-publish-guard.mjs');
  assert.equal(resGrep.decision, 'allow');
});

test('pr-publish-guard denies gh pr merge (human-only merge policy)', async () => {
  const res = await evaluateCommand('gh pr merge 514 --squash');
  assert.equal(res.decision, 'deny');
  assert.match(res.reason, /auto-merge tiltva/i);
});

test('pr-publish-guard blocks direct, force (+), delete, --all, and --mirror pushes to protected branches', async () => {
  for (const cmd of [
    'git push origin develop',
    'git push origin +develop',
    'git push origin HEAD:refs/heads/main',
    'git push origin --delete develop',
    'git push origin -d main',
    'git push --all origin',
    'git push --mirror origin',
  ]) {
    const res = await evaluateCommand(cmd, makeSafeOptions({ getBranch: () => 'ik-515' }));
    assert.equal(res.decision, 'deny', `Expected deny for: ${cmd}`);
  }

  const resCurrentMain = await evaluateCommand('git push', makeSafeOptions({ getBranch: () => 'main' }));
  assert.equal(resCurrentMain.decision, 'deny');

  const resUnknownBranch = await evaluateCommand('git push', makeSafeOptions({ getBranch: () => null }));
  assert.equal(resUnknownBranch.decision, 'deny');
  assert.match(resUnknownBranch.reason, /fail-closed/i);
});

test('pr-publish-guard allows normal push and branch deletion on non-protected ik-<issue> branches', async () => {
  const resPush = await evaluateCommand('git push -u origin ik-515', makeSafeOptions());
  assert.equal(resPush.decision, 'allow');

  const resDeleteFeature = await evaluateCommand('git push origin --delete ik-500', makeSafeOptions());
  assert.equal(resDeleteFeature.decision, 'allow');
});

test('pr-publish-guard denies gh pr create on non-ik branch, invalid title, missing body/--fill, or unknown branch', async () => {
  const resBad = await evaluateCommand(
    'gh pr create --base develop --head feature/foo --title "Updated stuff" --body "short"',
    makeSafeOptions({ getBranch: () => 'feature/foo' }),
  );
  assert.equal(resBad.decision, 'deny');
  assert.match(resBad.reason, /ik-<issue>/);
  assert.match(resBad.reason, /Conventional Commit/);
  assert.match(resBad.reason, /PR compliance hiba/);

  const resFill = await evaluateCommand(
    'gh pr create --base develop --head ik-515 --title "feat(hooks): add PR guard" --fill',
    makeSafeOptions(),
  );
  assert.equal(resFill.decision, 'deny');
  assert.match(resFill.reason, /--fill/);

  const resNoBranch = await evaluateCommand(
    'gh pr create --base develop --title "feat(hooks): add PR guard" --body "x"',
    makeSafeOptions({ getBranch: () => null }),
  );
  assert.equal(resNoBranch.decision, 'deny');
  assert.match(resNoBranch.reason, /fail-closed/i);
});

test('pr-publish-guard preserves quoted semicolons in --title and still validates --body-file', async () => {
  const tmpFile = path.join(os.tmpdir(), `pr-semi-test-${Date.now()}.md`);
  fs.writeFileSync(tmpFile, VALID_BODY, 'utf8');
  try {
    const cmd = `gh pr create --base develop --head ik-515 --title "feat(hooks): guard push; check PR" --body-file "${tmpFile}"`;
    assert.equal(splitCommandSegments(cmd).length, 1);
    const flags = parseGhPrCreateFlags(cmd);
    assert.equal(flags.title, 'feat(hooks): guard push; check PR');
    assert.equal(flags.bodyFile, tmpFile);

    const res = await evaluateCommand(cmd, makeSafeOptions());
    assert.equal(res.decision, 'allow');
  } finally {
    fs.unlinkSync(tmpFile);
  }
});

test('pr-publish-guard rejects UTF-8 BOM, dirty tracked files, missing verifier, and failed validate-skills.sh', async () => {
  const bomFile = path.join(os.tmpdir(), `pr-bom-test-${Date.now()}.md`);
  const okFile = path.join(os.tmpdir(), `pr-ok-test-${Date.now()}.md`);
  fs.writeFileSync(bomFile, Buffer.concat([Buffer.from([0xef, 0xbb, 0xbf]), Buffer.from(VALID_BODY, 'utf8')]));
  fs.writeFileSync(okFile, VALID_BODY, 'utf8');
  try {
    assert.equal(hasUtf8Bom(fs.readFileSync(bomFile)), true);
    const resBom = await evaluateCommand(
      `gh pr create --base develop --head ik-515 --title "feat(hooks): add PR guard" --body-file "${bomFile}"`,
      makeSafeOptions(),
    );
    assert.equal(resBom.decision, 'deny');
    assert.match(resBom.reason, /UTF-8 BOM/);

    const resDirty = await evaluateCommand(
      `gh pr create --base develop --head ik-515 --title "feat(hooks): add PR guard" --body-file "${okFile}"`,
      makeSafeOptions({ getDirtyFiles: () => ['M AGENTS.md'] }),
    );
    assert.equal(resDirty.decision, 'deny');
    assert.match(resDirty.reason, /Commitolatlan/);

    const resSkillsFail = await evaluateCommand(
      `gh pr create --base develop --head ik-515 --title "feat(hooks): add PR guard" --body-file "${okFile}"`,
      makeSafeOptions({ runValidateSkills: () => ({ ok: false, output: '[FAIL] Broken link' }) }),
    );
    assert.equal(resSkillsFail.decision, 'deny');
    assert.match(resSkillsFail.reason, /validate-skills\.sh sikertelen/);
  } finally {
    fs.unlinkSync(bomFile);
    fs.unlinkSync(okFile);
  }
});

test('pr-publish-guard allows develop -> main release PR and enforces DTO/OpenAPI drift via changedFiles', async () => {
  const relFile = path.join(os.tmpdir(), `pr-rel-test-${Date.now()}.md`);
  const okFile = path.join(os.tmpdir(), `pr-drift-test-${Date.now()}.md`);
  fs.writeFileSync(relFile, RELEASE_BODY, 'utf8');
  fs.writeFileSync(okFile, VALID_BODY, 'utf8');
  try {
    const resRelease = await evaluateCommand(
      `gh pr create --base main --head develop --title "chore(release): 3.6.0" --body-file "${relFile}"`,
      makeSafeOptions({ getBranch: () => 'develop' }),
    );
    assert.equal(resRelease.decision, 'allow');

    const resDrift = await evaluateCommand(
      `gh pr create --base develop --head ik-515 --title "feat(api): update DTO" --body-file "${okFile}"`,
      makeSafeOptions({
        getChangedFiles: () => ['src/main/java/ingatlanok/presentation/DashboardResponseDto.java'],
      }),
    );
    assert.equal(resDrift.decision, 'deny');
    assert.match(resDrift.reason, /PR compliance hiba/);
  } finally {
    fs.unlinkSync(relFile);
    fs.unlinkSync(okFile);
  }
});

test('issue-subagent-router extracts issue numbers, matches Hungarian accented words (ötlet), and classifies prompts', () => {
  assert.deepEqual(extractIssueNumbers('Oldd meg a #513 és ik-502 issue-kat'), ['513', '502']);

  const idea = classifyPrompt('Van egy új ötlet a keresőhöz');
  assert.ok(idea.agents.some((a) => a.agent === 'product-researcher'));

  const scraperBug = classifyPrompt('Javítsd az ingatlan.com scraper paginációs hibát (#495)');
  assert.deepEqual(scraperBug.issues, ['495']);
  const agentNames = scraperBug.agents.map((a) => a.agent);
  assert.ok(agentNames.includes('qa-test-engineer'));
  assert.ok(agentNames.includes('scraper-engineer'));
});

test('issue-subagent-router deduplicates repeated PreInvocation calls for same prompt and stays quiet on chit-chat', () => {
  const stateFile = path.join(os.tmpdir(), `router-state-${Date.now()}.json`);
  try {
    const quiet = buildRoutingOutput(
      { hookEventName: 'PreInvocation', conversationId: 'c1', lastUserInput: 'okés, köszi!' },
      { stateFile },
    );
    assert.deepEqual(quiet, {});

    const first = buildRoutingOutput(
      {
        hookEventName: 'PreInvocation',
        conversationId: 'c1',
        lastUserInput: 'Készíts új Recharts UI tervet a #515-höz',
      },
      { stateFile },
    );
    assert.equal(first.injectSteps.length, 1);
    assert.match(first.injectSteps[0].systemMessage.systemMessage, /gh issue view 515/);
    assert.match(first.injectSteps[0].systemMessage.systemMessage, /frontend-developer/);

    const second = buildRoutingOutput(
      {
        hookEventName: 'PreInvocation',
        conversationId: 'c1',
        lastUserInput: 'Készíts új Recharts UI tervet a #515-höz',
      },
      { stateFile },
    );
    assert.deepEqual(second, {});
  } finally {
    if (fs.existsSync(stateFile)) fs.unlinkSync(stateFile);
  }
});
