#!/usr/bin/env bash
# ==============================================================================
# scripts/validate-skills.sh
# Deterministic Offline Validation for AI Specialist Skills, Directives & PR Gate
#
# Enforces:
#  1. YAML Frontmatter presence, name alignment, and non-empty description
#  2. Mandatory 8 sections in all specialist skills (fullstack, scraper, quality)
#  3. Standard decision tokens (FINDING, NO_FINDING, NOT_APPLICABLE, NOT_VERIFIED)
#  4. Valid relative markdown file paths (no broken links, no file:// URIs)
#  5. Complete absence of banned words and obsolete tech claims
#
# Returns:
#  0 on 100% compliance
#  1 on any diagnostic violation (fail-closed)
# ==============================================================================

set -euo pipefail

# Determine repository root
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

VERBOSE=false
for arg in "$@"; do
  case "$arg" in
    -v|--verbose) VERBOSE=true ;;
    -h|--help)
      echo "Usage: $0 [-v|--verbose] [-h|--help]"
      echo "Validates AI specialist skills, markdown links, and obsolete patterns."
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
echo "      INGATLAN KERESO — OFFLINE DETERMINISTIC SKILLS & DIRECTIVES VALIDATOR   "
echo "=============================================================================="
echo "Working directory: $REPO_ROOT"
echo ""

