#!/usr/bin/env bash
set -eo pipefail

echo "=== 🛡️ Thematic Agent Approval Gate Ellenőrzése ==="

IS_PR="${IS_PR:-true}"
BASE_REF="${BASE_REF:-develop}"
ACTOR="${ACTOR:-${GITHUB_ACTOR:-}}"
BRANCH="${BRANCH:-${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}}"
BODY="${BODY:-}"
CHANGED_FILES="${CHANGED_FILES:-}"

if [ "$IS_PR" != "true" ]; then
  echo "ℹ️ Kézi ágfuttatás (nincs kapcsolt PR): Thematic Agent Approval Gate leírás-ellenőrzés kihagyva."
  exit 0
fi

if [[ "$ACTOR" == "dependabot[bot]" || "$ACTOR" == "dependabot" || "$BRANCH" =~ ^dependabot\/ ]]; then
  echo "🤖 Dependabot automatikus függőségfrissítés észlelve ($BRANCH / $ACTOR)."
  echo "✅ Thematic Agent Approval Gate: Dependabot bot által nyitott frissítés automatikusan jóváhagyva."
  exit 0
fi

if [ -z "$CHANGED_FILES" ]; then
  if git rev-parse --verify "origin/${BASE_REF}" >/dev/null 2>&1; then
    CHANGED_FILES=$(git diff --name-only "origin/${BASE_REF}...HEAD") || {
      echo "::error::A 'git diff origin/${BASE_REF}...HEAD' lekérdezése elbukott!"
      exit 1
    }
  elif git rev-parse --verify HEAD~1 >/dev/null 2>&1; then
    CHANGED_FILES=$(git diff --name-only HEAD~1) || {
      echo "::error::A 'git diff HEAD~1' lekérdezése elbukott!"
      exit 1
    }
  else
    echo "::error::Nem található érvényes git ref a módosított fájlok lekérdezéséhez (origin/${BASE_REF} vagy HEAD~1 hiányzik)!"
    exit 1
  fi
fi

CLEAN_CHANGED_FILES=$(echo "$CHANGED_FILES" | sed '/^[[:space:]]*$/d')
if [ -z "$CLEAN_CHANGED_FILES" ]; then
  echo "::error::A módosított fájlok listája üres (0 fájl észlelve). Fail-closed védelem aktiválva."
  exit 1
fi

