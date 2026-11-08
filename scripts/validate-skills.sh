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
echo "--- [1/7] Validating YAML frontmatter in specialist skills ---"

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
echo "--- [2/7] Validating the eight mandatory English-language sections ---"

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
echo "--- [3/7] Validating standard decision tokens ---"

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
echo "--- [4/7] Validating relative Markdown links and file paths ---"

DOC_FILES=(
  "README.md"
  "AGENTS.md"
  "gemini.md"
  "GEMINI.md"
  "CONTRIBUTING.md"
  "SECURITY.md"
  ".github/pull_request_template.md"
  ".github/copilot-instructions.md"
)

for dir in "${SKILL_DIRS[@]}"; do
  [ -f "$dir/SKILL.md" ] && DOC_FILES+=("$dir/SKILL.md")
done
[ -f "docs/architecture.md" ] && DOC_FILES+=("docs/architecture.md")
[ -f "docs/pr-compliance.md" ] && DOC_FILES+=("docs/pr-compliance.md")
[ -f "docs/PROJECT.md" ] && DOC_FILES+=("docs/PROJECT.md")
[ -f "frontend/AGENTS.md" ] && DOC_FILES+=("frontend/AGENTS.md")
[ -f "src/main/java/ingatlanok/AGENTS.md" ] && DOC_FILES+=("src/main/java/ingatlanok/AGENTS.md")
[ -f "docs/architecture/type-safety/allowlist.md" ] && DOC_FILES+=("docs/architecture/type-safety/allowlist.md")
[ -f "docs/architecture/type-safety/audit.md" ] && DOC_FILES+=("docs/architecture/type-safety/audit.md")

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
echo "--- [5/7] Scanning for banned words and obsolete technology claims ---"

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

while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Forbidden Lombok annotation/import (@Data, @EqualsAndHashCode, @ToString, @SneakyThrows) found in Java source" "$f" "$l"
done < <(grep -rnI -E '^[[:space:]]*(import[[:space:]]+lombok\.(Data|EqualsAndHashCode|ToString|SneakyThrows);|@(lombok\.)?(Data|EqualsAndHashCode|ToString|SneakyThrows)\b)' src/ 2>/dev/null || true)

# ------------------------------------------------------------------------------
# 6. Architectural guardrails & frontend OpenAPI derivation
# ------------------------------------------------------------------------------
echo ""
echo "--- [6/7] Validating architectural guardrails & frontend OpenAPI derivation ---"

