#!/usr/bin/env bash
# ==============================================================================
# scripts/validate-skills.sh
# Deterministic offline validation for AI specialist skills and directives.
#
# Enforces:
#  1. YAML frontmatter presence, matching skill names, and non-empty descriptions
#  2. Eight mandatory English-language sections in all specialist skills
#  3. Standard decision tokens (FINDING, NO_FINDING, NOT_APPLICABLE, NOT_VERIFIED)
#  4. Valid relative Markdown links (no broken links or file:// URIs)
#  5. No banned words or obsolete technology claims
#
# Exit codes:
#  0 when all checks pass
#  1 when any diagnostic violation is found (fail closed)
# ==============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

VERBOSE=false
for arg in "$@"; do
  case "$arg" in
    -v|--verbose) VERBOSE=true ;;
    -h|--help)
      echo "Usage: $0 [-v|--verbose] [-h|--help]"
      echo "Validates AI specialist skills, Markdown links, and obsolete patterns."
      exit 0
      ;;
  esac
done

ERRORS=0

log_fail() {
  local msg="$1"
  local file="${2:-}"
  local line="${3:-}"
  if [ -n "$file" ] && [ -n "$line" ]; then
    if [ -n "${GITHUB_ACTIONS:-}" ]; then
      echo "::error file=${file},line=${line}::${msg}"
    fi
    echo "❌ [FAIL] ${file}:${line}: ${msg}"
  elif [ -n "$file" ]; then
    if [ -n "${GITHUB_ACTIONS:-}" ]; then
      echo "::error file=${file}::${msg}"
    fi
    echo "❌ [FAIL] ${file}: ${msg}"
  else
    echo "❌ [FAIL] ${msg}"
  fi
  ERRORS=$((ERRORS + 1))
}

log_pass() {
  if [ "$VERBOSE" = true ]; then
    echo "✅ [PASS] $1"
  fi
}

echo "=============================================================================="
echo "      PROPERTY SEARCH — OFFLINE DETERMINISTIC SKILLS & DIRECTIVES VALIDATOR   "
echo "=============================================================================="
echo "Working directory: $REPO_ROOT"
echo ""

# ------------------------------------------------------------------------------
# 1. Frontmatter validation
# ------------------------------------------------------------------------------
echo "--- [1/5] Validating YAML frontmatter in specialist skills ---"

SKILL_DIRS=(
  ".agents/skills/fullstack-architect"
  ".agents/skills/scraper-security-engineer"
  ".agents/skills/quality-devops-engineer"
)