# ------------------------------------------------------------------------------
# 1. Frontmatter Validation
# ------------------------------------------------------------------------------
echo "--- [1/5] Validating YAML Frontmatter in Specialist Skills ---"

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

  # Find line number of closing '---'
  closing_line=$(awk 'NR>1 && /^---[[:space:]]*$/ {print NR; exit}' "$skill_file")
  if [ -z "$closing_line" ]; then
    log_fail "Frontmatter is not closed with a second '---'" "$skill_file" 1
    continue
  fi

  # Extract name and description from frontmatter
  name_val=$(awk -v end_line="$closing_line" 'NR>1 && NR<end_line && /^name:[[:space:]]*/ {sub(/^name:[[:space:]]*/, ""); print; exit}' "$skill_file" | tr -d '\r')
  desc_val=$(awk -v end_line="$closing_line" 'NR>1 && NR<end_line && /^description:[[:space:]]*/ {sub(/^description:[[:space:]]*/, ""); print; exit}' "$skill_file" | tr -d '\r')

  # Clean leading/trailing quotes if present
  name_val=$(echo "$name_val" | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")

  if [ "$name_val" != "$expected_name" ]; then
    log_fail "Frontmatter name mismatch: expected '$expected_name', found '$name_val'" "$skill_file" 2
  else
    log_pass "Skill '$expected_name' has valid matching name property"
  fi

  if [ -z "$desc_val" ]; then
    log_fail "Frontmatter description is missing or empty" "$skill_file" 3
  else
    log_pass "Skill '$expected_name' has non-empty description"
  fi
done

# ------------------------------------------------------------------------------
# 2. Mandatory 8 Sections Validation
# ------------------------------------------------------------------------------
echo ""
echo "--- [2/5] Validating Mandatory 8 Sections in Specialist Skills ---"

MANDATORY_SECTIONS=(
  "Purpose & Activation|Cél és [aA]ktiválás"
  "Scope & Non-Goals|Hatókör és [nN]em-[cC]élok"
  "Required Workflow|Kötelező [mM]unkafolyamat"
  "Review Checklist|Felülvizsgálati [eE]llenőrzőlista"
  "Evidence & Output|Bizonyítékok és [kK]imenet"
  "Standard Decision Semantics|Döntési [sS]zemantika"
  "Stop Conditions|Megállási [fF]eltételek"
  "Completion Criteria|Befejezési [kK]ritériumok"
)

for dir in "${SKILL_DIRS[@]}"; do
  skill_file="$dir/SKILL.md"
  if [ ! -f "$skill_file" ]; then continue; fi

  for sec_pat in "${MANDATORY_SECTIONS[@]}"; do
    sec_name="${sec_pat%%|*}"
    if ! grep -qiE "^[#]+ +[0-9.]* *($sec_pat)" "$skill_file"; then
      log_fail "Missing mandatory section: '$sec_name'" "$skill_file"
    else
      log_pass "$dir contains section '$sec_name'"
    fi
  done
done

# ------------------------------------------------------------------------------
# 3. Decision Tokens Validation
# ------------------------------------------------------------------------------
echo ""
echo "--- [3/5] Validating Decision Tokens in Specialist Skills ---"

DECISION_TOKENS=("FINDING" "NO_FINDING" "NOT_APPLICABLE" "NOT_VERIFIED")

for dir in "${SKILL_DIRS[@]}"; do
  skill_file="$dir/SKILL.md"
  if [ ! -f "$skill_file" ]; then continue; fi

  for token in "${DECISION_TOKENS[@]}"; do
    if ! grep -q "$token" "$skill_file"; then
      log_fail "Missing required decision token '$token'" "$skill_file"
    else
      log_pass "$dir defines decision token '$token'"
    fi
  done
done

# ------------------------------------------------------------------------------
# 4. Markdown Links & Relative File Path Validation
# ------------------------------------------------------------------------------
echo ""
echo "--- [4/5] Validating Markdown Relative Links & File Paths ---"

DOC_FILES=(
  "README.md"
  "gemini.md"
  "GEMINI.md"
  ".github/pull_request_template.md"
  ".github/copilot-instructions.md"
)

for dir in "${SKILL_DIRS[@]}"; do
  if [ -f "$dir/SKILL.md" ]; then
    DOC_FILES+=("$dir/SKILL.md")
  fi
done

if [ -f "docs/architecture.md" ]; then
  DOC_FILES+=("docs/architecture.md")
fi

for doc in "${DOC_FILES[@]}"; do
  if [ ! -f "$doc" ]; then continue; fi
  doc_dir="$(dirname "$doc")"

  while IFS= read -r line_info; do
    if [ -z "$line_info" ]; then continue; fi
    line_num="${line_info%%:*}"
    link_match="${line_info#*:}"

    # Extract target inside (...)
    target=$(echo "$link_match" | sed -E 's/.*\]\(([^)]+)\)/\1/' | tr -d '\r')
    target="$(echo "$target" | xargs)"

    # Rule 1: Ban file:// local URIs
    if [[ "$target" =~ ^file:// ]]; then
      log_fail "Forbidden local URI scheme (file://): '$target'" "$doc" "$line_num"
      continue
    fi

    # Rule 2: Ban absolute Windows drive letters
    if [[ "$target" =~ ^[a-zA-Z]: ]]; then
      log_fail "Forbidden absolute drive path: '$target'" "$doc" "$line_num"
      continue
    fi

    # Rule 3: Skip external web URLs and email links
    if [[ "$target" =~ ^https?:// ]] || [[ "$target" =~ ^mailto: ]]; then
      continue
    fi

    # Rule 4: Skip in-page anchors
    if [[ "$target" =~ ^# ]]; then
      continue
    fi

    # Strip anchor fragment and query parameters
    clean_target=$(echo "$target" | sed -E 's/[#?].*//')
    if [ -z "$clean_target" ]; then
      continue
    fi

    # Candidate 1: relative to the document directory
    cand1="$doc_dir/$clean_target"
    # Candidate 2: relative to the repository root
    cand2="$clean_target"

    if [ ! -e "$cand1" ] && [ ! -e "$cand2" ]; then
      log_fail "Broken relative link target: '$target' (resolved as '$cand1' or '$cand2')" "$doc" "$line_num"
    else
      log_pass "$doc:$line_num valid relative link '$clean_target'"
    fi
  done < <(grep -n -o -E '\[[^]]*\]\([^)]+\)' "$doc" || true)
done

# ------------------------------------------------------------------------------
# 5. Banned Words & Obsolete Claims Scanner
# ------------------------------------------------------------------------------
echo ""
echo "--- [5/5] Scanning for Banned Words and Obsolete Tech Claims ---"

# 5a. file:// anywhere in markdown files
while IFS= read -r match; do
  if [ -n "$match" ]; then
    f=$(echo "$match" | cut -d: -f1)
    l=$(echo "$match" | cut -d: -f2)
    log_fail "Banned token 'file://' found in markdown file" "$f" "$l"
  fi
done < <(grep -rnI 'file://' README.md gemini.md GEMINI.md .github/ .agents/skills/ docs/ 2>/dev/null || true)

# 5b. Non-existent workflow pr-guard.yml
while IFS= read -r match; do
  if [ -n "$match" ]; then
    f=$(echo "$match" | cut -d: -f1)
    l=$(echo "$match" | cut -d: -f2)
    log_fail "Banned reference 'pr-guard.yml' (must use 'pr-review.yml')" "$f" "$l"
  fi
done < <(grep -rnI 'pr-guard\.yml' README.md gemini.md GEMINI.md .github/ .agents/skills/ docs/ 2>/dev/null || true)

# 5c. Obsolete class WebDashboardHtml
while IFS= read -r match; do
  if [ -n "$match" ]; then
    f=$(echo "$match" | cut -d: -f1)
    l=$(echo "$match" | cut -d: -f2)
    log_fail "Banned reference 'WebDashboardHtml' (class decommissioned)" "$f" "$l"
  fi
done < <(grep -rnI 'WebDashboardHtml' README.md gemini.md GEMINI.md .github/ .agents/skills/ .gemini/ src/ 2>/dev/null || true)

# 5d. Obsolete Maven commands (must use Gradle: ./gradlew)
while IFS= read -r match; do
  if [ -n "$match" ]; then
    f=$(echo "$match" | cut -d: -f1)
    l=$(echo "$match" | cut -d: -f2)
    log_fail "Banned command 'mvn' (project uses Gradle: './gradlew')" "$f" "$l"
  fi
done < <(grep -rnI -E '(mvn |mvn\t|mvn test|mvn-compile)' README.md gemini.md GEMINI.md .github/ .agents/skills/ .gemini/ docs/ 2>/dev/null || true)

# 5e. Obsolete persistence engines (SQLite & Nitrite) in active configurations and prompts
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
  if [ -f "$target" ]; then
    # Scan for sqlite (case-insensitive)
    while IFS= read -r match; do
      if [ -n "$match" ]; then
        l=$(echo "$match" | cut -d: -f1)
        content=$(echo "$match" | cut -d: -f2-)
        # Allow historical references if line explicitly notes retirement/deprecation
        if echo "$content" | grep -qiE "(retired|deprecated|historical|phased out|removed|legacy)"; then
          continue
        fi
        log_fail "Banned term 'sqlite' found in active instruction" "$target" "$l"
      fi
    done < <(grep -n -i 'sqlite' "$target" || true)

    # Scan for nitrite (case-insensitive)
    while IFS= read -r match; do
      if [ -n "$match" ]; then
        l=$(echo "$match" | cut -d: -f1)
        content=$(echo "$match" | cut -d: -f2-)
        if echo "$content" | grep -qiE "(retired|deprecated|historical|phased out|removed|legacy)"; then
          continue
        fi
        log_fail "Banned term 'nitrite' found in active instruction" "$target" "$l"
      fi
    done < <(grep -n -i 'nitrite' "$target" || true)
  fi
done

# 5f. Verify that production Java source and Gradle build files have zero SQLite/Nitrite
while IFS= read -r match; do
  if [ -n "$match" ]; then
    f=$(echo "$match" | cut -d: -f1)
    l=$(echo "$match" | cut -d: -f2)
    log_fail "Banned persistence keyword found in source code / build script" "$f" "$l"
  fi
done < <(grep -rniI -E '(sqlite|nitrite)' src/main/ build.gradle.kts 2>/dev/null || true)

# ------------------------------------------------------------------------------
# Validation Summary & Exit Code
# ------------------------------------------------------------------------------
echo ""
echo "=============================================================================="
if [ "$ERRORS" -gt 0 ]; then
  echo "❌ VALIDATION FAILED: $ERRORS diagnostic violation(s) detected."
  echo "=============================================================================="
  exit 1
else
  echo "✅ VALIDATION PASSED: 100% of skills, instructions, and links are compliant."
  echo "=============================================================================="
  exit 0
fi
