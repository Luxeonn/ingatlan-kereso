# GitHub Actions Workflow Sablonok és Minták

Ez a dokumentum azonnal felhasználható, production-ready munkafolyamat sablonokat tartalmaz.

---

## 1. Teljes Körű Java 21 & Maven CI Sablon Gyorstárazással és Összefoglalóval

```yaml
name: Java CI Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
  workflow_dispatch:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read

jobs:
  build-and-test:
    name: Build & Test (Java 21 LTS)
    runs-on: ubuntu-latest
    timeout-minutes: 15

    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Set up JDK 21 (Eclipse Temurin)
        uses: actions/setup-java@v5
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Maven Tests & Build
        run: mvn --batch-mode --no-transfer-progress clean test package

      - name: Generate Action Summary
        if: always()
        run: |
          echo "## 🚀 CI Pipeline Eredmények" >> $GITHUB_STEP_SUMMARY
          echo "| Paraméter | Érték |" >> $GITHUB_STEP_SUMMARY
          echo "| :--- | :--- |" >> $GITHUB_STEP_SUMMARY
          echo "| **Állapot** | ${{ job.status == 'success' && '✅ Sikeres' || '❌ Sikertelen' }} |" >> $GITHUB_STEP_SUMMARY
          echo "| **Java Verzió** | 21 LTS Temurin |" >> $GITHUB_STEP_SUMMARY
          echo "| **Ág / Commit** | \`${{ github.ref_name }}\` (\`${{ github.sha }}\`) |" >> $GITHUB_STEP_SUMMARY
```

---

## 2. Automatikus PR Címke és Méret Kezelő Sablon

```yaml
name: PR Auto Labeler

on:
  pull_request_target:
    types: [opened, synchronize]

permissions:
  pull-requests: write
  contents: read

jobs:
  labeler:
    name: Auto Label PR
    runs-on: ubuntu-latest
    timeout-minutes: 5

    steps:
      - name: Label based on file changes
        uses: actions/labeler@v5
        with:
          repo-token: ${{ secrets.GITHUB_TOKEN }}
```

---

## 3. Ütemezett Háttérfigyelő Scraping Sablon

```yaml
name: Scheduled Real Estate Monitor

on:
  schedule:
    # Minden reggel 7:00 és 18:00 UTC-kor fut
    - cron: '0 7,18 * * *'
  workflow_dispatch:

concurrency:
  group: scheduled-monitor
  cancel-in-progress: false

permissions:
  contents: write

jobs:
  run-monitor:
    name: Execute Scraper & Update Feed
    runs-on: ubuntu-latest
    timeout-minutes: 20

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v5
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Run Search & Persistence Update
        run: mvn compile exec:java -Dexec.args="--cli --all-cities"

      - name: Commit Updated Database
        run: |
          git config user.name "github-actions[bot]"
          git config user.email "github-actions[bot]@users.noreply.github.com"
          git add data/
          git diff --quiet && git diff --staged --quiet || (git commit -m "chore(data): auto-update listings database" && git push)

---

## 4. GHCR Docker Konténer Build & Publish Sablon GHA Gyorstárazással

```yaml
name: Publish Docker Image to GHCR

on:
  push:
    branches: [develop]
    tags: ['v*.*.*']
  workflow_dispatch:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

permissions:
  contents: read
  packages: write

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-push:
    name: Build & Publish Container Image
    runs-on: ubuntu-latest
    timeout-minutes: 20

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract Docker metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=raw,value=latest,enable=${{ github.ref == 'refs/heads/develop' }}
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,format=short

      - name: Build and push Docker image
        id: push-step
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Generate Publication Summary
        if: always()
        run: |
          echo "## 🐳 Docker Konténer Publikálás Eredménye" >> $GITHUB_STEP_SUMMARY
          echo "| Tulajdonság | Érték |" >> $GITHUB_STEP_SUMMARY
          echo "| :--- | :--- |" >> $GITHUB_STEP_SUMMARY
          echo "| **Képnév** | \`${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}\` |" >> $GITHUB_STEP_SUMMARY
          echo "| **Digest** | \`${{ steps.push-step.outputs.digest }}\` |" >> $GITHUB_STEP_SUMMARY
          echo "| **Állapot** | ${{ job.status == 'success' && '✅ Sikeresen közzétéve' || '❌ Sikertelen' }} |" >> $GITHUB_STEP_SUMMARY
```
```