# 6.1 Frontend OpenAPI type derivation (no manual DTO duplication in frontend/src/types/)
for type_file in frontend/src/types/*.ts; do
  [ -f "$type_file" ] || continue
  filename="$(basename "$type_file")"
  if [ "$filename" = "generated-api.ts" ]; then
    continue
  fi
  if ! grep -q "components\['schemas'\]" "$type_file"; then
    log_fail "Frontend type file must derive schemas from generated-api.ts (missing components['schemas']): $type_file" "$type_file"
  else
    log_pass "Frontend type file '$filename' derives schemas from generated-api.ts"
  fi
done

# 6.2 Single-point SSRF isolation: Jsoup.connect must only occur in HttpFetcher.java
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  if [[ "$f" != *"HttpFetcher.java" ]]; then
    log_fail "SSRF violation: direct Jsoup.connect call outside HttpFetcher.java" "$f" "$l"
  else
    log_pass "Jsoup.connect isolated in $f:$l"
  fi
done < <(grep -rnI 'Jsoup\.connect' src/main/java/ 2>/dev/null || true)

# 6.3 Virtual thread non-pinning: no synchronized anywhere in src/main/java/ingatlanok/
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Virtual thread pinning violation: 'synchronized' keyword found in Java source" "$f" "$l"
done < <(grep -rnI --include="*.java" -E '\bsynchronized\b' src/main/java/ingatlanok/ 2>/dev/null || true)

# 6.4 Persistence encapsulation: no repository imports outside persistence
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Persistence encapsulation violation: direct repository import outside persistence package" "$f" "$l"
done < <(grep -rnI 'import ingatlanok\.persistence\.repository\.' src/main/java/ingatlanok/core/ src/main/java/ingatlanok/portal/ src/main/java/ingatlanok/web/ src/main/java/ingatlanok/presentation/ src/main/java/ingatlanok/model/ 2>/dev/null || true)

# 6.5 Forbidden direct window.open calls in frontend components
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Direct window.open is forbidden in frontend components (use openExternalUrl from src/utils/formatters.ts)" "$f" "$l"
done < <(grep -rnI -E '\bwindow\.open\b' frontend/src/components/ 2>/dev/null || true)

# 6.6 Exception-free unidirectional layer flow: lower layers never import presentation
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Layer violation: lower layer imports presentation package" "$f" "$l"
done < <(grep -rnI 'import ingatlanok\.presentation\.' src/main/java/ingatlanok/core/ src/main/java/ingatlanok/persistence/ src/main/java/ingatlanok/portal/ src/main/java/ingatlanok/model/ src/main/java/ingatlanok/common/ 2>/dev/null || true)

# 6.7 No test-mode branching in production: app.test-mode restricted strictly to HttpFetcher.java
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  if [[ "$f" != *"HttpFetcher.java" ]]; then
    log_fail "Test-mode leak: 'app.test-mode' found outside HttpFetcher.java in production code" "$f" "$l"
  else
    log_pass "app.test-mode isolated in $f:$l"
  fi
done < <(grep -rnI 'app\.test-mode' src/main/java/ 2>/dev/null || true)

# 6.8 E2E TypeScript coverage enforcement: frontend/tsconfig.json must include e2e directory
TSCONFIG="frontend/tsconfig.json"
if [ -f "$TSCONFIG" ]; then
  if ! grep -q '"e2e"' "$TSCONFIG"; then
    log_fail "tsconfig.json must include 'e2e' in the include array to ensure full TypeScript checking" "$TSCONFIG"
  else
    log_pass "frontend/tsconfig.json includes 'e2e' directory"
  fi
fi

# 6.9 Frontend multi-source domain invariant AST rule in eslint.config.js
ESLINT_CFG="frontend/eslint.config.js"
if [ -f "$ESLINT_CFG" ]; then
  if ! grep -q "amountHuf" "$ESLINT_CFG" || ! grep -q "pricePerSquareMeter" "$ESLINT_CFG"; then
    log_fail "eslint.config.js must enforce no-restricted-syntax on amountHuf and pricePerSquareMeter to preserve multi-source domain invariants" "$ESLINT_CFG"
  else
    log_pass "frontend/eslint.config.js enforces multi-source AST rules"
  fi
fi

# 6.10 Validate allowlist entries in docs/architecture/type-safety/allowlist.md
ALLOWLIST_FILE="docs/architecture/type-safety/allowlist.md"
if [ ! -f "$ALLOWLIST_FILE" ]; then
  log_fail "Type-safety allowlist file missing: $ALLOWLIST_FILE" "$ALLOWLIST_FILE"
else
  allowlist_count=0
  while IFS= read -r line; do
    [ -n "$line" ] || continue
    id=$(echo "$line" | awk -F'|' '{print $2}' | tr -d '`' | xargs)
    scope=$(echo "$line" | awk -F'|' '{print $3}' | tr -d '`' | xargs)
    [ -n "$id" ] && [ -n "$scope" ] || continue
    allowlist_count=$((allowlist_count + 1))
    if [ ! -e "$scope" ]; then
      log_fail "Allowlist entry '$id' specifies non-existent scope: '$scope'" "$ALLOWLIST_FILE"
    else
      log_pass "Allowlist entry '$id' maps to valid file scope '$scope'"
    fi
  done < <(grep -E '^\s*\|\s*`?ALLOW-[A-Z0-9_-]+`?\s*\|' "$ALLOWLIST_FILE" || true)

  if [ "$allowlist_count" -eq 0 ]; then
    log_fail "Type-safety allowlist contains no valid entries in $ALLOWLIST_FILE" "$ALLOWLIST_FILE"
  fi
fi

# 6.11 Zero-tolerance check: @SuppressWarnings in src/main/java/
suppress_warnings_found=0
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  suppress_warnings_found=$((suppress_warnings_found + 1))
  log_fail "Type-safety bypass: '@SuppressWarnings' is forbidden in production Java code" "$f" "$l"
done < <(grep -rnI --include="*.java" -E '@SuppressWarnings\b' src/main/java/ 2>/dev/null || true)
if [ "$suppress_warnings_found" -eq 0 ]; then
  log_pass "Zero @SuppressWarnings annotations found in src/main/java/"
fi

# 6.12 Zero-tolerance check: Class.forName in src/main/java/
class_for_name_found=0
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  class_for_name_found=$((class_for_name_found + 1))
  log_fail "Type-safety bypass: 'Class.forName' reflective loading is forbidden in production Java code" "$f" "$l"
done < <(grep -rnI --include="*.java" -E 'Class\.forName\b' src/main/java/ 2>/dev/null || true)
if [ "$class_for_name_found" -eq 0 ]; then
  log_pass "Zero Class.forName reflection calls found in src/main/java/"
fi

# 6.13 Zero-tolerance check: @ts-ignore, @ts-nocheck, @ts-expect-error in frontend/src/ and frontend/e2e/
ts_suppress_found=0
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  ts_suppress_found=$((ts_suppress_found + 1))
  log_fail "Type-safety bypass: TypeScript compiler suppression is forbidden in frontend code" "$f" "$l"
done < <(grep -rnI --include="*.ts" --include="*.tsx" -E '@ts-(ignore|nocheck|expect-error)\b' frontend/src/ frontend/e2e/ 2>/dev/null || true)
if [ "$ts_suppress_found" -eq 0 ]; then
  log_pass "Zero TypeScript compiler suppressions found in frontend/src/ and frontend/e2e/"
fi

# 6.14 Verify .github/CODEOWNERS and non-tampering directives
CODEOWNERS_FILE=".github/CODEOWNERS"
if [ ! -f "$CODEOWNERS_FILE" ]; then
  log_fail "Missing required CODEOWNERS file: $CODEOWNERS_FILE" "$CODEOWNERS_FILE"
elif ! grep -q "@Luxeonn" "$CODEOWNERS_FILE"; then
  log_fail "CODEOWNERS file must assign repository owner @Luxeonn" "$CODEOWNERS_FILE"
else
  log_pass ".github/CODEOWNERS exists and assigns @Luxeonn"
fi

for agent_guide in "AGENTS.md" "frontend/AGENTS.md" "src/main/java/ingatlanok/AGENTS.md"; do
  if [ ! -f "$agent_guide" ]; then
    log_fail "Agent guide missing: $agent_guide" "$agent_guide"
  elif ! grep -qi "Non-Tampering" "$agent_guide"; then
    log_fail "Agent guide '$agent_guide' must contain Non-Tampering governance rules" "$agent_guide"
  else
    log_pass "Agent guide '$agent_guide' contains Non-Tampering governance rules"
  fi
done

# ------------------------------------------------------------------------------
# 7. PR template compliance, banned review tokens & version alignment
# ------------------------------------------------------------------------------
echo ""
echo "--- [7/7] Validating PR template compliance, review tokens & version alignment ---"

PR_TEMPLATE=".github/pull_request_template.md"
if [ ! -f "$PR_TEMPLATE" ]; then
  log_fail "Missing PR template: $PR_TEMPLATE" "$PR_TEMPLATE"
else
  PR_SECTIONS=(
    "Miért készült"
    "Mit módosít"
    "Acceptance Criteria Traceability"
    "Tesztelés és Ellenőrzés"
    "Thematic Agent Approval Gate"
  )
  for sec in "${PR_SECTIONS[@]}"; do
    if ! grep -qiE "^##.*${sec}" "$PR_TEMPLATE"; then
      log_fail "PR template missing section: '$sec'" "$PR_TEMPLATE"
    else
      log_pass "PR template contains section '$sec'"
    fi
  done
  if grep -q "\[OK\]" "$PR_TEMPLATE"; then
    log_fail "PR template contains forbidden/legacy token '[OK]' (use [NO_FINDING] or [NOT_APPLICABLE])" "$PR_TEMPLATE"
  else
    log_pass "PR template uses standard decision tokens"
  fi
fi

# 7.2 No legacy [OK]/[NOK] in approval gate instructions
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Legacy review token '[OK]' found in governance instruction" "$f" "$l"
done < <(grep -rnI -E '^\s*[-*]\s*\[[ xX]\]\s*.*\[OK\]' .github/ .agents/skills/ docs/ 2>/dev/null || true)

# 7.3 Spring Boot version alignment: no obsolete 4.1.1 in active instructions
while IFS= read -r match; do
  [ -n "$match" ] || continue
  f=$(echo "$match" | cut -d: -f1)
  l=$(echo "$match" | cut -d: -f2)
  log_fail "Obsolete Spring Boot version '4.1.1' found (expected 4.1.2 per build.gradle.kts)" "$f" "$l"
done < <(grep -rnI 'Spring Boot 4\.1\.1' README.md AGENTS.md GEMINI.md gemini.md SECURITY.md docs/ .agents/skills/ .github/ 2>/dev/null || true)

# 7.4 Execute PR compliance unit tests
if command -v node >/dev/null 2>&1; then
  if node --test scripts/test-verify-pr-compliance.mjs >/dev/null 2>&1; then
    log_pass "PR compliance test suite passed (19 unit tests)"
  else
    log_fail "PR compliance test suite failed (scripts/test-verify-pr-compliance.mjs)" "scripts/test-verify-pr-compliance.mjs"
  fi
fi

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
