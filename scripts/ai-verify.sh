#!/usr/bin/env bash
# ==============================================================================
# scripts/ai-verify.sh
# Token-efficient, high-signal verification runner for AI agents & developers.
#
# Usage:
#   bash scripts/ai-verify.sh [--backend | --frontend | --all]
#
# Behavior:
#   - Suppresses thousands of lines of routine build/test stdout.
#   - On PASS: prints a single compact summary line per suite.
#   - On FAIL: prints only the actionable error lines and exits with code 1.
# ==============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

MODE="all"
for arg in "$@"; do
  case "$arg" in
    --backend) MODE="backend" ;;
    --frontend) MODE="frontend" ;;
    --all) MODE="all" ;;
    -h|--help)
      echo "Usage: $0 [--backend | --frontend | --all]"
      exit 0
      ;;
  esac
done

ensure_java_home() {
  if [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
    return 0
  fi
  if command -v java >/dev/null 2>&1; then
    return 0
  fi
  for candidate in "$HOME"/.jdks/*25* "/c/Users/${USERNAME:-Viktor}/.jdks/"*25*; do
    if [ -d "$candidate" ] && [ -x "$candidate/bin/java" ]; then
      export JAVA_HOME="$candidate"
      export PATH="$JAVA_HOME/bin:$PATH"
      return 0
    fi
  done
}

run_step() {
  local label="$1"
  shift
  local tmp_log
  tmp_log="$(mktemp)"
  if "$@" >"$tmp_log" 2>&1; then
    echo "✅ [PASS] ${label}"
    rm -f "$tmp_log"
    return 0
  else
    echo "❌ [FAIL] ${label}"
    echo "--- Actionable Error Excerpt (${label}) ---"
    grep -E "(ERROR|FAILED|Exception|error TS|[0-9]+:[0-9]+[[:space:]]+error|❌)" "$tmp_log" | head -n 50 || tail -n 40 "$tmp_log"
    echo "--- Tail of Output ---"
    tail -n 25 "$tmp_log"
    rm -f "$tmp_log"
    return 1
  fi
}

if [ "$MODE" = "backend" ] || [ "$MODE" = "all" ]; then
  ensure_java_home
  GRADLE_CMD="./gradlew"
  if [[ "${OSTYPE:-}" == msys* || "${OSTYPE:-}" == cygwin* || "${OS:-}" == "Windows_NT" ]]; then
    GRADLE_CMD="./gradlew.bat"
  fi
  run_step "Backend (Checkstyle, ArchUnit, JUnit 5, JaCoCo >=80%)" \
    "$GRADLE_CMD" checkstyleMain checkstyleTest test jacocoTestReport jacocoTestCoverageVerification --console=plain -q
fi

if [ "$MODE" = "frontend" ] || [ "$MODE" = "all" ]; then
  run_step "Frontend (TypeScript --noEmit, ESLint max-warnings=0, Vitest)" \
    bash -c "cd frontend && npm run typecheck && npm run lint && npm run test"
fi

if [ "$MODE" = "all" ]; then
  run_step "Skills & Architectural Guardrails (validate-skills.sh)" \
    bash scripts/validate-skills.sh
fi

echo "🎯 [AI-VERIFY] All requested checks passed with 0 errors."
