#!/usr/bin/env bash
set -eo pipefail

CSV_FILE="${1:-target/site/jacoco/jacoco.csv}"

if [ ! -f "$CSV_FILE" ]; then
  CSV_FILE="build/reports/jacoco/test/jacocoTestReport.csv"
fi

if [ ! -f "$CSV_FILE" ]; then
  echo "::error::JaCoCo CSV jelentés nem található sem a target/site/jacoco/ sem a build/reports/... helyen!"
  if [ -n "$GITHUB_OUTPUT" ]; then
    echo "coverage_pct=N/A" >> "$GITHUB_OUTPUT"
    echo "coverage_lines=0 / 0 sor" >> "$GITHUB_OUTPUT"
    echo "coverage_status=fail" >> "$GITHUB_OUTPUT"
  fi
  exit 1
fi

LINE_MISSED=$(awk -F',' 'NR>1 {sum+=$8} END {print sum+0}' "$CSV_FILE")
LINE_COVERED=$(awk -F',' 'NR>1 {sum+=$9} END {print sum+0}' "$CSV_FILE")
TOTAL_LINES=$((LINE_MISSED + LINE_COVERED))

if [ "$TOTAL_LINES" -eq 0 ]; then
  echo "::error::JaCoCo CSV nem tartalmaz kódsorokat!"
  if [ -n "$GITHUB_OUTPUT" ]; then
    echo "coverage_pct=0.00%" >> "$GITHUB_OUTPUT"
    echo "coverage_lines=0 / 0 sor" >> "$GITHUB_OUTPUT"
    echo "coverage_status=fail" >> "$GITHUB_OUTPUT"
  fi
  exit 1
fi

COVERAGE_PCT=$(awk -v cov="$LINE_COVERED" -v tot="$TOTAL_LINES" 'BEGIN {printf "%.2f", (cov / tot) * 100}')
IS_PASS=$(awk -v pct="$COVERAGE_PCT" 'BEGIN {print (pct >= 80.0) ? "pass" : "fail"}')

echo "=== 📈 JaCoCo Kódlefedettség Elemzés ==="
echo "Lefedett sorok:   $LINE_COVERED"
echo "Kihagyott sorok:  $LINE_MISSED"
echo "Összes sor:       $TOTAL_LINES"
echo "Lefedettség:      ${COVERAGE_PCT}%"
echo "Státusz:          $IS_PASS (Cél: >= 80.00%)"

if [ -n "$GITHUB_OUTPUT" ]; then
  echo "coverage_pct=${COVERAGE_PCT}%" >> "$GITHUB_OUTPUT"
  echo "coverage_lines=${LINE_COVERED} / ${TOTAL_LINES} sor" >> "$GITHUB_OUTPUT"
  echo "coverage_status=${IS_PASS}" >> "$GITHUB_OUTPUT"
fi

if [ "$IS_PASS" != "pass" ]; then
  echo "::error::A JaCoCo kódlefedettség (${COVERAGE_PCT}%) nem éri el a megkövetelt 80%-os minimumot!"
  exit 1
fi

echo "✅ JaCoCo kódlefedettség sikeresen ellenőrizve: ${COVERAGE_PCT}% (>= 80%)."
