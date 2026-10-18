#!/usr/bin/env bash
set -eo pipefail

echo "=== 🛡️ Thematic Agent Approval Gate Ellenőrzése ==="

IS_PR="${IS_PR:-true}"
BASE_REF="${BASE_REF:-develop}"
ACTOR="${ACTOR:-${GITHUB_ACTOR:-}}"
BRANCH="${BRANCH:-${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-}}}"
BODY="${BODY:-}"

if [ "$IS_PR" != "true" ]; then
  echo "ℹ️ Kézi ágfuttatás (nincs kapcsolt PR): Thematic Agent Approval Gate leírás-ellenőrzés kihagyva."
  exit 0
fi

if [[ "$ACTOR" == "dependabot[bot]" || "$ACTOR" == "dependabot" || "$BRANCH" =~ ^dependabot\/ ]]; then
  echo "🤖 Dependabot automatikus függőségfrissítés észlelve ($BRANCH / $ACTOR)."
  echo "✅ Thematic Agent Approval Gate: Dependabot bot által nyitott frissítés automatikusan jóváhagyva."
  exit 0
fi

CHANGED_FILES=$(git diff --name-only "origin/${BASE_REF}...HEAD" 2>/dev/null || git diff --name-only HEAD~1 2>/dev/null || true)
echo "Módosított fájlok:"
echo "$CHANGED_FILES"
echo "----------------------------------------"

MISSING_APPROVALS=""

# 1. Fullstack Architecture & UI (fullstack-architect)
if echo "$CHANGED_FILES" | grep -qE "src/main/java/ingatlanok/(core|presentation|web|persistence|model)/|Main\.java|frontend/|WebDashboardHtml\.java|static/"; then
  echo "-> Érintett téma: Fullstack Architektúra, Perzisztencia vagy UI (fullstack-architect szükséges)"
  if ! echo "$BODY" | grep -qiE "\[x\][[:space:]]*.*(fullstack-architect|Fullstack Architect)"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - 🏛️ Fullstack Architect Agent (\`fullstack-architect\`): Rendszerarchitektúra, Spring Data JPA perzisztencia, React 19 UI vagy REST szerződés jóváhagyása szükséges."
  fi
fi

# 2. Scraper Motorok & Alkalmazásbiztonság (scraper-security-engineer)
if echo "$CHANGED_FILES" | grep -qE "src/main/java/ingatlanok/(portal|scraper|common)/|SidecarCliExecutor\.java|BrowserUtils\.java|HttpFetcher\.java"; then
  echo "-> Érintett téma: Scraper Motorok és Alkalmazásbiztonság (scraper-security-engineer szükséges)"
  if ! echo "$BODY" | grep -qiE "\[x\][[:space:]]*.*(scraper-security-engineer|Scraper & Security)"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - 🛡️ Scraper & Security Specialist (\`scraper-security-engineer\`): Scraper megbízhatóság, WAF/bot védelem vagy AppSec hardening jóváhagyása szükséges."
  fi
fi

# 3. Minőségbiztosítás, Tesztelés & DevOps/CI (quality-devops-engineer)
if echo "$CHANGED_FILES" | grep -qE "\.github/|Dockerfile|docker-compose|src/test/|frontend/.*test|pom\.xml|package\.json"; then
  echo "-> Érintett téma: Minőségbiztosítás, DevOps vagy CI/CD (quality-devops-engineer szükséges)"
  if ! echo "$BODY" | grep -qiE "\[x\][[:space:]]*.*(quality-devops-engineer|Quality & DevOps)"; then
    MISSING_APPROVALS="${MISSING_APPROVALS}\n  - ⚙️ Quality & DevOps Specialist (\`quality-devops-engineer\`): Tesztlefedettség, determinista tesztek, Docker vagy CI/CD pipeline jóváhagyása szükséges."
  fi
fi

if [ -n "$MISSING_APPROVALS" ]; then
  echo "::error::Hiányzó Tematikus Agent Jóváhagyás(ok)! A PR nem engedélyezett a megfelelő szakértői ágens igazolása nélkül:${MISSING_APPROVALS}"
  echo ""
  echo "Kérjük, add hozzá a jóváhagyást a PR leírásának 'Thematic Agent Approval Gate' blokkjához!"
  exit 1
fi

echo "✅ Minden érintett szakterület rendelkezik a kijelölt Agent jóváhagyásával."