for dir in "${SKILL_DIRS[@]}"; do
  skill_file="$dir/SKILL.md"
  expected_name="$(basename "$dir")"

  if [ ! -f "$skill_file" ]; then
    log_fail "Mandatory skill file does not exist: $skill_file" "$skill_file"
    continue
  fi

  first_line=$(head -n 1 "$skill_file" | tr -d '\r')
  if [ "$first_line" != "---" ]; then
    log_fail "Frontmatter must start with '---' on line 1 (found: '$first_line')" "$skill_file" 1
    continue
  fi

  closing_line=$(awk 'NR>1 && NR<=15 && /^---[[:space:]]*$/ {print NR; exit}' "$skill_file")
  if [ -z "$closing_line" ]; then
    log_fail "Frontmatter is not closed with a second '---'" "$skill_file" 1
    continue
  fi

  name_val=$(awk -v end_line="$closing_line" 'NR>1 && NR<end_line && /^name:[[:space:]]*/ {sub(/^name:[[:space:]]*/, ""); print; exit}' "$skill_file" | tr -d '\r')
  desc_val=$(awk -v end_line="$closing_line" 'NR>1 && NR<end_line && /^description:[[:space:]]*/ {sub(/^description:[[:space:]]*/, ""); print; exit}' "$skill_file" | tr -d '\r')
  name_val=$(echo "$name_val" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

  if [ "$name_val" != "$expected_name" ]; then
    log_fail "Frontmatter name mismatch: expected '$expected_name', found '$name_val'" "$skill_file" 2
  else
    log_pass "Skill '$expected_name' has a matching name property"
  fi

  if [ -z "$desc_val" ]; then
    log_fail "Frontmatter description is missing or empty" "$skill_file" 3
  else
    log_pass "Skill '$expected_name' has a non-empty description"
  fi
done

# ------------------------------------------------------------------------------
# 2. Mandatory English-language section validation
# ------------------------------------------------------------------------------
echo ""
echo "--- [2/5] Validating the eight mandatory English-language sections ---"

MANDATORY_SECTIONS=(
  "Purpose & Activation"
  "Scope & Non-Goals"
  "Required Workflow"
  "Review Checklist"
  "Evidence & Output"
  "Standard Decision Semantics"
  "Stop Conditions"
  "Completion Criteria"
)

for dir in "${SKILL_DIRS[@]}"; do
  skill_file="$dir/SKILL.md"
  [ -f "$skill_file" ] || continue

  for section in "${MANDATORY_SECTIONS[@]}"; do
    if ! grep -qiE "^[#]+ +[0-9.]* *${section//&/\\&}([[:space:]]*$|[[:space:]]*\\()" "$skill_file"; then
      log_fail "Missing mandatory English section: '$section'" "$skill_file"
    else
      log_pass "$dir contains section '$section'"
    fi
  done
done

# ------------------------------------------------------------------------------
# 3. Decision-token validation
# ------------------------------------------------------------------------------
echo ""
echo "--- [3/5] Validating standard decision tokens ---"

DECISION_TOKENS=("FINDING" "NO_FINDING" "NOT_APPLICABLE" "NOT_VERIFIED")
for dir in "${SKILL_DIRS[@]}"; do
  skill_file="$dir/SKILL.md"
  [ -f "$skill_file" ] || continue

  for token in "${DECISION_TOKENS[@]}"; do
    if ! grep -q "$token" "$skill_file"; then
      log_fail "Missing required decision token '$token'" "$skill_file"
    else
      log_pass "$dir defines decision token '$token'"
    fi
  done
done

# ------------------------------------------------------------------------------
# 4. Markdown links and relative file paths
# ------------------------------------------------------------------------------
echo ""
echo "--- [4/5] Validating relative Markdown links and file paths ---"

DOC_FILES=(
  "README.md"
  "gemini.md"
  "GEMINI.md"
  ".github/pull_request_template.md"
  ".github/copilot-instructions.md"
)

for dir in "${SKILL_DIRS[@]}"; do
  [ -f "$dir/SKILL.md" ] && DOC_FILES+=("$dir/SKILL.md")
done
[ -f "docs/architecture.md" ] && DOC_FILES+=("docs/architecture.md")

for doc in "${DOC_FILES[@]}"; do
  [ -f "$doc" ] || continue
  doc_dir="$(dirname "$doc")"

  while IFS= read -r line_info; do
    [ -n "$line_info" ] || continue
    line_num="${line_info%%:*}"
    link_match="${line_info#*:}"
    target=$(echo "$link_match" | sed -E 's/.*\]\(([^)]+)\)/\1/' | tr -d '\r' | xargs)

    if [[ "$target" =~ ^file:// ]]; then
      log_fail "Forbidden local URI scheme (file://): '$target'" "$doc" "$line_num"
      continue
    fi
    if [[ "$target" =~ ^[a-zA-Z]: ]]; then
      log_fail "Forbidden absolute drive path: '$target'" "$doc" "$line_num"
      continue
    fi
    if [[ "$target" =~ ^https?:// ]] || [[ "$target" =~ ^mailto: ]] || [[ "$target" =~ ^# ]]; then
      continue
    fi

    clean_target=$(echo "$target" | sed -E 's/[#?].*//')
    [ -n "$clean_target" ] || continue
    cand1="$doc_dir/$clean_target"
    cand2="$clean_target"
    if [ ! -e "$cand1" ] && [ ! -e "$cand2" ]; then
      log_fail "Broken relative link target: '$target' (resolved as '$cand1' or '$cand2')" "$doc" "$line_num"
    else
      log_pass "$doc:$line_num valid relative link '$clean_target'"
    fi
  done < <(grep -n -o -E '\[[^]]*\]\([^)]+\)' "$doc" || true)
done

# ------------------------------------------------------------------------------
# 5. Banned words and obsolete claims
# ------------------------------------------------------------------------------
echo ""
echo "--- [5/5] Scanning for banned words and obsolete technology claims ---"

while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Banned token 'file://' found in Markdown file" "$f" "$l"
done < <(grep -rnI 'file://' README.md gemini.md GEMINI.md .github/ .agents/skills/ docs/ 2>/dev/null || true)

while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Obsolete workflow reference 'pr-review.yml' (use 'pr-build.yml')" "$f" "$l"
done < <(grep -rnI 'pr-review\.yml' README.md gemini.md GEMINI.md .github/ .agents/skills/ docs/ 2>/dev/null || true)

while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Banned reference 'WebDashboardHtml' (class decommissioned)" "$f" "$l"
done < <(grep -rnI 'WebDashboardHtml' README.md gemini.md GEMINI.md .github/ .agents/skills/ .gemini/ src/ 2>/dev/null || true)

while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Banned command 'mvn' (use Gradle: './gradlew')" "$f" "$l"
done < <(grep -rnI -E '(mvn[[:space:]]|mvn test|mvn-compile)' README.md gemini.md GEMINI.md .github/ .agents/skills/ .gemini/ docs/ 2>/dev/null || true)

BANNED_PERSISTENCE_FILES=(
  ".agents/skills/fullstack-architect/SKILL.md"
  ".agents/skills/scraper-security-engineer/SKILL.md"
  ".agents/skills/quality-devops-engineer/SKILL.md"
  ".github/copilot-instructions.md"
  ".github/.pr_agent.toml"
  ".github/pull_request_template.md"
  ".gemini/commands/review-and-fix.toml"
  "README.md"
  "gemini.md"
  "GEMINI.md"
)

for target in "${BANNED_PERSISTENCE_FILES[@]}"; do
  [ -f "$target" ] || continue
  while IFS= read -r match; do
    [ -n "$match" ] || continue
    l=$(echo "$match" | cut -d: -f1)
    content=$(echo "$match" | cut -d: -f2-)
    echo "$content" | grep -qiE "(retired|deprecated|historical|phased out|removed|legacy)" && continue
    log_fail "Banned term 'sqlite' found in active instruction" "$target" "$l"
  done < <(grep -n -i 'sqlite' "$target" || true)

  while IFS= read -r match; do
    [ -n "$match" ] || continue
    l=$(echo "$match" | cut -d: -f1)
    content=$(echo "$match" | cut -d: -f2-)
    echo "$content" | grep -qiE "(retired|deprecated|historical|phased out|removed|legacy)" && continue
    log_fail "Banned term 'nitrite' found in active instruction" "$target" "$l"
  done < <(grep -n -i 'nitrite' "$target" || true)
done

while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Banned persistence keyword found in source code / build script" "$f" "$l"
done < <(grep -rniI -E '(sqlite|nitrite)' src/main/ build.gradle.kts 2>/dev/null || true)

echo ""
echo "=============================================================================="
if [ "$ERRORS" -gt 0 ]; then
  echo "❌ VALIDATION FAILED: $ERRORS diagnostic violation(s) detected."
  echo "=============================================================================="
  exit 1
else
  echo "✅ VALIDATION PASSED: all skills, instructions, and links comply."
  echo "=============================================================================="
  exit 0
fi