echo "Módosított fájlok:"
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
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: Nincs bejelölve ([x]) a jóváhagyási négyzet. ${guidance}"
    return 1
  fi

  if echo "$match_line" | grep -qiE "(\[[^]]*\/[^]]*\]|1-sentence rationale|\[TBD\])"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: A jóváhagyási sor kitöltetlen sablonszöveget tartalmaz: '${match_line}'"
    return 1
  fi

  local after_colon
  after_colon=$(echo "$match_line" | sed -E "s/^[[:space:]]*-[[:space:]]*\[[xX]\][[:space:]]*.*(${role_pattern})[^:]*:[[:space:]]*//")

  if [ -z "$after_colon" ] || [ "$after_colon" = "$match_line" ]; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: Hiányzik a kettőspont és a döntési státusz: '${match_line}'"
    return 1
  fi

  if echo "$after_colon" | grep -qiE "^[[:space:]]*(\[FINDING\]|\bFINDING\b|\[NOK\]|\bNOK\b|\[NOT_VERIFIED\]|\bNOT_VERIFIED\b)"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: Elutasító vagy nem igazolt döntési token található ('${match_line}'). Csak pozitív döntés ([NO_FINDING], [NOT_APPLICABLE], [OK], [N/A]) engedélyezett!"
    return 1
  fi

  if ! echo "$after_colon" | grep -qiE "^[[:space:]]*(\[NO_FINDING\]|\bNO_FINDING\b|\[NOT_APPLICABLE\]|\bNOT_APPLICABLE\b|\[OK\]|\bOK\b|\[N/A\]|\bN/A\b)"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: Érvénytelen vagy hiányzó jóváhagyó token ('${match_line}'). Csak [NO_FINDING], [NOT_APPLICABLE], [OK], [N/A] engedélyezett!"
    return 1
  fi

  local after_token
  after_token=$(echo "$after_colon" | sed -E "s/^[[:space:]]*(\[NO_FINDING\]|NO_FINDING|\[NOT_APPLICABLE\]|NOT_APPLICABLE|\[OK\]|OK|\[N\/A\]|N\/A)[[:space:]]*//I")
  local rationale
  rationale=$(echo "$after_token" | sed -E "s/^[[:space:]]*[–—:-]+[[:space:]]*//")
  local clean_rationale
  clean_rationale=$(echo "$rationale" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')

  if [ -z "$clean_rationale" ]; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: Hiányzik a szakmai indoklás (üres indoklás tiltott). ${guidance}"
    return 1
  fi

  if echo "$clean_rationale" | grep -qiE "^(\[1-sentence rationale\]|1-sentence rationale|\[TBD\]|TBD)$"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: A szakmai indoklás sablonszöveg maradt: '${clean_rationale}'"
    return 1
  fi

  if [ "${#clean_rationale}" -lt 5 ]; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ❌ ${display_name}: A szakmai indoklás túl rövid (minimum 5 karakter szükséges): '${clean_rationale}'"
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

  # 1. Fullstack Architecture & UI
  if echo "$file" | grep -qE "^(src/main/java/ingatlanok/(core|presentation|web|persistence|model)/|src/main/java/ingatlanok/Main\.java|src/main/java/ingatlanok/IngatlanApplication\.java|frontend/|build\.gradle\.kts|settings\.gradle\.kts|docs/architecture\.md|data/|\.agents/skills/fullstack-architect/)"; then
    TOUCHED_FULLSTACK=true
    matched=true
  fi

  # 2. Scraper Motorok & Alkalmazásbiztonság
  if echo "$file" | grep -qE "^(src/main/java/ingatlanok/(portal|scraper|common)/|tools/|src/main/java/ingatlanok/common/(SidecarCliExecutor|BrowserUtils|HttpFetcher)\.java|\.agents/skills/scraper-security-engineer/)"; then
    TOUCHED_SCRAPER=true
    matched=true
  fi

  # 3. Minőségbiztosítás, Tesztelés & DevOps/CI
  if echo "$file" | grep -qE "^(\.github/|Dockerfile.*|docker-compose.*|src/test/|frontend/.*test.*|frontend/package.*\.json|frontend/playwright.*|frontend/vite.*|build\.gradle\.kts|settings\.gradle\.kts|gradle/|gradlew|gradlew\.bat|config/|scripts/|\.agents/skills/quality-devops-engineer/|\.agents/|\.gemini/|GEMINI\.md|gemini\.md|README\.md)"; then
    TOUCHED_DEVOPS=true
    matched=true
  fi

  if [ "$matched" = false ]; then
    echo "ℹ️ Nem besorolt fájl észlelve: $file"
    TOUCHED_UNCLASSIFIED=true
  fi
done <<< "$CLEAN_CHANGED_FILES"

if [ "$TOUCHED_UNCLASSIFIED" = true ]; then
  echo "⚠️ Nem besorolt fájl(ok) észlelve! Konzervatív fallback aktiválva: Quality & DevOps és Fullstack felülvizsgálat kötelező."
  TOUCHED_DEVOPS=true
  TOUCHED_FULLSTACK=true
fi

if [ "$TOUCHED_FULLSTACK" = true ]; then
  echo "-> Érintett téma: Fullstack Architektúra, Perzisztencia vagy UI (fullstack-architect szükséges)"
  validate_agent_approval "fullstack-architect|Fullstack Architect" \
    "🏛️ Fullstack Architect Agent (\`fullstack-architect\`)" \
    "Rendszerarchitektúra, Spring Data JPA perzisztencia, React 19 UI vagy REST szerződés jóváhagyása szükséges." || true
fi

if [ "$TOUCHED_SCRAPER" = true ]; then
  echo "-> Érintett téma: Scraper Motorok és Alkalmazásbiztonság (scraper-security-engineer szükséges)"
  validate_agent_approval "scraper-security-engineer|Scraper & Security" \
    "🛡️ Scraper & Security Specialist (\`scraper-security-engineer\`)" \
    "Scraper megbízhatóság, 0-crash hibatűrés, WAF/bot védelem vagy AppSec hardening jóváhagyása szükséges." || true
fi

if [ "$TOUCHED_DEVOPS" = true ]; then
  echo "-> Érintett téma: Minőségbiztosítás, DevOps vagy CI/CD (quality-devops-engineer szükséges)"
  validate_agent_approval "quality-devops-engineer|Quality & DevOps" \
    "⚙️ Quality & DevOps Specialist (\`quality-devops-engineer\`)" \
    "Tesztlefedettség (80%+), determinista offline tesztek, Docker vagy CI/CD pipeline jóváhagyása szükséges." || true
fi

if [ -n "$MISSING_APPROVALS" ]; then
  echo -e "::error::Hiányzó vagy érvénytelen Tematikus Agent Jóváhagyás(ok)! A PR nem engedélyezett a megfelelő szakértői ágens igazolása nélkül:${MISSING_APPROVALS}"
  echo ""
  echo "Kérjük, pótold a hiányzó vagy elutasított jóváhagyást a PR leírásának 'Thematic Agent Approval Gate' blokkjában!"
  exit 1
fi

echo "✅ Minden érintett szakterület rendelkezik a kijelölt Agent jóváhagyásával."
