#!/usr/bin/env bash
set -eo pipefail

echo "=== 🛡️ Thematic Agent Approval Gate (advisory) ==="

IS_PR="${IS_PR:-true}"
BASE_REF="${BASE_REF:-develop}"
ACTOR="${ACTOR:-${GITHUB_ACTOR:-}}"
BRANCH="${BRANCH:-${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}}"
BODY="${BODY:-}"
CHANGED_FILES="${CHANGED_FILES:-}"

if [ "$IS_PR" != "true" ]; then
  echo "ℹ️ No associated PR; description-based thematic review is skipped."
  exit 0
fi

if [[ "$ACTOR" == "dependabot[bot]" || "$ACTOR" == "dependabot" || "$BRANCH" =~ ^dependabot\/ ]]; then
  echo "🤖 Dependabot update detected ($BRANCH / $ACTOR); thematic description checks are skipped."
  exit 0
fi

# This check is intentionally advisory: a PR description is free-form text.
# Missing/malformed approval notes must not prevent contributors from opening a PR.
if [ -z "$CHANGED_FILES" ]; then
  if git rev-parse --verify "origin/${BASE_REF}" >/dev/null 2>&1; then
    CHANGED_FILES=$(git diff --name-only "origin/${BASE_REF}...HEAD") || {
      echo "::warning::Could not determine changed files; skipping advisory thematic review."
      exit 0
    }
  elif git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
    CHANGED_FILES=$(git diff --name-only HEAD~1) || {
      echo "::warning::Could not determine changed files; skipping advisory thematic review."
      exit 0
    }
  else
    echo "::warning::No suitable git ref found to determine changed files; skipping advisory thematic review."
    exit 0
  fi
fi

CLEAN_CHANGED_FILES=$(echo "$CHANGED_FILES" | sed '/^[[:space:]]*$/d')
if [ -z "$CLEAN_CHANGED_FILES" ]; then
  echo "::warning::Changed-file list is empty; skipping advisory thematic review."
  exit 0
fi

echo "Changed files:"
echo "$CLEAN_CHANGED_FILES"
echo "----------------------------------------"

MISSING_APPROVALS=""

validate_agent_approval() {
  local role_pattern="$1"
  local display_name="$2"
  local guidance="$3"

  local match_line
  match_line=$(echo "$BODY" | grep -iE "^[[:space:]]*-[[:space:]]*\[[xX]\][[:space:]]*.*(${role_pattern})" | head -n 1 || true)

  if [ -z "$match_line" ]; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ${display_name}: No checked approval note. ${guidance}"
    return 1
  fi

  local after_colon
  after_colon=$(echo "$match_line" | sed -E "s/^[[:space:]]*-[[:space:]]*\[[xX]\][[:space:]]*.*(${role_pattern})[^:]*:[[:space:]]*//")

  if [ -z "$after_colon" ] || [ "$after_colon" = "$match_line" ] ||
     ! echo "$after_colon" | grep -qiE "^[[:space:]]*(\[NO_FINDING\]|\bNO_FINDING\b|\[NOT_APPLICABLE\]|\bNOT_APPLICABLE\b|\[OK\]|\bOK\b|\[N/A\]|\bN/A\b)" ||
     echo "$after_colon" | grep -qiE "^(\[FINDING\]|\bFINDING\b|\[NOK\]|\bNOK\b|\[NOT_VERIFIED\]|\bNOT_VERIFIED\b)" ||
     [ "$(echo "$after_colon" | wc -c)" -lt 6 ]; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ${display_name}: Approval note is missing, incomplete, or non-positive: '${match_line}'"
    return 1
  fi
  return 0
}

TOUCHED_FULLSTACK=false
TOUCHED_SCRAPER=false
TOUCHED_DEVOPS=false
TOUCHED_UNCLASSIFIED=false

while IFS= read -r file; do
  [ -z "$file" ] && continue
  matched=false

  if echo "$file" | grep -qE "^(src/main/java/ingatlanok/(core|presentation|web|persistence|model)/|src/main/java/ingatlanok/Main\.java|src/main/java/ingatlanok/IngatlanApplication\.java|frontend/|build\.gradle\.kts|settings\.gradle\.kts|docs/architecture\.md|data/|\.agents/skills/fullstack-architect/)"; then
    TOUCHED_FULLSTACK=true
    matched=true
  fi

  if echo "$file" | grep -qE "^(src/main/java/ingatlanok/(portal|scraper|common)/|tools/|src/main/java/ingatlanok/common/(SidecarCliExecutor|BrowserUtils|HttpFetcher)\.java|\.agents/skills/scraper-security-engineer/)"; then
    TOUCHED_SCRAPER=true
    matched=true
  fi

  if echo "$file" | grep -qE "^(\.github/|Dockerfile.*|docker-compose.*|src/test/|frontend/.*test.*|frontend/package.*\.json|frontend/playwright.*|frontend/vite.*|build\.gradle\.kts|settings\.gradle\.kts|gradle/|gradlew|gradlew\.bat|config/|scripts/|\.agents/skills/quality-devops-engineer/|\.agents/|\.gemini/|GEMINI\.md|gemini\.md|README\.md)"; then
    TOUCHED_DEVOPS=true
    matched=true
  fi

  if [ "$matched" = false ]; then
    echo "ℹ️ Unclassified file: $file"
    TOUCHED_UNCLASSIFIED=true
  fi
done <<< "$CLEAN_CHANGED_FILES"

if [ "$TOUCHED_UNCLASSIFIED" = true ]; then
  echo "⚠️ Unclassified files detected; advisory review also considers Fullstack and Quality & DevOps."
  TOUCHED_DEVOPS=true
  TOUCHED_FULLSTACK=true
fi

if [ "$TOUCHED_FULLSTACK" = true ]; then
  echo "-> Touched domain: Fullstack Architecture & UI"
  validate_agent_approval "fullstack-architect|Fullstack Architect" "Fullstack Architecture & UI" "Consider adding a short technical review note." || true
fi

if [ "$TOUCHED_SCRAPER" = true ]; then
  echo "-> Touched domain: Scraper & Security"
  validate_agent_approval "scraper-security-engineer|Scraper & Security" "Scraper & Security" "Consider adding a short technical review note." || true
fi

if [ "$TOUCHED_DEVOPS" = true ]; then
  echo "-> Touched domain: Quality & DevOps"
  validate_agent_approval "quality-devops-engineer|Quality & DevOps" "Quality & DevOps" "Consider adding a short technical review note." || true
fi

if [ -n "$MISSING_APPROVALS" ]; then
  echo "::warning::Thematic review notes are missing or incomplete; this is advisory only and does not block PR creation or CI."
  printf '%b\n' "$MISSING_APPROVALS"
  echo "Add specialist notes when useful. Required CI, build, lint, security, and test checks remain authoritative."
else
  echo "✅ Thematic review notes are present for the detected domains."
fi

exit 0
