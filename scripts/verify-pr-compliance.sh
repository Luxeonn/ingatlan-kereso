#!/usr/bin/env bash
# ==============================================================================
# scripts/verify-pr-compliance.sh
# Cross-platform runner for the PR compliance & agent governance validator.
#
# Usage:
#   bash scripts/verify-pr-compliance.sh --body-file pr_body.md --diff-base origin/develop
# ==============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
node "$REPO_ROOT/scripts/verify-pr-compliance.mjs" "$@"
